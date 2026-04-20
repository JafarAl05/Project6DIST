package systemy.namingserver;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MapManager {
    private ConcurrentHashMap<Integer, String> nameMap;

    public MapManager() {
        try {
            nameMap = FileStorage.loadMap("nameserver_map.json");
        } catch (IOException e) {
            System.out.println("IOException at MapManager Init.");
        }
    }

    public ConcurrentHashMap<Integer, String> getNameMap() {
        return nameMap;
    }

    public void addNode(int nodeID, String IP) {
        nameMap.put(nodeID, IP);
        try {
            FileStorage.storeMap(nameMap);
        } catch (IOException e) {
            System.out.println("IOException at MapManager: addNode.");
        }
    }

    public void removeNode(int nodeID, String IP) {
        boolean success = nameMap.remove(nodeID, IP);
        if (!success) {
            System.out.println("No Match found for node: " + nodeID + " at IP: " + IP + ". Skipping removal request.");
        }
        try {
            FileStorage.storeMap(nameMap);
        } catch (IOException e) {
            System.out.println("IOException at MapManager: removeNode.");
        }

    }

    public String getIP(int nodeID) {
        return nameMap.get(nodeID);
    }

    public int[] getNeighbors(int failedNodeId) {
        // Get all current node IDs and sort them
        List<Integer> sortedIds = new ArrayList<>(nameMap.keySet());
        Collections.sort(sortedIds);

        // Failsafe: if there are not enough nodes to form a ring
        if (sortedIds.size() <= 1) {
            return new int[]{failedNodeId, failedNodeId};
        }

        // Find where the failed node is in the sorted list
        int index = sortedIds.indexOf(failedNodeId);

        if (index == -1) {
            // Node not found, return something safe or handle the error
            return new int[]{-1, -1};
        }

        // Calculate Previous (with wrap-around if it's the very first node)
        int previousId;
        if (index == 0) {
            previousId = sortedIds.get(sortedIds.size() - 1); // Wrap to the last element
        } else {
            previousId = sortedIds.get(index - 1);
        }

        // Calculate Next (with wrap-around if it's the very last node)
        int nextId;
        if (index == sortedIds.size() - 1) {
            nextId = sortedIds.get(0); // Wrap to the first element
        } else {
            nextId = sortedIds.get(index + 1);
        }

        return new int[]{previousId, nextId};
    }
}
