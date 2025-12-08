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
    private String planeName;           // For PLANE/BOSS: e.g., "Dominaria"
    private String planeboundName;      // For PLANE/BOSS: e.g., "Meria, Scholar of Antiquity"
    private String deckPath;            // Path to Planebound deck file
    private boolean completed;          // Has this node been completed?

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
    public static NodeData createPlane(String planeName, String planeboundName, String deckPath) {
        NodeData node = new NodeData(NodeType.PLANE);
        node.setPlaneName(planeName);
        node.setPlaneboundName(planeboundName);
        node.setDeckPath(deckPath);
        return node;
    }

    public static NodeData createBoss(String planeName, String planeboundName, String deckPath) {
        NodeData node = new NodeData(NodeType.BOSS);
        node.setPlaneName(planeName);
        node.setPlaneboundName(planeboundName);
        node.setDeckPath(deckPath);
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

    public String getPlaneName() {
        return planeName;
    }

    public void setPlaneName(String planeName) {
        this.planeName = planeName;
    }

    public String getPlaneboundName() {
        return planeboundName;
    }

    public void setPlaneboundName(String planeboundName) {
        this.planeboundName = planeboundName;
    }

    public String getDeckPath() {
        return deckPath;
    }

    public void setDeckPath(String deckPath) {
        this.deckPath = deckPath;
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

    @Override
    public String toString() {
        switch (type) {
            case PLANE:
                return "Plane: " + planeName + " (vs " + planeboundName + ")";
            case BOSS:
                return "Boss: " + planeName + " (vs " + planeboundName + ")";
            case SANCTUM:
                return "Sanctum (Heal " + healAmount + ", Remove up to " + freeRemoves + ")";
            default:
                return "Unknown Node";
        }
    }
}
