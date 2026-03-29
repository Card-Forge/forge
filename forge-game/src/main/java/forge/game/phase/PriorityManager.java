package forge.game.phase;

import forge.game.player.Player;

import java.util.List;

/**
 * Interface for managing priority state during a game phase.
 * <p>
 * Implementations can handle single-player priority or team-based priority
 * where multiple players share priority simultaneously.
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
     * Gets all players who currently share priority.
     * In single-player mode, returns a list with just the priority player.
     * In team mode, returns the team captain and all in-game team members.
     *
     * @return a list of players with priority
     */
    List<Player> getPlayersWithPriority();

    /**
     * Sets priority to a specific player.
     * In team mode, also includes all team members.
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
}

