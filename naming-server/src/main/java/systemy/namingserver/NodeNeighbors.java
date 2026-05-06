package systemy.namingserver;

public class NodeNeighbors {
    private int previousId;
    private int nextId;

    // Empty constructor required by Jackson
    public NodeNeighbors() {}

    public NodeNeighbors(int previousId, int nextId) {
        this.previousId = previousId;
        this.nextId = nextId;
    }

    public int getPreviousId() { return previousId; }
    public void setPreviousId(int previousId) { this.previousId = previousId; }

    public int getNextId() { return nextId; }
    public void setNextId(int nextId) { this.nextId = nextId; }
}