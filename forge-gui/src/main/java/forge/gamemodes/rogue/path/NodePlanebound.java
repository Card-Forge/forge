package forge.gamemodes.rogue.path;

import forge.gamemodes.rogue.RoguePlanebound;
import forge.gamemodes.rogue.RoguePlaneboundType;
import forge.util.MyRandom;
import java.util.Random;

/**
 * Represents a planebound encounter node in a Rogue Commander path.
 * This node type involves combat against a planebound opponent on their plane.
 * The type (NORMAL/ELITE/BOSS) is now defined in the RoguePlanebound itself.
 */
public class NodePlanebound extends RoguePathNode {

    private static final int MAX_RANDOM_MARKERS_PER_TYPE = 2;
    private static final int MAX_RANDOM_MARKERS = 3;

    private RoguePlanebound roguePlanebound;
    private int wrathfulCount;
    private int cursedCount;
    private int startingLifeModification;
    private boolean revealed;

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

    public int getWrathfulCount() {
        return wrathfulCount;
    }

    public void setWrathfulCount(int wrathfulCount) {
        this.wrathfulCount = wrathfulCount;
    }

    public int getCursedCount() {
        return cursedCount;
    }

    public void setCursedCount(int cursedCount) {
        this.cursedCount = cursedCount;
    }

    public boolean canAddRandomWrathfulMarker() {
        return wrathfulCount < MAX_RANDOM_MARKERS_PER_TYPE;
    }

    public boolean canAddRandomCursedMarker() {
        return cursedCount < MAX_RANDOM_MARKERS_PER_TYPE
            && wrathfulCount + cursedCount < MAX_RANDOM_MARKERS;
    }

    public void rerollMarkers(boolean wrathfulUnlocked, boolean cursedUnlocked) {
        if (wrathfulCount <= 0 && cursedCount <= 0) {
            return;
        }

        int maxWrathful = wrathfulUnlocked || wrathfulCount > 0 ? MAX_RANDOM_MARKERS_PER_TYPE : 0;
        int maxCursed = cursedUnlocked || cursedCount > 0 ? MAX_RANDOM_MARKERS_PER_TYPE : 0;
        Random rng = MyRandom.getRandom();
        int total = rng.nextInt(Math.min(MAX_RANDOM_MARKERS, maxWrathful + maxCursed) + 1);
        int minWrathful = Math.max(0, total - maxCursed);
        wrathfulCount = minWrathful + rng.nextInt(Math.min(total, maxWrathful) - minWrathful + 1);
        cursedCount = total - wrathfulCount;
    }

    public int getStartingLifeModification() {
        return startingLifeModification;
    }

    public void setStartingLifeModification(int startingLifeModification) {
        this.startingLifeModification = startingLifeModification;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }

    public RoguePlaneboundType getPlaneboundType() {
        return roguePlanebound != null ? roguePlanebound.type() : RoguePlaneboundType.NORMAL;
    }

    // set life based on Planebound rows + Planebound type
    public int getPlaneboundLife(int pathRowCount) {
        int rowLife = pathRowCount * 5;
        int planeboundTypeBaseLife;

        if (getPlaneboundType().equals(RoguePlaneboundType.BOSS)) {
            return 30 + startingLifeModification;
        }
        else if (getPlaneboundType().equals(RoguePlaneboundType.ELITE)) {
            planeboundTypeBaseLife = 5;
        }
        else {
            planeboundTypeBaseLife = 0;
        }

        return rowLife + planeboundTypeBaseLife + startingLifeModification;
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
