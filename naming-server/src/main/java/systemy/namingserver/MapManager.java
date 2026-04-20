package systemy.namingserver;

import java.io.IOException;
import java.util.HashMap;

public class MapManager {
    private HashMap<Integer, String> nameMap;

    public MapManager() {
        try {
            nameMap = FileStorage.loadMap("nameserver_map.json");
        } catch (IOException e) {
            System.out.println("IOException at MapManager Init.");
        }
    }

    public HashMap<Integer, String> getNameMap() {
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
}
