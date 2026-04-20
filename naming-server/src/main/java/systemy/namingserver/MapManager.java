package systemy.namingserver;

import org.springframework.stereotype.Service;
import java.io.IOException;
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

    public void notifyNodes(String message) {
        for (int key : nameMap.keySet()) {
            System.out.println("Sent: " + message + "to " + nameMap.get(key));
        }
        notifyNS(message);
    }

    public void notifyNS(String message) {
        System.out.println("Sent: " + message + "to Name Server");
    }
}
