package systemy.node;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Broadcaster {
    private final InetAddress address;
    private final int port;
    private final DatagramSocket socket;

    public Broadcaster(int socketNum, String broadcastIP) throws UnknownHostException, SocketException {
        address = InetAddress.getByName(broadcastIP);
        port = socketNum;
        socket = new DatagramSocket(); // Picks random open port
        socket.setSoTimeout(5000); // Prevents infinite freezing
    }

    // CHANGED: Now returns an int, waits for response, and is renamed!
    public int broadcastPresence(String nodeName, String ipAddress) throws IOException {
        String messageContents = nodeName + "," + ipAddress;
        byte[] message = messageContents.getBytes(StandardCharsets.UTF_8);
        DatagramPacket multicast = new DatagramPacket(message, message.length, address, port);

        socket.send(multicast); // Send the shout
        System.out.println("Sent multicast: " + messageContents);

        try {
            // Wait for Naming Server to tell us the network size
            byte[] responseBuffer = new byte[256];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(responsePacket);

            // Return that size to NodeApp!
            String responseStr = new String(responsePacket.getData(), 0, responsePacket.getLength()).trim();
            return Integer.parseInt(responseStr);
        } catch (SocketTimeoutException e) {
            System.err.println("Naming server did not respond.");
            return -1;
        }
    }
}