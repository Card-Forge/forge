package forge.headless;

import forge.game.Game;
import forge.game.GameLogEntry;
import forge.gui.GuiBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Extended HeadlessGuiBase for TUI mode that provides game log access.
 */
public class TUIGuiBase extends HeadlessGuiBase {

    private static volatile Game currentGame = null;
    private static volatile int lastLogIndex = 0;
    private static volatile boolean tuiModeActive = false;

    /**
     * Install the TUI GUI base as the global interface.
     */
    public static void install() {
        GuiBase.setInterface(new TUIGuiBase());
        tuiModeActive = true;
    }

    /**
     * Check if TUI mode is currently active.
     * In TUI mode, all player actions (including draws) are logged since
     * both players share the same console view.
     */
    public static boolean isTUIMode() {
        return tuiModeActive;
    }

    /**
     * Set the current game being played in TUI mode.
     */
    public static void setCurrentGame(Game game) {
        currentGame = game;
        lastLogIndex = 0;
    }

    /**
     * Get new log entries since the last check.
     * Returns entries with the +++ prefix for easy identification.
     */
    public static List<String> getNewLogEntries() {
        List<String> newEntries = new ArrayList<>();

        if (currentGame == null) {
            return newEntries;
        }

        List<GameLogEntry> allEntries = currentGame.getGameLog().getLogEntries(null);

        // The game log is stored in reverse chronological order (newest first)
        // So we need to track from the end and work backwards
        int totalEntries = allEntries.size();

        // Calculate how many new entries there are
        int newEntriesCount = totalEntries - lastLogIndex;

        if (newEntriesCount > 0) {
            // New entries are at indices [0, newEntriesCount)
            // But we want to print them in chronological order (oldest to newest)
            for (int i = newEntriesCount - 1; i >= 0; i--) {
                GameLogEntry entry = allEntries.get(i);
                // Prefix with +++ for TUI log entries
                newEntries.add("+++ " + entry.toString());
            }
        }

        lastLogIndex = totalEntries;
        return newEntries;
    }

    /**
     * Print any new log entries to stdout.
     */
    public static void printNewLogEntries() {
        for (String entry : getNewLogEntries()) {
            System.out.println(entry);
        }
    }
}
