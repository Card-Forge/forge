package forge.gamemodes.rogue;

/**
 * Represents a planebound encounter node in a Rogue Commander path.
 * This node type involves combat against a planebound opponent on their plane.
 */
public class NodePlanebound extends RoguePathNode {

    private RoguePlanebound roguePlanebound;
    private RoguePlaneboundType roguePlaneboundType;

    public NodePlanebound() {
        super();
        this.roguePlaneboundType = RoguePlaneboundType.NORMAL;
    }

    public NodePlanebound(RoguePlanebound roguePlanebound, RoguePlaneboundType roguePlaneboundType) {
        super();
        this.roguePlanebound = roguePlanebound;
        this.roguePlaneboundType = roguePlaneboundType;
    }

    // Factory methods for convenience
    public static NodePlanebound createNormal(RoguePlanebound roguePlanebound) {
        return new NodePlanebound(roguePlanebound, RoguePlaneboundType.NORMAL);
    }

    public static NodePlanebound createElite(RoguePlanebound roguePlanebound) {
        return new NodePlanebound(roguePlanebound, RoguePlaneboundType.ELITE);
    }

    public static NodePlanebound createBoss(RoguePlanebound roguePlanebound) {
        return new NodePlanebound(roguePlanebound, RoguePlaneboundType.BOSS);
    }

    // Getters and Setters
    public RoguePlanebound getRoguePlanebound() {
        return roguePlanebound;
    }

    public void setRoguePlanebound(RoguePlanebound roguePlanebound) {
        this.roguePlanebound = roguePlanebound;
    }

    public RoguePlaneboundType getPlaneboundType() {
        return roguePlaneboundType;
    }

    public void setPlaneboundType(RoguePlaneboundType roguePlaneboundType) {
        this.roguePlaneboundType = roguePlaneboundType;
    }

    // Convenience methods for rewards
    public int getGoldReward() {
        return roguePlaneboundType.getGoldReward();
    }

    public int getEchoReward() {
        return roguePlaneboundType.getEchoReward();
    }

    @Override
    public String toString() {
        String typeStr = roguePlaneboundType == RoguePlaneboundType.BOSS ? "Boss" :
                        roguePlaneboundType == RoguePlaneboundType.ELITE ? "Elite" : "Plane";
        return typeStr + ": " + roguePlanebound.planeName() + " (vs " + roguePlanebound.planeboundName() + ")";
    }
}
