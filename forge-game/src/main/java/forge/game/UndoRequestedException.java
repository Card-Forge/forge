package forge.game;

/**
 * Thrown on the game thread, from wherever it is waiting for a human to act, once an undo has been
 * agreed. It unwinds to {@link forge.game.phase.PhaseHandler#mainGameLoop()}, which restores the
 * game from {@link UndoHistory}. Everything the unwinding interrupts is rewritten by that restore.
 */
public class UndoRequestedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UndoRequestedException() {
        super("Undo requested", null, false, false);
    }
}
