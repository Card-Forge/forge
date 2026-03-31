package forge.game.phase;

import forge.game.player.Player;

/**
 * Interface for managing priority state during a game phase.
 * <p>
 * Implementations can handle single-player priority or team-based priority
 * where team members get priority sequentially.
 * </p>
 */
public interface PriorityManager {

    /**
     * Gets the current player with priority.
     *
     * @return the current priority player, or null if not set
     */
    Player getPriorityPlayer();

    /**
     * Sets priority to a specific player.
     * In team mode, also initializes the team member sequence.
     *
     * @param player the player to give priority to
     */
    void setPriority(Player player);

    /**
     * Resets priority to a specific player at the start of a new priority round.
     *
     * @param player the player to reset priority to
     */
    void resetPriority(Player player);

    /**
     * Advances priority to the next player in turn order.
     * <p>
     * In team mode, cycles through team members sequentially before moving to
     * the next team.
     * </p>
     *
     * @return true if priority has completed a full circle (returned to first player),
     *         false otherwise
     */
    boolean advancePriorityToNextPlayer();

    /**
     * Gets the first player to receive priority in the current round.
     *
     * @return the first player to receive priority in this round
     */
    Player getStartOfPriorityPlayer();

    /**
     * Clears all priority state.
     */
    void clear();

    /**
     * Gets a formatted message representing the active player(s) with priority.
     * For single-player: returns just the player name.
     * For team mode: returns "Player, (Teammate)" to show who has priority and who's next.
     *
     * @return formatted string with active player information
     */
    String getActivePlayerMessage();
}

