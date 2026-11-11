package forge.gamemodes.rogue.path;

/**
 * Abstract base class for all node types in a Rogue Commander path.
 * Subclasses define specific behavior and properties for different node types.
 */
public abstract class RoguePathNode {

    private boolean completed;
    private int rowIndex;
    private int columnIndex = 0; // Column position within row

    protected RoguePathNode() {
        this.completed = false;
        this.rowIndex = 0;
        this.columnIndex = 0;
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

    public int getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }

    public boolean isSideNode() {
        return false;
    }

    /**
     * Get a string representation of this node for display purposes.
     * Subclasses should override this to provide specific information.
     */
    @Override
    public abstract String toString();
}
