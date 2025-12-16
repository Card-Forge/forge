package forge.gamemodes.rogue;

/**
 * Abstract base class for all node types in a Rogue Commander path.
 * Subclasses define specific behavior and properties for different node types.
 */
public abstract class RoguePathNode {

    private boolean completed;
    private int rowIndex;

    protected RoguePathNode() {
        this.completed = false;
        this.rowIndex = 0;
    }

    // Common getters and setters
    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    /**
     * Get a string representation of this node for display purposes.
     * Subclasses should override this to provide specific information.
     */
    @Override
    public abstract String toString();
}
