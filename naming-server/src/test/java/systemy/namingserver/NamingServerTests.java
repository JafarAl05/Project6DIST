package systemy.namingserver;

import org.junit.jupiter.api.*;
import systemy.node.RestClient;
import systemy.common.HashingUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NamingServerTests {

    private static RestClient pc1Client;
    private static RestClient pc2Client;

    @BeforeAll
    public static void setup() {

        pc1Client = new RestClient();
        pc2Client = new RestClient();
        System.out.println("Starting Naming Server Test Suite...");
    }


    @Test
    @Order(1)
    @DisplayName("1. Add a node with a unique node name")
    public void testAddUniqueNode() {
        int nodeId = HashingUtil.hash("Node-Alpha");
        boolean success = pc1Client.registerNode(nodeId, "192.168.1.10");

        assertTrue(success, "Server should accept a brand new, unique node.");
    }


    @Test
    @Order(2)
    @DisplayName("2. Add a node with an existing node name")
    public void testAddExistingNode() {
        int nodeId = HashingUtil.hash("Node-Alpha"); // Same name as Test 1

        // Try to register it again with a different IP
        boolean success = pc1Client.registerNode(nodeId, "192.168.1.99");

        // Note: For this to pass, Role B MUST update their code to return a 409 error!
        assertFalse(success, "Server MUST reject a duplicate node ID and return false.");
    }


    @Test
    @Order(3)
    @DisplayName("3. Send a filename and get the correct IP address")
    public void testStandardRouting() {

        int nodeBetaId = HashingUtil.hash("Node-Beta");
        pc1Client.registerNode(nodeBetaId, "192.168.1.20");

        // Act: Ask for a standard file
        String resultIp = pc1Client.getFileLocation("standard_document.pdf");

        // Assert: We just need to ensure the server actually returned an IP, not an error.
        assertNotNull(resultIp);
        assertFalse(resultIp.startsWith("Error"), "Server failed to return an IP address for the file.");
    }


    @Test
    @Order(4)
    @DisplayName("4. Wrap-around routing (hash smaller than smallest node)")
    public void testWrapAroundRouting() {
        // 1. Find the biggest node currently in the network to act as our expected target
        int nodeOmegaId = HashingUtil.hash("Node-Omega");
        pc1Client.registerNode(nodeOmegaId, "192.168.1.99"); // Let's assume this is the biggest

        String tinyHashFile = "default";
        for (int i = 0; i < 10000; i++) {
            if (HashingUtil.hash("file" + i) < 10) {
                tinyHashFile = "file" + i;
                break;
            }
        }

        System.out.println("Testing wrap-around with file: " + tinyHashFile + " (Hash: " + HashingUtil.hash(tinyHashFile) + ")");

        // Act
        String resultIp = pc1Client.getFileLocation(tinyHashFile);

        // Assert: It shouldn't crash. It should wrap around.
        assertFalse(resultIp.startsWith("Error"), "Routing algorithm crashed on wrap-around edge case!");
    }


    @Test
    @Order(5)
    @DisplayName("5. Concurrency: Get file location and remove node simultaneously")
    public void testConcurrentReadAndDelete() throws InterruptedException {
        int targetNodeId = HashingUtil.hash("Node-Beta");

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread 1: Asking for a file
        executor.submit(() -> {
            try {
                latch.await(); // Wait for the starting pistol
                pc1Client.getFileLocation("some_file.mp4");
            } catch (Exception ignored) {}
        });

        // Thread 2: Deleting the node
        executor.submit(() -> {
            try {
                latch.await(); // Wait for the starting pistol
                pc2Client.removeNode(targetNodeId);
            } catch (Exception ignored) {}
        });

        // FIRE THE STARTING PISTOL - Both threads hit the server at the exact same time
        latch.countDown();

        executor.shutdown();
        boolean finished = executor.awaitTermination(3, TimeUnit.SECONDS);
        assertTrue(finished, "Concurrency test deadlocked or timed out.");
    }

    @Test
    @Order(6)
    @DisplayName("6. Concurrency: Two PCs asking for a file simultaneously")
    public void testConcurrentReadsFromTwoPCs() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // PC 1 asking
        executor.submit(() -> {
            try {
                latch.await();
                pc1Client.getFileLocation("viral_video.mp4");
            } catch (Exception ignored) {}
        });

        // PC 2 asking
        executor.submit(() -> {
            try {
                latch.await();
                pc2Client.getFileLocation("viral_video.mp4");
            } catch (Exception ignored) {}
        });

        // FIRE THE STARTING PISTOL
        latch.countDown();

        executor.shutdown();
        boolean finished = executor.awaitTermination(3, TimeUnit.SECONDS);
        assertTrue(finished, "Concurrency test deadlocked or timed out.");
    }

    @AfterAll
    public static void tearDown() {
        // Clean up the network so we don't pollute the server for the next run
        pc1Client.removeNode(HashingUtil.hash("Node-Alpha"));
        pc1Client.removeNode(HashingUtil.hash("Node-Omega"));
    }
}