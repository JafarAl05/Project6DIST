package systemy.namingserver;

public class NodeRegistrationRequest {
    private int nodeId;
    private String ipAddress;

    public NodeRegistrationRequest() {}

    public NodeRegistrationRequest(int nodeId, String ipAddress) {
        this.nodeId = nodeId;
        this.ipAddress = ipAddress;
    }

    public int getNodeId() { return nodeId; }
    public void setNodeId(int nodeId) { this.nodeId = nodeId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}