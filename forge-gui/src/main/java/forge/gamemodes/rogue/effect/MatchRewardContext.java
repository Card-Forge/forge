package forge.gamemodes.rogue.effect;

/**
 * Mutable context for onBeforeRewards triggers.
 * Effects can modify the overall reward package after a match win.
 */
public class MatchRewardContext {
    public boolean skipRewards;
    public int goldRewardAdjustment;
    public int nonMythicCardCountAdjustment;
}
