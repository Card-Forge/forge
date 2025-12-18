package forge.gamemodes.rogue;

/**
 * Represents the current state of a Rogue Commander run.
 */
public enum RogueRunState {
    /** Run has been created but not started */
    INITIAL,

    /** Run is in progress */
    STARTED,

    /** Run was completed successfully (defeated final boss) */
    WON,

    /** Run failed (lost a match) */
    LOST
}
