package forge.game.phase;

/**
 * Describes the outcome of a single priority step executed by an
 * {@link IPriorityManager}.
 *
 * <p>The {@link forge.game.phase.PhaseHandler} uses this value to decide
 * whether to advance the phase, resolve the top of the stack, or simply
 * continue the main game loop.</p>
 */
public enum PriorityResult {

    /**
     * The current priority player cast a spell or activated an ability.
     * Priority recirculates; the game loop continues with no phase change.
     */
    ACTION_TAKEN,

    /**
     * All players have passed priority in sequence and the stack is <em>empty</em>.
     * The caller should end the current phase and advance to the next one.
     */
    ALL_PASSED,

    /**
     * The game ended during this step (e.g. a player conceded, or
     * state-based actions caused a win/loss).  The caller should return
     * immediately without further processing.
     */
    GAME_OVER
}

