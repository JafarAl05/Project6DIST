package systemy.node;

import systemy.common.HashingUtil;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class NodeMulticastListener implements Runnable {

    private final NeighborInfo neighborInfo;
    private final RestClient restClient;

    // Must match Role A and Role B exactly
    private static final String MULTICAST_ADDRESS = "224.0.0.200";
    private static final int PORT = 8888;

    public NodeMulticastListener(NeighborInfo neighborInfo, RestClient restClient) {
        this.neighborInfo = neighborInfo;
        this.restClient = restClient;
    }

    // Call this from NodeApp to start the background thread
    public void startListening() {
        new Thread(this).start();
        System.out.println("📡 [System] Node Multicast Listener active. Waiting for new peers...");
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            byte[] buffer = new byte[256];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Blocks until someone shouts!

                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                String[] parts = receivedMessage.split(",");

                // Role A should be sending "NodeName,IP"
                if (parts.length == 2) {
                    String newNodeName = parts[0];
                    String newNodeIp = parts[1];
                    int newNodeHash = HashingUtil.hash(newNodeName);

                    int currentId = neighborInfo.getCurrentID();

                    // Ignore our own shouts!
                    if (newNodeHash == currentId) continue;

                    System.out.println("\n📡 [Multicast Heard] Node " + newNodeHash + " is joining the ring!");
                    checkAndUpdateNeighbors(newNodeHash, newNodeIp);
                    System.out.print("Choose an option: "); // Reprint terminal prompt
                }
            }
        } catch (Exception e) {
            System.err.println("Node Multicast Listener crashed: " + e.getMessage());
        }
    }

    private void checkAndUpdateNeighbors(int newNodeHash, String newNodeIp) {
        int currentId = neighborInfo.getCurrentID();
        int nextId = neighborInfo.getNextID();
        int prevId = neighborInfo.getPreviousID();

        // SCENARIO 1: We are the ONLY node in the network right now.
        // If someone joins, they immediately become our previous AND next node.
        if (currentId == nextId && currentId == prevId) {
            neighborInfo.setNextID(newNodeHash);
            neighborInfo.setPreviousID(newNodeHash);

            // Tell the new node that we are BOTH of their neighbors
            restClient.updatePeer(newNodeIp, "previous", currentId);
            restClient.updatePeer(newNodeIp, "next", currentId);
            return;
        }

        // SCENARIO 2: Does the new node fit exactly after us?
        if (isBetween(currentId, newNodeHash, nextId)) {
            neighborInfo.setNextID(newNodeHash);
            // Tell the new guy: "I am your previous node!"
            restClient.updatePeer(newNodeIp, "previous", currentId);
        }

        // SCENARIO 3: Does the new node fit exactly before us?
        if (isBetween(prevId, newNodeHash, currentId)) {
            neighborInfo.setPreviousID(newNodeHash);
            // Tell the new guy: "I am your next node!"
            restClient.updatePeer(newNodeIp, "next", currentId);
        }
    }

    // Helper method to handle the ring's math (including the wrap-around)
    private boolean isBetween(int left, int target, int right) {
        if (left < right) {
            // Standard case: 5000 < 6000 < 7000
            return target > left && target < right;
        } else {
            // Wrap-around case (e.g., left is 32000, right is 500)
            // The target must be bigger than 32000 OR smaller than 500
            return target > left || target < right;
        }
    }
}