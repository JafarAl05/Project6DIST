package systemy.node;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class UnicastListener {

    private final NeighborInfo neighborInfo;
    private HttpServer server;

    // We pass in the NeighborInfo so this listener can directly update the brain
    public UnicastListener(NeighborInfo neighborInfo) {
        this.neighborInfo = neighborInfo;
    }

    // New method: Accepts the IP and binds strictly to it!
    public void start(String ipAddress, int port) {
        try {
            // By passing the IP here, we restrict the port to ONLY this specific container's IP
            server = HttpServer.create(new InetSocketAddress(ipAddress, port), 0);

            // Endpoint 1: Existing node tells us it is our PREVIOUS neighbor
            server.createContext("/update/previous", new PreviousHandler());

            // Endpoint 2: Existing node tells us it is our NEXT neighbor
            server.createContext("/update/next", new NextHandler());

            server.setExecutor(null); // Use the default executor
            server.start();
            System.out.println("Unicast Listener active on port " + port);
        } catch (IOException e) {
            System.err.println("Failed to start Unicast Listener: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Unicast Listener stopped.");
        }
    }

    // =========================================================================
    // HANDLERS: These trigger whenever a network request hits the URL
    // =========================================================================

    class PreviousHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // To avoid complex JSON parsing without Spring Boot, we will just pass
                // the ID in the URL. Example: POST /update/previous?id=5000
                String query = exchange.getRequestURI().getQuery();
                int newPrevId = extractId(query);

                if (newPrevId != -1) {
                    neighborInfo.setPreviousID(newPrevId); // Update the brain!
                    System.out.println("\n My PREVIOUS node is now: " + newPrevId);
                    System.out.print("Choose an option: "); // Reprint prompt so terminal looks clean
                    sendResponse(exchange, 200, "Previous ID updated successfully.");
                } else {
                    sendResponse(exchange, 400, "Bad Request: Missing ID parameter.");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed.");
            }
        }
    }

    class NextHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                int newNextId = extractId(query);

                if (newNextId != -1) {
                    neighborInfo.setNextID(newNextId); // Update the brain!
                    System.out.println("\n🔄 [Network Update] My NEXT node is now: " + newNextId);
                    System.out.print("Choose an option: ");
                    sendResponse(exchange, 200, "Next ID updated successfully.");
                } else {
                    sendResponse(exchange, 400, "Bad Request: Missing ID parameter.");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed.");
            }
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    // extracts the ID out of the URl
    private int extractId(String query) {
        if (query != null && query.startsWith("id=")) {
            try {
                return Integer.parseInt(query.substring(3));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    // Sends a standard HTTP response back to the node that called us
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}