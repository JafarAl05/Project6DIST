package systemy.node;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestClient {

    private static final String DEFAULT_BASE_URL = "http://server:8080";
    private final HttpClient httpClient;
    private final String baseUrl;

    public RestClient() {
        this(System.getProperty("systemy.namingServerUrl", DEFAULT_BASE_URL));
    }

    public RestClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }


    public boolean registerNode(int nodeId, String ipAddress) {
        String url = baseUrl + "/nodes";

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
        String url = baseUrl + "/nodes/" + nodeId;

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
        String url = baseUrl + "/files/" + filename;

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
    public boolean updatePeer(String targetIp, String parameterToUpdate, int newId) {
        String url = "http://" + targetIp + ":8081/update/" + parameterToUpdate + "?id=" + newId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody()) // No JSON
                .build();


        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                System.out.println("Successfully updated peer at " + targetIp);
                return true;
            }
            System.err.println("Peer at " + targetIp + " responded with HTTP " + response.statusCode());
        } catch (Exception e) {
            System.err.println("Could not reach peer at " + targetIp + ". They might be dead!");
        }
        return false;
    }

    // =========================================================================
    // FAILURE RECOVERY: Ask Naming Server for a dead node's neighbors
    // =========================================================================
    /**
     * If a node crashes, we ask the Naming Server who its neighbors were so we can fix the ring.
     * Note: You must tell Role B to create this GET /nodes/{id}/neighbors endpoint!
     */
    public String getNeighborsOfFailedNode(int failedNodeId) {
        String url = baseUrl + "/nodes/" + failedNodeId + "/neighbors";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body(); // Role B should return a JSON with {previousID, nextID}
            } else {
                System.err.println("Failed to get neighbors for node " + failedNodeId + ". HTTP " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Could not retrieve neighbors for node " + failedNodeId + ": " + e.getMessage());
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
        String url = baseUrl + "/nodes/" + nodeId + "/ip";

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

    public int[] getNeighborsById(int nodeId) {
        String neighborsJson = getNeighborsOfFailedNode(nodeId);
        if (neighborsJson == null) {
            return null;
        }

        Pattern previousPattern = Pattern.compile("\"previousId\"\\s*:\\s*(\\d+)");
        Pattern nextPattern = Pattern.compile("\"nextId\"\\s*:\\s*(\\d+)");
        Matcher previousMatcher = previousPattern.matcher(neighborsJson);
        Matcher nextMatcher = nextPattern.matcher(neighborsJson);

        if (previousMatcher.find() && nextMatcher.find()) {
            return new int[]{
                    Integer.parseInt(previousMatcher.group(1)),
                    Integer.parseInt(nextMatcher.group(1))
            };
        }

        System.err.println("Could not parse neighbor payload: " + neighborsJson);
        return null;
    }

    public boolean pingPeer(String targetIp) {
        String url = "http://" + targetIp + ":8081/update/ping";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
