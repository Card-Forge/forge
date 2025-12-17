package forge.gamemodes.rogue;

/**
 * Types of planebound encounters with associated rewards.
 */
public enum RoguePlaneboundType {
    NORMAL(2, 2),   // Standard planebound encounter
    ELITE(4, 4),    // Elite difficulty planebound
    BOSS(8, 8);     // Boss encounter

    private final int goldReward;
    private final int echoReward;

    RoguePlaneboundType(int goldReward, int echoReward) {
        this.goldReward = goldReward;
        this.echoReward = echoReward;
    }

    public int getGoldReward() {
        return goldReward;
    }

    public int getEchoReward() {
        return echoReward;
    }

    /**
     * Get RoguePlaneboundType from integer index.
     * @param index 0=NORMAL, 1=ELITE, 2=BOSS
     * @return The corresponding type, defaults to NORMAL if invalid
     */
    public static RoguePlaneboundType fromIndex(int index) {
        return switch (index) {
            case 1 -> ELITE;
            case 2 -> BOSS;
            default -> NORMAL;
        };
    }
}
