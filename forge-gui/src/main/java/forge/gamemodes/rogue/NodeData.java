package forge.gamemodes.rogue;

/**
 * Represents a single node in a Rogue Commander path.
 * Nodes can be Planes (matches), Sanctums (heal/remove), or Boss encounters.
 */
public class NodeData {

    public enum NodeType {
        PLANE,      // Standard match vs Planebound opponent
        SANCTUM,    // Heal life + remove cards from deck
        BAZAAR,     // Buy cards and loot
        EVENT,      // Random event
        LOOT,       // Loot reward
        ELITE,      // Elite plane
        BOSS,       // Final boss plane
    }

    private NodeType type;
    private PlaneboundConfig planeboundConfig;  // Config for Plane type
    private boolean completed;                  // Has this node been completed?
    private int rowIndex;                       // Which row this node is on (for life scaling: 5 + 5*rowIndex)

    // For Sanctum nodes
    private int healAmount;             // Life to restore (default: 5)
    private int freeRemoves;            // Cards that can be removed (default: 3)

    // Constructors
    public NodeData() {
        this.completed = false;
    }

    public NodeData(NodeType type) {
        this.type = type;
        this.completed = false;
    }

    // Factory methods for convenience
    public static NodeData createPlane(PlaneboundConfig planeboundConfig) {
        NodeData node = new NodeData(NodeType.PLANE);
        node.setPlaneBoundConfig(planeboundConfig);
        return node;
    }

    public static NodeData createBoss(PlaneboundConfig planeboundConfig) {
        NodeData node = new NodeData(NodeType.BOSS);
        node.setPlaneBoundConfig(planeboundConfig);
        return node;
    }

    public static NodeData createSanctum(int healAmount, int freeRemoves) {
        NodeData node = new NodeData(NodeType.SANCTUM);
        node.setHealAmount(healAmount);
        node.setFreeRemoves(freeRemoves);
        return node;
    }

    // Getters and Setters
    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public PlaneboundConfig getPlaneBoundConfig() {
        return planeboundConfig;
    }

    public void setPlaneBoundConfig(PlaneboundConfig planeboundConfig) {
        this.planeboundConfig = planeboundConfig;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getHealAmount() {
        return healAmount;
    }

    public void setHealAmount(int healAmount) {
        this.healAmount = healAmount;
    }

    public int getFreeRemoves() {
        return freeRemoves;
    }

    public void setFreeRemoves(int freeRemoves) {
        this.freeRemoves = freeRemoves;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    @Override
    public String toString() {
        switch (type) {
            case PLANE:
                return "Plane: " + planeboundConfig.planeName() + " (vs " + planeboundConfig.planeboundName() + ")";
            case BOSS:
                return "Boss: " + planeboundConfig.planeName() + " (vs " + planeboundConfig.planeboundName() + ")";
            case SANCTUM:
                return "Sanctum (Heal " + healAmount + ", Remove up to " + freeRemoves + ")";
            default:
                return "Unknown Node";
        }
    }
}
