package systemy.namingserver;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static systemy.common.HashingUtil.hash;

@RestController
public class NamingController {
    private final MapManager mapManager = new MapManager();

    // Listen for GET requests asking for a file's location
    @GetMapping("/files/{filename}")
    public NodeResponse getFileLocation(@PathVariable("filename") String filename) {
        // Calculate the hash of the filename
        int fileHash = hash(filename);
        // Get the set of all node IDs currently registered in Robeen's Map
        Set<Integer> allNodeIds = mapManager.getNameMap().keySet();
        // Use routing algorithm to find the host ID
        int targetNodeId = RoutingAlgorithm.findOwnerNode(fileHash, allNodeIds);
        // Ask MapManager for the IP address attached to that Node ID
        String targetIp = mapManager.getIP(targetNodeId);

        // Return the dynamic JSON response
        return new NodeResponse(targetNodeId, targetIp);
    }

    @PostMapping("/nodes")
    public ResponseEntity<String> registerNode(@RequestBody NodeRegistrationRequest request) {
        // Check if the map already contains this node ID
        if (mapManager.getNameMap().containsKey(request.nodeId())) {
            // Return HTTP 409 Conflict if it's a duplicate
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Node already exists!");
        }

        mapManager.addNode(request.nodeId(), request.ipAddress());
        return ResponseEntity.ok("Node registered successfully!");
    }

    @DeleteMapping("/nodes/{nodeId}")
    public String removeNode(@PathVariable("nodeId") int nodeId, @RequestParam("ip") String ip) {
        mapManager.removeNode(nodeId, ip);
        return "Node removed successfully.";
    }

    @GetMapping("/nodes/{nodeId}/ip")
    public ResponseEntity<String> getNodeIp(@PathVariable int nodeId) {
        String ip = mapManager.getIP(nodeId);
        if (ip != null) {
            return ResponseEntity.ok(ip);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/nodes/{nodeId}/neighbors")
    public ResponseEntity<String> getNeighbors(@PathVariable int nodeId) {
        Set<Integer> keySet = mapManager.getNameMap().keySet();

        // Failsafe: If there is only 1 node, it is its own neighbor
        if (keySet.size() <= 1) {
            String json = String.format("{\"previousID\": %d, \"nextID\": %d}", nodeId, nodeId);
            return ResponseEntity.ok(json);
        }

        // 1. Put the ring in a sorted list so they form a proper number line
        List<Integer> sortedNodes = new ArrayList<>(keySet);
        Collections.sort(sortedNodes);

        // 2. Find where the target node sits in the list
        int currentIndex = sortedNodes.indexOf(nodeId);

        if (currentIndex == -1) {
            return ResponseEntity.notFound().build(); // The node doesn't exist!
        }

        // 3. Find the previous node (wrapping around to the end if we are at the very beginning)
        int previousId;
        if (currentIndex == 0) {
            previousId = sortedNodes.get(sortedNodes.size() - 1);
        } else {
            previousId = sortedNodes.get(currentIndex - 1);
        }

        // 4. Find the next node (wrapping around to the beginning if we are at the very end)
        int nextId;
        if (currentIndex == sortedNodes.size() - 1) {
            nextId = sortedNodes.get(0);
        } else {
            nextId = sortedNodes.get(currentIndex + 1);
        }

        // 5. Return it as a simple JSON string so Role C's RestClient can parse it
        String jsonResponse = String.format("{\"previousID\": %d, \"nextID\": %d}", previousId, nextId);
        return ResponseEntity.ok(jsonResponse);
    }
}