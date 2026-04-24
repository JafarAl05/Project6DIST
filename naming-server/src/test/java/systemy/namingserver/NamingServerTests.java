package systemy.namingserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "systemy.multicast.enabled=false"
)
class NamingServerTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MapManager mapManager;

    @BeforeEach
    void resetState() {
        mapManager.clear();
    }

    @Test
    void registerNodeRejectsDuplicates() {
        NodeRegistrationRequest request = new NodeRegistrationRequest(1000, "192.168.1.10");

        ResponseEntity<String> firstResponse = restTemplate.postForEntity(url("/nodes"), request, String.class);
        ResponseEntity<String> secondResponse = restTemplate.postForEntity(url("/nodes"), request, String.class);

        assertEquals(HttpStatus.OK, firstResponse.getStatusCode());
        assertEquals(HttpStatus.CONFLICT, secondResponse.getStatusCode());
    }

    @Test
    void fileLookupReturnsOwnerIp() {
        mapManager.addNode(1000, "192.168.1.10");
        mapManager.addNode(2000, "192.168.1.20");

        ResponseEntity<NodeResponse> response =
                restTemplate.getForEntity(url("/files/test-file.txt"), NodeResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().ipAddress());
    }

    @Test
    void fileLookupWithoutNodesReturnsServiceUnavailable() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(url("/files/test-file.txt"), String.class);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void getNeighborsReturnsPreviousAndNext() {
        mapManager.addNode(1000, "192.168.1.10");
        mapManager.addNode(2000, "192.168.1.20");
        mapManager.addNode(3000, "192.168.1.30");

        ResponseEntity<NodeNeighbors> response =
                restTemplate.getForEntity(url("/nodes/2000/neighbors"), NodeNeighbors.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1000, response.getBody().previousId());
        assertEquals(3000, response.getBody().nextId());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
