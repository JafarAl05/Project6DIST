package systemy.node;

import systemy.common.HashingUtil;
import java.net.InetAddress;
import java.util.Scanner;

public class NodeApp {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        RestClient restClient = new RestClient();

        System.out.println("=========================================");
        System.out.println("      System Y - Node Simulator          ");
        System.out.println("=========================================");

        System.out.print("Enter a unique name for this node (e.g., Node-1): ");
        String nodeName = scanner.nextLine();

        int myNodeId = HashingUtil.hash(nodeName);
        System.out.println("--> Calculated Node ID: " + myNodeId);

        String resolvedIp = "127.0.0.1";
        try {
            resolvedIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            System.out.println("⚠️ Could not determine local IP. Defaulting to 127.0.0.1.");
        }
        final String myIp = resolvedIp; // Adding 'final' makes Java happy!

        // 1. Initialize the Brain and Ears
        NeighborInfo neighborInfo = new NeighborInfo(myNodeId);

        UnicastListener unicastListener = new UnicastListener(neighborInfo);
        unicastListener.start(8081); // Start listening for specific neighbor updates

        NodeMulticastListener nodeMultiListener = new NodeMulticastListener(neighborInfo, restClient);
        nodeMultiListener.startListening(); // Start listening for new nodes joining

        // 2. Bootstrap via Multicast (Using Role A's Broadcaster)
        System.out.println("Attempting to join network...");
        int networkSize = -1;
        try {
            Broadcaster roleABroadcaster = new Broadcaster(8888, "224.0.0.200");
            networkSize = roleABroadcaster.broadcastPresence(nodeName, myIp);
        } catch (Exception e) {
            System.out.println("❌ Failed to broadcast: " + e.getMessage());
        }

        if (networkSize == -1) {
            System.out.println("❌ Registration failed. Exiting...");
            unicastListener.stop();
            return;
        }

        if (networkSize < 1) {
            System.out.println("🌟 I am the first node in the network! (Setting Prev/Next to myself)");
        } else {
            System.out.println("⏳ " + networkSize + " other node(s) exist. Waiting for my neighbors to contact me...");
        }

        // 3. Graceful Shutdown Hook (Bridging the gap when you close the app)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[System] Shutting down. Bridging the ring...");

            // Only bridge if we actually have different neighbors
            if (neighborInfo.getPreviousID() != myNodeId && neighborInfo.getNextID() != myNodeId) {
                String prevIp = restClient.getNodeIpById(neighborInfo.getPreviousID());
                String nextIp = restClient.getNodeIpById(neighborInfo.getNextID());

                // Send the ID of the next node to the previous node
                if (prevIp != null) restClient.updatePeer(prevIp, "next", neighborInfo.getNextID());

                // Send the ID of the previous node to the next node
                if (nextIp != null) restClient.updatePeer(nextIp, "previous", neighborInfo.getPreviousID());
            }

            restClient.removeNode(myNodeId, myIp);
            unicastListener.stop();
            System.out.println("[System] Node safely removed. Goodbye!");
        }));

        // 4. Interactive Menu
        while (true) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1. Find a file location");
            System.out.println("2. View my Ring Status (Neighbors)");
            System.out.println("3. Ping Next Node (Test Failure Detection)");
            System.out.println("4. Exit and remove node");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine();

            if ("1".equals(choice)) {
                System.out.print("Enter the filename: ");
                String filename = scanner.nextLine();
                String resultIp = restClient.getFileLocation(filename);
                System.out.println("--> Result: " + resultIp);
            }
            else if ("2".equals(choice)) {
                System.out.println(neighborInfo.toString());
            }
            else if ("3".equals(choice)) {
                System.out.println("🔍 Pinging NEXT node (" + neighborInfo.getNextID() + ")...");
                String nextIp = restClient.getNodeIpById(neighborInfo.getNextID());

                // We use our updatePeer method as a dummy "ping"
                try {
                    if (nextIp == null) throw new Exception("IP not found");
                    restClient.updatePeer(nextIp, "ping", myNodeId);
                } catch (Exception e) {
                    // =========================================================================
                    // ADDED LOGIC: Failure Recovery (Diagnosis, Surgery, Cleanup)
                    // =========================================================================
                    int deadNodeId = neighborInfo.getNextID();
                    System.out.println("🚨 PING FAILED! Node " + deadNodeId + " is dead!");
                    System.out.println("🛠️ Asking Naming Server for dead node's neighbors...");

                    String neighborsJson = restClient.getNeighborsOfFailedNode(deadNodeId);
                    System.out.println("--> Naming Server replied: " + neighborsJson);

                    if (neighborsJson != null) {
                        try {
                            // Strip away the JSON brackets/quotes so we just have the numbers
                            String cleanedJson = neighborsJson.replaceAll("[^0-9,]", "");
                            String[] parts = cleanedJson.split(",");
                            int survivingNext = Integer.parseInt(parts[1]);

                            System.out.println("🩹 Patching the ring...");

                            // Step A: Update my own brain
                            neighborInfo.setNextID(survivingNext);
                            System.out.println("--> Updated my own NEXT pointer to: " + survivingNext);

                            // Step B: Tell the surviving next node to point its PREVIOUS back to me
                            String survivingNextIp = restClient.getNodeIpById(survivingNext);
                            if (survivingNextIp != null) {
                                restClient.updatePeer(survivingNextIp, "previous", myNodeId);
                                System.out.println("--> Told Node " + survivingNext + " that its new PREV is " + myNodeId);
                            }

                            // Step C: Cleanup the Naming Server
                            System.out.println("🧹 Removing dead node from Naming Server...");
                            restClient.removeNode(deadNodeId, nextIp != null ? nextIp : "");

                            System.out.println("✅ Failure recovery successfully completed!");

                        } catch (Exception ex) {
                            System.out.println("❌ Failed to parse Naming Server response or patch the ring.");
                        }
                    }
                }
            }
            else if ("4".equals(choice)) {
                System.exit(0);
            } else {
                System.out.println("⚠️ Invalid option.");
            }
        }
    }
}