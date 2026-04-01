package systemy.namingserver;
import org.springframework.web.bind.annotation.*;

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
    public String registerNode(@RequestBody NodeRegistrationRequest request) {
        mapManager.addNode(request.nodeId(), request.ipAddress());
        return "Node registered successfully!";
    }

    @DeleteMapping("/nodes/{nodeId}")
    public String removeNode(@PathVariable("nodeId") int nodeId, @RequestParam("ip") String ip) {
        mapManager.removeNode(nodeId, ip);
        return "Node removed successfully.";
    }
}