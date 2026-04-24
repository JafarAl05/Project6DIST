package systemy.node;

import systemy.common.HashingUtil;
import java.net.InetAddress;
import java.util.Scanner;

public class NodeApp {

    private static final int UNICAST_PORT = 8081;
    private static final int MULTICAST_PORT = 8888;
    private static final String MULTICAST_IP = "224.0.0.200";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        RestClient restClient = new RestClient();

        printHeader();

        // --- 1. Node Identity Setup ---
        System.out.print("Enter a unique name for this node (e.g., Node-1): ");
        String nodeName = scanner.nextLine();
        int myNodeId = HashingUtil.hash(nodeName);
        System.out.println("[System] Calculated Node ID: " + myNodeId);

        // --- 2. Network IP Configuration ---
        String resolvedIp = "127.0.0.1";
        try {
            resolvedIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            System.err.println("[Warning] Could not determine local IP. Defaulting to 127.0.0.1.");
        }

        System.out.print("Enter IP address for this node (Press Enter to use detected IP: " + resolvedIp + "): ");
        String manualIp = scanner.nextLine();
        if (!manualIp.trim().isEmpty()) {
            resolvedIp = manualIp;
        }
        final String myIp = resolvedIp;
        System.out.println("[System] Node IP locked to: " + myIp);

        // --- 3. Initialize Listeners (The Brain and Ears) ---
        NeighborInfo neighborInfo = new NeighborInfo(myNodeId);

        UnicastListener unicastListener = new UnicastListener(neighborInfo);
        unicastListener.start(UNICAST_PORT);

        NodeMulticastListener nodeMultiListener = new NodeMulticastListener(neighborInfo, restClient);
        nodeMultiListener.startListening();

        // --- 4. Bootstrap (Join the Ring) ---
        System.out.println("[System] Attempting to join network...");
        int networkSize = -1;

        try {
            Broadcaster roleABroadcaster = new Broadcaster(MULTICAST_PORT, MULTICAST_IP);
            networkSize = roleABroadcaster.broadcastPresence(nodeName, myIp);
        } catch (Exception e) {
            System.err.println("[Error] Failed to broadcast: " + e.getMessage());
        }

        if (networkSize == -1) {
            System.err.println("[Fatal] Registration failed. Shutting down listeners...");
            unicastListener.stop();
            return;
        }

        if (networkSize < 1) {
            System.out.println("[System] I am the first node in the network. (Prev/Next set to self)");
        } else {
            System.out.println("[System] " + networkSize + " other node(s) exist. Awaiting neighbor contact...");
        }

        // --- 5. Graceful Shutdown Hook ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[System] Shutdown initiated. Bridging the ring...");

            if (neighborInfo.getPreviousID() != myNodeId && neighborInfo.getNextID() != myNodeId) {
                String prevIp = restClient.getNodeIpById(neighborInfo.getPreviousID());
                String nextIp = restClient.getNodeIpById(neighborInfo.getNextID());

                if (prevIp != null) restClient.updatePeer(prevIp, "next", neighborInfo.getNextID());
                if (nextIp != null) restClient.updatePeer(nextIp, "previous", neighborInfo.getPreviousID());
            }

            restClient.removeNode(myNodeId);
            unicastListener.stop();
            System.out.println("[System] Node safely removed from network. Goodbye.");
        }));

        // --- 6. Interactive Main Menu ---
        runMenuLoop(scanner, restClient, neighborInfo, myNodeId, myIp);
    }

    private static void runMenuLoop(Scanner scanner, RestClient restClient, NeighborInfo neighborInfo, int myNodeId, String myIp) {
        while (true) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1. Find a file location");
            System.out.println("2. View my Ring Status (Neighbors)");
            System.out.println("3. Ping Next Node (Test Failure Detection)");
            System.out.println("4. Exit and remove node");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    System.out.print("Enter the filename: ");
                    String filename = scanner.nextLine();
                    String resultIp = restClient.getFileLocation(filename);
                    System.out.println("[Result] " + resultIp);
                    break;

                case "2":
                    System.out.println(neighborInfo.toString());
                    break;

                case "3":
                    System.out.println("[System] Pinging NEXT node (" + neighborInfo.getNextID() + ")...");
                    String nextIp = restClient.getNodeIpById(neighborInfo.getNextID());

                    try {
                        if (nextIp == null) throw new Exception("IP not found in Naming Server.");
                        restClient.updatePeer(nextIp, "ping", myNodeId);
                    } catch (Exception e) {
                        handleNodeFailure(neighborInfo, restClient, myNodeId, nextIp);
                    }
                    break;

                case "4":
                    System.exit(0); // Triggers the shutdown hook automatically
                    break;

                default:
                    System.out.println("[Warning] Invalid option. Please try again.");
            }
        }
    }

    private static void handleNodeFailure(NeighborInfo neighborInfo, RestClient restClient, int myNodeId, String nextIp) {
        int deadNodeId = neighborInfo.getNextID();
        System.err.println("[Alert] PING FAILED! Node " + deadNodeId + " is unresponsive.");
        System.out.println("[System] Requesting dead node's neighbors from Naming Server...");

        String neighborsJson = restClient.getNeighborsOfFailedNode(deadNodeId);
        System.out.println("[System] Naming Server replied: " + neighborsJson);

        if (neighborsJson != null) {
            try {
                // Parse the JSON string to extract the surviving neighbor IDs
                String cleanedJson = neighborsJson.replaceAll("[^0-9,]", "");
                String[] parts = cleanedJson.split(",");
                int survivingNext = Integer.parseInt(parts[1]);

                System.out.println("[System] Commencing ring repair...");

                // Step A: Update local pointer
                neighborInfo.setNextID(survivingNext);
                System.out.println("[System] Updated local NEXT pointer to: " + survivingNext);

                // Step B: Inform the surviving next node
                String survivingNextIp = restClient.getNodeIpById(survivingNext);
                if (survivingNextIp != null) {
                    restClient.updatePeer(survivingNextIp, "previous", myNodeId);
                    System.out.println("[System] Alerted Node " + survivingNext + " to update its PREV to " + myNodeId);
                }

                // Step C: Cleanup the Naming Server
                System.out.println("[System] Instructing Naming Server to scrub dead node...");
                restClient.removeNode(deadNodeId);

                System.out.println("[Success] Ring failure recovered gracefully.");

            } catch (Exception ex) {
                System.err.println("[Error] Ring repair failed during parsing or network update.");
            }
        }
    }

    private static void printHeader() {
        System.out.println("=========================================");
        System.out.println("      System Y - Node Simulator          ");
        System.out.println("=========================================");
    }
}