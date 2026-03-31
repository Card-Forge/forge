package forge.game.phase;

import forge.game.Game;
import forge.game.player.Player;

import java.util.*;

/**
 * Manages priority state for single-player games.
 * <p>
 * Tracks:
 * - The current player with priority
 * - The first player to receive priority in the current priority round
 * - Priority rotation to the next player in turn order
 * </p>
 * <p>
 * For team-based games where multiple players can have priority simultaneously,
 * use {@link TeamPriority} instead.
 * </p>
 */
public class Priority implements PriorityManager, java.io.Serializable {
    private static final long serialVersionUID = 1L;

    // Current player with priority
    private transient Player currentPlayer = null;

    // First player to receive priority in the current priority round
    private transient Player firstPlayer = null;

    // Reference to the game for accessing turn order
    private final transient Game game;

    /**
     * Creates a new Priority manager.
     *
     * @param game the game instance
     */
    public Priority(final Game game) {
        this.game = game;
    }

    /**
     * Gets the current player with priority.
     *
     * @return the current priority player, or null if not set
     */
    @Override
    public final Player getPriorityPlayer() {
        return currentPlayer;
    }


    /**
     * Sets priority to a specific player.
     *
     * @param player the player to give priority to
     */
    @Override
    public final void setPriority(final Player player) {
        // Mark this player as the first to receive priority in this round
        firstPlayer = player;
        currentPlayer = player;
    }

    /**
     * Resets priority to a specific player at the start of a new priority round.
     * This is typically called when priority returns to the turn player.
     *
     * @param player the player to reset priority to
     */
    @Override
    public final void resetPriority(final Player player) {
        setPriority(player);
    }


    /**
     * Advances priority to the next player in turn order.
     * <p>
     * This method checks if priority has come full circle (returned to {@code firstPlayer}).
     * It updates {@code currentPlayer} to the next player but keeps the team priority set
     * intact so that team members continue to have priority.
     * </p>
     *
     * @return true if priority has completed a full circle (returned to {@code firstPlayer}),
     *         false otherwise
     */
    @Override
    public final boolean advancePriorityToNextPlayer() {
        Player nextPlayer = game.getNextPlayerAfter(currentPlayer);

        if (nextPlayer == null) {
            return false;
        }

        // Check if we've completed a full circle of priority
        boolean priorityRoundComplete = (firstPlayer == nextPlayer);

        // Advance to the next player
        currentPlayer = nextPlayer;

        return priorityRoundComplete;
    }

    /**
     * Gets the first player to receive priority in the current round.
     * This is used to detect when priority has cycled back to the beginning.
     *
     * @return the first player to receive priority in this round
     */
    @Override
    public final Player getStartOfPriorityPlayer() {
        return firstPlayer;
    }

    /**
     * Clears all priority state. Used when priority needs to be reset
     * (e.g., during a phase change or game initialization).
     */
    @Override
    public final void clear() {
        currentPlayer = null;
        firstPlayer = null;
    }

    /**
     * Gets a formatted message representing the active player with priority.
     *
     * @return the player name, or empty string if no player has priority
     */
    @Override
    public final String getActivePlayerMessage() {
        if (currentPlayer != null) {
            return currentPlayer.toString();
        }
        return "";
    }
}
