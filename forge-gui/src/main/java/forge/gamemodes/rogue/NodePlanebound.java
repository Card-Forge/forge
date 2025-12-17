package forge.gamemodes.rogue;

/**
 * Represents a planebound encounter node in a Rogue Commander path.
 * This node type involves combat against a planebound opponent on their plane.
 * The type (NORMAL/ELITE/BOSS) is now defined in the RoguePlanebound itself.
 */
public class NodePlanebound extends RoguePathNode {

    private RoguePlanebound roguePlanebound;

    public NodePlanebound() {
        super();
    }

    public NodePlanebound(RoguePlanebound roguePlanebound) {
        super();
        this.roguePlanebound = roguePlanebound;
    }

    // Getters and Setters
    public RoguePlanebound getRoguePlanebound() {
        return roguePlanebound;
    }

    public void setRoguePlanebound(RoguePlanebound roguePlanebound) {
        this.roguePlanebound = roguePlanebound;
    }

    public RoguePlaneboundType getPlaneboundType() {
        return roguePlanebound != null ? roguePlanebound.type() : RoguePlaneboundType.NORMAL;
    }

    // Convenience methods for rewards
    public int getGoldReward() {
        return getPlaneboundType().getGoldReward();
    }

    public int getEchoReward() {
        return getPlaneboundType().getEchoReward();
    }

    @Override
    public String toString() {
        RoguePlaneboundType type = getPlaneboundType();
        String typeStr = type == RoguePlaneboundType.BOSS ? "Boss" :
                        type == RoguePlaneboundType.ELITE ? "Elite" : "Plane";
        return typeStr + ": " + roguePlanebound.planeName() + " (vs " + roguePlanebound.planeboundName() + ")";
    }
}
