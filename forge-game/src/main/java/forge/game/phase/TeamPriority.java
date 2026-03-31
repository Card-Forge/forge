package forge.game.phase;

import forge.game.Game;
import forge.game.player.Player;
import forge.game.player.Team;

import java.util.*;

/**
 * Manages priority state for team-based games where team members get priority
 * sequentially (one after another), not simultaneously.
 * <p>
 * When a team has priority, each team member gets their own priority in order.
 * After all team members pass, priority moves to the next team.
 * </p>
 */
public class TeamPriority implements PriorityManager, java.io.Serializable {
    private static final long serialVersionUID = 1L;

    // Current player with priority
    private transient Player currentPlayer = null;

    // First player to receive priority in the current priority round
    private transient Player firstPlayer = null;

    // Ordered list of current team members (for cycling through)
    private final transient List<Player> currentTeamMembers = new ArrayList<>();
    private final transient List<Team> alreadyGainedPriority = new ArrayList<>();

    // Index of current player within the team
    private transient int currentTeamMemberIndex = 0;

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
     * Gets the current player with priority.
     *
     * @return the current priority player, or null if not set
     */
    @Override
    public final Player getPriorityPlayer() {
        return currentPlayer;
    }


    /**
     * Sets priority to a specific player, who should be the team captain or
     * the player whose turn it is in the team.
     *
     * @param player the player to give priority to
     */
    @Override
    public final void setPriority(final Player player) {
        // Mark this player as the first to receive priority in this round
        firstPlayer = player;
        currentPlayer = player;

        alreadyGainedPriority.clear();
        // Initialize the team member list and index
        initializeTeamMembers(player);
    }

    /**
     * Resets priority to a specific player at the start of a new priority round.
     *
     * @param player the player to reset priority to
     */
    @Override
    public final void resetPriority(final Player player) {
        setPriority(player);
    }

    /**
     * Initializes the list of team members for the given player.
     * Ensures the given player is first, then adds other in-game team members.
     *
     * @param player the player whose team to initialize
     */
    private void initializeTeamMembers(final Player player) {
        currentTeamMembers.clear();
        currentTeamMemberIndex = 0;
        
        if (player == null) {
            return;
        }

        // Add the player first
        currentTeamMembers.add(player);

        // Add team members in order
        if (player.getTeamObject() != null && player.getTeamObject() != Team.UNASSIGNED) {
            for (Player member : player.getTeamObject().getMembers()) {
                if (member.equals(player)) {
                    // Skip the player, already added
                    continue;
                }
                if (member.isInGame()) {
                    currentTeamMembers.add(member);
                }
            }
        }
    }

    /**
     * Advances priority to the next player in turn order.
     * <p>
     * If the current team has more members, priority moves to the next team member.
     * If all team members have had priority, priority moves to the first member of the next team.
     * </p>
     *
     * @return true if priority has completed a full circle (returned to {@code firstPlayer}),
     *         false otherwise
     */
    @Override
    public final boolean advancePriorityToNextPlayer() {
        // Try to advance within the current team first
        currentTeamMemberIndex++;
        if (currentTeamMemberIndex < currentTeamMembers.size()) {
            // More team members - give priority to the next one
            currentPlayer = currentTeamMembers.get(currentTeamMemberIndex);
            return false;  // Not done with team yet
        }

        alreadyGainedPriority.add(currentPlayer.getTeamObject());

        // If all teams return true

        Player nextPlayer = currentPlayer;
        for (int i = 0; i < game.getPlayers().size(); i++) {
            nextPlayer = game.getNextPlayerAfter(nextPlayer);
            if (!alreadyGainedPriority.contains(nextPlayer.getTeamObject())) {
                break;
            }
        }

        if (alreadyGainedPriority.contains(nextPlayer.getTeamObject())) {
            return true;
        }

        initializeTeamMembers(nextPlayer);

        return false;
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
        currentTeamMembers.clear();
        alreadyGainedPriority.clear();
        currentTeamMemberIndex = 0;
    }

    /**
     * Gets a formatted message representing the active player(s) with priority.
     * Shows the current player and the next teammate, e.g., "Player1, (Player2)".
     *
     * @return formatted string like "CurrentPlayer" or "CurrentPlayer, (NextTeammate)"
     */
    @Override
    public final String getActivePlayerMessage() {
        if (currentPlayer == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(currentPlayer);

        // Find the next in-game teammate using circular loop with modulo
        int length = currentTeamMembers.size();
        if (length > 1) {
            int currentIndex = currentTeamMembers.indexOf(currentPlayer);
            
            if (currentIndex >= 0) {
                Player nextTeammate = null;
                // Loop through team members circularly starting from next position
                for (int i = 1; i < length; i++) {
                    int nextIndex = (currentIndex + i) % length;
                    Player candidate = currentTeamMembers.get(nextIndex);
                    if (candidate.isInGame()) {
                        nextTeammate = candidate;
                        break;
                    }
                }
                
                if (nextTeammate != null) {
                    sb.append(", [").append(nextTeammate).append("]");
                }
            }
        }

        return sb.toString();
    }
}

