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
        socket = new DatagramSocket(port);
    }

    private void sendFile(String nodeName, String ipAddress) throws IOException {
        String messageContents = nodeName + "," + ipAddress;
        byte[] message = messageContents.getBytes(StandardCharsets.UTF_8);
        int length = message.length;
        DatagramPacket multicast = new DatagramPacket(message, length, address, port);
        socket.send(multicast);
    }
}
