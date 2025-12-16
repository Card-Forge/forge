package forge.gamemodes.rogue;

/**
 * Represents a single node in a Rogue Commander path.
 * Nodes can be Planes (matches), Sanctums (heal/remove), or Boss encounters.
 */
public class RoguePathNode {

    public enum NodeType {
        PLANE(2, 2),       // Standard match vs Planebound opponent
        SANCTUM(0, 0),      // Heal life + remove cards from deck
        BAZAAR(0, 0),       // Buy cards and loot
        EVENT(0, 0),        // Random event
        LOOT(0, 0),         // Loot reward
        ELITE(4, 4),       // Elite plane
        BOSS(8, 8);        // Final boss plane

        private final int goldReward;
        private final int echoReward;

        NodeType(int goldReward, int echoReward) {
            this.goldReward = goldReward;
            this.echoReward = echoReward;
        }

        public int getGoldReward() {
            return goldReward;
        }

        public int getEchoReward() {
            return echoReward;
        }
    }

    private NodeType type;
    private RoguePlanebound roguePlanebound;  // Config for Plane type
    private boolean completed;                  // Has this node been completed?
    private int rowIndex;                       // Which row this node is on (for life scaling: 5 + 5*rowIndex)

    // For Sanctum nodes
    private int healAmount;             // Life to restore (default: 5)
    private int freeRemoves;            // Cards that can be removed (default: 3)

    // Constructors
    public RoguePathNode() {
        this.completed = false;
    }

    public RoguePathNode(NodeType type) {
        this.type = type;
        this.completed = false;
    }

    // Factory methods for convenience
    public static RoguePathNode createPlane(RoguePlanebound roguePlanebound) {
        RoguePathNode node = new RoguePathNode(NodeType.PLANE);
        node.setPlaneBoundConfig(roguePlanebound);
        return node;
    }

    public static RoguePathNode createBoss(RoguePlanebound roguePlanebound) {
        RoguePathNode node = new RoguePathNode(NodeType.BOSS);
        node.setPlaneBoundConfig(roguePlanebound);
        return node;
    }

    public static RoguePathNode createSanctum(int healAmount, int freeRemoves) {
        RoguePathNode node = new RoguePathNode(NodeType.SANCTUM);
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

    public RoguePlanebound getPlaneBoundConfig() {
        return roguePlanebound;
    }

    public void setPlaneBoundConfig(RoguePlanebound roguePlanebound) {
        this.roguePlanebound = roguePlanebound;
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
                return "Plane: " + roguePlanebound.planeName() + " (vs " + roguePlanebound.planeboundName() + ")";
            case BOSS:
                return "Boss: " + roguePlanebound.planeName() + " (vs " + roguePlanebound.planeboundName() + ")";
            case SANCTUM:
                return "Sanctum (Heal " + healAmount + ", Remove up to " + freeRemoves + ")";
            default:
                return "Unknown Node";
        }
    }
}
