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
        System.out.println("Total map size is: " + mapManager.getNameMap().size());
        System.out.println("Map looks like:" + mapManager.getNameMap().);

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
    public ResponseEntity<NodeNeighbors> getNeighbors(@PathVariable int nodeId) {
        // 1. Ask your MapManager for the answer (using the method you just built!)
        int[] neighbors = mapManager.getNeighbors(nodeId);

        // 2. If getNeighbors returns -1, the node didn't exist in the map
        if (neighbors[0] == -1) {
            return ResponseEntity.notFound().build();
        }

        // 3. Return the Java Record (Spring Boot automatically turns this into clean JSON!)
        return ResponseEntity.ok(new NodeNeighbors(neighbors[0], neighbors[1]));
    }

}