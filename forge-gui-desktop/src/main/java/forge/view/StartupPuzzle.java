package forge.view;

import java.io.File;

import javax.swing.SwingUtilities;

import forge.gamemodes.puzzle.Puzzle;
import forge.gamemodes.puzzle.PuzzleIO;
import forge.screens.home.puzzle.CSubmenuPuzzleSolve;

/**
 * Holds the puzzle file passed as {@code puzzle <file>} on the command line and starts it once the
 * home screen is up, so a test board can be launched without going through Puzzle Mode.
 */
public final class StartupPuzzle {
    private static File pending;

    private StartupPuzzle() {
    }

    /**
     * Records the puzzle to start after startup. Only checks that the file exists: parsing needs the
     * card database, which isn't loaded yet.
     *
     * @return false (after printing why) if there is no such file
     */
    public static boolean setPending(final String path) {
        final File file = new File(path).getAbsoluteFile();
        if (!file.isFile()) {
            System.out.println("Puzzle file not found: " + file);
            return false;
        }
        pending = file;
        return true;
    }

    /** Starts the pending puzzle, if any. Called on the EDT once the main window is showing. */
    public static void startIfPending() {
        if (pending == null) {
            return;
        }
        final File file = pending;
        pending = null;
        // Let initialization finish unwinding before the match takes over the screen.
        SwingUtilities.invokeLater(() -> {
            final Puzzle puzzle = PuzzleIO.loadPuzzle(file);
            CSubmenuPuzzleSolve.startPuzzle(puzzle, false);
        });
    }
}
