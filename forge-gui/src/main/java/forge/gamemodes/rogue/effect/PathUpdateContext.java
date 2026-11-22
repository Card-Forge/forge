package forge.gamemodes.rogue.effect;

/**
 * Mutable context for onPathUpdate triggers.
 * Effects can modify path visibility behavior.
 */
public class PathUpdateContext {
    public boolean hidePlanes;
    public boolean allowAllNodesInCurrentRow;
    public int additionalVisiblePlaneboundRows;
    public int remainingPlaneboundRerolls;
}
