package systemy.node;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestClient {

    private static final String BASE_URL = "http://localhost:8080";
    private final HttpClient httpClient;

    public RestClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }


    public boolean registerNode(int nodeId, String ipAddress) {
        String url = BASE_URL + "/nodes";

        String jsonPayload = String.format("{\"nodeId\": %d, \"ipAddress\": \"%s\"}", nodeId, ipAddress);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Node successfully registered. Status code: " + response.statusCode());
                return true;
            } else {
                System.err.println("Registration failed with HTTP " + response.statusCode());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Failed to connect to Naming Server: " + e.getMessage());
            return false;
        }
    }


    public void removeNode(int nodeId) {
        String url = BASE_URL + "/nodes/" + nodeId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                System.out.println("Successfully removed from Naming Server.");
            } else {
                System.err.println("Failed to remove node. HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Could not reach Naming Server to delete node.");
        }
    }


    public String getFileLocation(String filename) {
        String url = BASE_URL + "/files/" + filename;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String ip = extractIpFromJson(response.body());
                System.out.println("IP address successfully found: " + ip);
                return ip;
            } else {
                return "Error: File not found or Naming Server issue (HTTP " + response.statusCode() + ").";
            }
        } catch (Exception e) {
            return "Connection error: " + e.getMessage();
        }
    }

    private String extractIpFromJson(String json) {
        Pattern pattern = Pattern.compile("\"ipAddress\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "Error: Could not parse IP from response -> " + json;
        }
    }
    // =========================================================================
    // PEER-TO-PEER: Talk directly to another node's UnicastListener
    // =========================================================================
    /**
     * Sends a direct message to a neighbor to update its ring pointers.
     * @param targetIp The IP address of the node you are talking to
     * @param parameterToUpdate Either "previous" or "next"
     * @param newId The new ID they should save in their brain
     */
    public void updatePeer(String targetIp, String parameterToUpdate, int newId) {
        // Notice we REMOVED the :8081 because targetIp now has the port attached to it!
        String url = "http://" + targetIp + "/update/" + parameterToUpdate + "?id=" + newId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody()) // No JSON
                .build();


        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                System.out.println("Successfully updated peer at " + targetIp);
            }
        } catch (Exception e) {
            System.err.println("Could not reach peer at " + targetIp + ". They might be dead!");
        }
    }

    // =========================================================================
    // FAILURE RECOVERY: Ask Naming Server for a dead node's neighbors
    // =========================================================================
    /**
     * If a node crashes, we ask the Naming Server who its neighbors were so we can fix the ring.
     * Note: You must tell Role B to create this GET /nodes/{id}/neighbors endpoint!
     */
    public String getNeighborsOfFailedNode(int failedNodeId) {
        String url = BASE_URL + "/nodes/" + failedNodeId + "/neighbors";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body(); // Role B should return a JSON with {previousID, nextID}
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================================
    // HELPER: Get IP by Node ID
    // =========================================================================
    /**
     * Before we can send a Unicast message to a neighbor, we need their IP address!
     * Note: Role B needs to ensure this endpoint exists.
     */
    public String getNodeIpById(int nodeId) {
        String url = BASE_URL + "/nodes/" + nodeId + "/ip";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (Exception e) {}
        return null;
    }
}