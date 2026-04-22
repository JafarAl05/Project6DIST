package systemy.namingserver;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import systemy.common.HashingUtil;

@Component
public class MulticastListener implements Runnable {

    // Multicast communication parameters
    private static final String MULTICAST_ADDRESS = "224.0.0.200";
    private static final int PORT = 8888;

    private final MapManager mapManager;

    // Inject map manager
    public MulticastListener(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    @PostConstruct // This Spring annotation starts the thread when the server boots
    public void init() {
        Thread listenerThread = new Thread(this);
        listenerThread.start();
        System.out.println("Multicast Listener started on " + MULTICAST_ADDRESS + ":" + PORT);
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);

            byte[] buffer = new byte[256]; // Message of 256 bytes

            while (true) { // Infinite listening loop
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                socket.receive(packet); // This line blocks until a message is received

                // Receive the message (e.g. "Node-Alpha,192.168.1.10")
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received multicast from " + packet.getAddress() + ": " + receivedMessage);

                // Parse the message ("Name,IP" format)
                String[] parts = receivedMessage.split(",");
                if (parts.length == 2) {
                    String nodeName = parts[0];
                    String nodeIp = parts[1];

                    // Calculate hash
                    int nodeHash = HashingUtil.hash(nodeName);

                    // FIX: Respond with the number of existing nodes FIRST!
                    int currentCount = mapManager.getNameMap().size();

                    // THEN Add to Map
                    mapManager.addNode(nodeHash, nodeIp);

                    // Convert the number into a byte array so it can travel over the network
                    String responseMessage = String.valueOf(currentCount);
                    byte[] responseBuffer = responseMessage.getBytes();

                    // Create the UDP response packet.
                    // We use packet.getAddress() to aim it directly back at the connecting node
                    DatagramPacket responsePacket = new DatagramPacket(
                            responseBuffer,
                            responseBuffer.length,
                            packet.getAddress(),
                            packet.getPort()     // Aiming it back at the port they sent it from
                    );

                    // Fire it back using the same socket!
                    socket.send(responsePacket);
                    System.out.println("Sent UDP response to " + packet.getAddress() + ": " + responseMessage);
                }
            }
        } catch (Exception e) {
            System.err.println("Error in MulticastListener: " + e.getMessage());
        }
    }
}
