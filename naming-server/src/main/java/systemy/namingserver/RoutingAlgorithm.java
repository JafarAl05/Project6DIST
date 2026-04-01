package systemy.namingserver;

import java.util.Set;

public class RoutingAlgorithm {

    public static int findOwnerNode(int fileHash, Set<Integer> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return -1; // Failsafe if the network has no nodes yet
        }

        int closestNode = -1;
        int smallestDifference = Integer.MAX_VALUE;
        int maxNodeId = -1; // To track the biggest hash overall

        for (int nodeId : nodeIds) {
            // Keep track of the absolute biggest hash in the map
            if (nodeId > maxNodeId) {
                maxNodeId = nodeId;
            }

            // Look only at nodes with a hash SMALLER than the file hash
            if (nodeId < fileHash) {
                int difference = fileHash - nodeId;

                // Find the node with the smallest difference
                if (difference < smallestDifference) {
                    smallestDifference = difference;
                    closestNode = nodeId;
                }
            }
        }

        // If the collection of smaller nodes was empty, default to the biggest hash
        if (closestNode == -1) {
            return maxNodeId;
        }

        return closestNode;
    }
}