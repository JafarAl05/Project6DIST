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

    // =========================================================================
    // 1. REGISTRATION (Updated for Role B's NodeRegistrationRequest record)
    // =========================================================================
    // Notice it now takes 'int nodeId' instead of 'String nodeName'
    public boolean registerNode(int nodeId, String ipAddress) {
        String url = BASE_URL + "/nodes";

        // MATCHING ROLE B: {"nodeId": 1234, "ipAddress": "192.168.1.5"}
        String jsonPayload = String.format("{\"nodeId\": %d, \"ipAddress\": \"%s\"}", nodeId, ipAddress);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Role B's code just returns 200 OK with a success string.
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

    // =========================================================================
    // 2. REMOVAL (Updated for Role B's DeleteMapping)
    // =========================================================================
    public void removeNode(int nodeId, String ipAddress) {
        // MATCHING ROLE B: DELETE /nodes/{nodeId}?ip={ip}
        String url = BASE_URL + "/nodes/" + nodeId + "?ip=" + ipAddress;

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

    // =========================================================================
    // 3. FILE LOCATION (Updated Regex for NodeResponse record)
    // =========================================================================
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

    // =========================================================================
    // HELPER: Regex JSON Parser (Updated for camelCase "ipAddress")
    // =========================================================================
    private String extractIpFromJson(String json) {
        // MATCHING ROLE B: Now looking for "ipAddress" instead of "ip_address"
        Pattern pattern = Pattern.compile("\"ipAddress\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "Error: Could not parse IP from response -> " + json;
        }
    }
}