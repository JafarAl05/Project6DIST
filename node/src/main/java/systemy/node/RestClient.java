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


    public void removeNode(int nodeId, String ipAddress) {
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
}