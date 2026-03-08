package forge.gamemodes.net;

import forge.game.GameView;
import forge.game.player.PlayerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shared checksum computation for delta sync validation.
 * Used by both server (DeltaSyncManager) and client (NetworkGuiGame)
 * to ensure identical checksums.
 */
public final class NetworkChecksumUtil {

    private NetworkChecksumUtil() {} // Utility class

    /**
     * Compute a state checksum from turn, phase, and player state.
     * Both server and client must call this method to ensure compatibility.
     *
     * @param turn the current turn number
     * @param phaseOrdinal the phase ordinal, or -1 if phase is null
     * @param players the players to include (will be sorted by ID internally)
     * @return checksum value
     */
    public static int computeStateChecksum(int turn, int phaseOrdinal, Iterable<PlayerView> players) {
        int hash = 17;
        hash = 31 * hash + turn;
        if (phaseOrdinal >= 0) {
            hash = 31 * hash + phaseOrdinal;
        }
        if (players != null) {
            for (PlayerView player : getSortedPlayers(players)) {
                hash = 31 * hash + player.getId();
                hash = 31 * hash + player.getLife();
            }
        }
        return hash;
    }

    /**
     * Get players sorted by ID for consistent iteration order.
     * Required for deterministic checksum computation across server and client.
     *
     * @param gameView the game view
     * @return sorted list, or empty list if no players
     */
    public static List<PlayerView> getSortedPlayers(GameView gameView) {
        if (gameView == null || gameView.getPlayers() == null) {
            return new ArrayList<>();
        }
        return getSortedPlayers(gameView.getPlayers());
    }

    /**
     * Get players sorted by ID for consistent iteration order.
     *
     * @param players the players iterable
     * @return sorted list
     */
    public static List<PlayerView> getSortedPlayers(Iterable<PlayerView> players) {
        List<PlayerView> sorted = new ArrayList<>();
        for (PlayerView p : players) {
            sorted.add(p);
        }
        sorted.sort(Comparator.comparingInt(PlayerView::getId));
        return sorted;
    }
}
