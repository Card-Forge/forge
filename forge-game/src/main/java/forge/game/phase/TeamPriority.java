package forge.game.phase;

import forge.game.Game;
import forge.game.player.Player;
import forge.game.player.Team;

import java.util.*;

/**
 * Manages priority state for team-based games where multiple players can have
 * priority simultaneously.
 * <p>
 * Extends single-player priority logic with support for team members who share
 * priority with the team captain. All team members are considered to have priority
 * at the same time and act simultaneously.
 * </p>
 */
public class TeamPriority implements PriorityManager, java.io.Serializable {
    private static final long serialVersionUID = 1L;

    // Current player with priority (typically team captain)
    private transient Player currentPlayer = null;

    // First player to receive priority in the current priority round
    private transient Player firstPlayer = null;

    // Set of all players with priority. Preserve insertion order for predictable
    // rotation/selection (captain should be first member of team).
    private final transient LinkedHashSet<Player> playersWithPriority = new LinkedHashSet<>();

    // Reference to the game for accessing turn order and game rules
    private final transient Game game;

    /**
     * Creates a new TeamPriority manager for team-based games.
     *
     * @param game the game instance
     */
    public TeamPriority(final Game game) {
        this.game = game;
    }

    /**
     * Gets the current player with priority (typically the team captain).
     *
     * @return the current priority player, or null if not set
     */
    @Override
    public final Player getPriorityPlayer() {
        return currentPlayer;
    }

    /**
     * Gets all players who currently share priority (team members).
     * The first element in the list is the team's captain.
     *
     * @return an ordered list of players with priority
     */
    @Override
    public final List<Player> getPlayersWithPriority() {
        if (playersWithPriority == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(playersWithPriority);
    }

    /**
     * Sets priority to a specific player and their team members.
     * All in-game team members are automatically included in the priority set.
     *
     * @param player the player to give priority to (typically team captain)
     */
    @Override
    public final void setPriority(final Player player) {
        // Mark this player as the first to receive priority in this round
        firstPlayer = player;
        currentPlayer = player;
        updatePlayersWithPriorityFrom(player);
    }

    /**
     * Resets priority to a specific player at the start of a new priority round.
     * This is typically called when priority returns to the turn player.
     *
     * @param player the player to reset priority to (typically team captain)
     */
    @Override
    public final void resetPriority(final Player player) {
        setPriority(player);
    }

    /**
     * Updates the set of players with priority based on a given player.
     * Includes the player and all in-game team members.
     *
     * @param player the primary player to get priority
     */
    private void updatePlayersWithPriorityFrom(final Player player) {
        playersWithPriority.clear();
        if (player == null) {
            return;
        }

        // Add the player first (captain)
        playersWithPriority.add(player);

        // Add team members
        if (player.getTeamObject() != null && player.getTeamObject() != Team.UNASSIGNED) {
            for (Player member : player.getTeamObject().getMembers()) {
                if (member.equals(player)) {
                    // Skip the captain, already added
                    continue;
                }
                if (member.isInGame()) {
                    playersWithPriority.add(member);
                }
            }
        }
    }

    /**
     * Advances priority to the next player's team in turn order.
     * <p>
     * This method checks if priority has come full circle (returned to the first team).
     * It updates {@code currentPlayer} to the next player and includes all their team members.
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

        // Advance to the next player, and update team members for new priority holder
        currentPlayer = nextPlayer;
        updatePlayersWithPriorityFrom(currentPlayer);

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
        playersWithPriority.clear();
    }
}

