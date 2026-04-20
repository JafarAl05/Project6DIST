package systemy.node;

public class NeighborInfo {

    // Your ID (never changes so final)
    private final int currentID;

    // These will change as nodes join, leave, or crash
    private int previousID;
    private int nextID;

    /**
     * Constructor:
     * When a node first starts, it assumes it is the ONLY node in the network.
     * Therefore, it acts as its own previous and next node!
     */
    public NeighborInfo(int currentID) {
        this.currentID = currentID;
        this.previousID = currentID;
        this.nextID = currentID;
    }

    // --- Getters ---
    public int getCurrentID() {
        return currentID;
    }

    public int getPreviousID() {
        return previousID;
    }

    public int getNextID() {
        return nextID;
    }

    // --- Setters ---
    public void setPreviousID(int previousID) {
        this.previousID = previousID;
    }

    public void setNextID(int nextID) {
        this.nextID = nextID;
    }

    //visualizer so you can print your ring status to the console it has all the ID's of a node
    @Override
    public String toString() {
        return String.format("Ring Status: [Prev: %d] <-- [Me: %d] --> [Next: %d]", previousID, currentID, nextID);
    }
}