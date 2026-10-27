package forge.game.undo;

import forge.game.Game;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.event.GameEventPlayerStatsChanged;
import forge.game.event.GameEventSnapshotRestored;
import forge.game.player.Player;
import forge.util.MyRandom;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Game-thread undo manager. All game-state capture/restoration must run on the game thread;
 * synchronized protects manager metadata, not the game object graph or Recorder itself.
 */
public final class UndoManager {
    private final GameMemento.Recorder recorder = new GameMemento.Recorder();

    /** Safety cap on how many steps are kept. */
    private static final int MAX = 100;
    /**
     * Hard bound on retained memory, independent of game size: total objects held across all kept checkpoints
     * (each costs about two array slots, so roughly 2.5-5 MB at this value). Big boards keep fewer steps.
     */
    private static final long MAX_SLOTS = 300_000L;
    /** false = unlimited undos; true = limited to UNDOS_PER_PHASE each phase. */
    public static final boolean LIMITED = false;
    public static final int UNDOS_PER_PHASE = 2;

    /** OFF: nothing recorded. SINGLE_STEP: one undo, then act again. FREE: chain back until a barrier. */
    public enum Mode { OFF, SINGLE_STEP, FREE }
    public static final boolean OWN_TURN_ONLY = false;

    private record Entry(GameMemento memento, int logSize) { }

    private final Deque<Entry> stack = new ArrayDeque<>(); // newest first
    private final AtomicBoolean requested = new AtomicBoolean();
    private long randomMark = MyRandom.callCount();
    private boolean justRestored;
    private String barrierReason;
    private Mode mode = Mode.FREE;
    private Player owner;
    private int undosLeft = UNDOS_PER_PHASE;
    private long budgetPhase = -1;
    private boolean restoring;

    public synchronized Mode mode() { return mode; }

    public synchronized void setMode(Mode m) {
        if (m == null) throw new NullPointerException("mode");
        if (m == mode) return;
        if (restoring) throw new IllegalStateException("Cannot change undo mode during restoration");
        mode = m;
        stack.clear();
        justRestored = false;
        barrierReason = m == Mode.OFF ? "Undo is off" : null;
        owner = null;
        if (m == Mode.OFF) { recorder.reset(); }   // nothing is recorded any more: let go of the snapshots too
    }

    private long slots() {   // caller holds the lock; at most MAX entries
        long n = 0;
        for (Entry e : stack) { n += e.memento().objectCount(); }
        return n;
    }

    public synchronized int depth() { return Math.max(0, stack.size() - 1); }

    public synchronized boolean isOwner(Player p) { return owner == null || owner == p; }

    /** Game thread, immediately before a player is shown a priority prompt. */
    public void checkpoint(Game game, Player who) {
        synchronized (this) {
            if (restoring) return;
            requested.set(false);
            if (mode == Mode.OFF) return;
            if (owner != null && owner != who) {
                stack.clear();
                barrierReason = "Another player had priority";
                justRestored = false;
            }
            owner = who;
            if (justRestored) {
                justRestored = false;
                return;
            }
            long r = MyRandom.callCount();
            if (r != randomMark) {
                barrier("randomness used");
                randomMark = r;
            }
            var ph = game.getPhaseHandler();
            long phaseKey = ph.getTurn() * 100L + (ph.getPhase() == null ? 0 : ph.getPhase().ordinal());
            if (phaseKey > budgetPhase) {
                budgetPhase = phaseKey;
                undosLeft = UNDOS_PER_PHASE;
            }
            final int cap = mode == Mode.SINGLE_STEP ? 2 : MAX;
            while (stack.size() >= cap || (stack.size() > 1 && slots() > MAX_SLOTS)) stack.removeLast();
        }

        // IMPORTANT: capture is intentionally outside the monitor, but must remain on the game thread.
        final Entry entry;
        try {
            entry = new Entry(recorder.capture(game), game.getGameLog().size());
        } catch (RuntimeException ex) {
            // Undo is a convenience: a failed capture must never take the game thread down with it.
            System.err.println("Undo capture failed, history dropped: " + ex);
            recorder.reset();
            barrier("undo capture failed");
            return;
        }

        synchronized (this) {
            // If mode was changed while capture ran, don't retain a checkpoint that violates the new mode.
            if (mode == Mode.OFF || restoring) return;
            stack.push(entry);
            if (stack.size() >= 2) barrierReason = null;
        }
    }

    public synchronized void barrier(String why) {
        if (restoring) return;
        stack.clear();
        barrierReason = why;
    }

    public synchronized boolean canUndo(Player who) { return isOwner(who) && canUndo(); }
    public synchronized boolean canUndo() {
        return !restoring && mode != Mode.OFF && stack.size() >= 2 && (!LIMITED || undosLeft > 0);
    }
    public synchronized int undosLeft() { return LIMITED ? undosLeft : 0; }

    public synchronized String whyNot() {
        if (canUndo()) return null;
        if (restoring) return "Undo is in progress";
        if (mode == Mode.OFF) return "Undo is off";
        return stack.size() >= 2 ? "No undos left this phase" : barrierReason;
    }

    public void requestUndo() { requested.set(true); }
    public boolean consumeRequest() { return requested.getAndSet(false); }

    /**
     * Restores the previous checkpoint. Stack mutation happens only after restoration succeeds.
     * If restoring the target fails, attempts to recover the current state from the current memento.
     * Recovery is best-effort: a failure during both restores requires higher-level game recovery.
     */
    public boolean undo(Game game) {
        final Entry current;
        final Entry target;
        synchronized (this) {
            if (!canUndo()) return false;
            Iterator<Entry> it = stack.iterator();
            current = it.next();
            target = it.next();
            restoring = true;
        }

        boolean eventStarted = false;
        boolean restored = false;
        RuntimeException failure = null;
        try {
            game.fireEvent(new GameEventSnapshotRestored(true));
            eventStarted = true;
            target.memento().restore();
            game.getGameLog().truncateTo(target.logSize());
            refreshViews(game);
            restored = true;
        } catch (RuntimeException ex) {
            failure = ex;
            try {
                current.memento().restore();
                game.getGameLog().truncateTo(current.logSize());
                refreshViews(game);
            } catch (RuntimeException recoveryFailure) {
                ex.addSuppressed(recoveryFailure);
            }
        } finally {
            if (eventStarted) {
                try {
                    game.fireEvent(new GameEventSnapshotRestored(false));
                } catch (RuntimeException eventFailure) {
                    // The game may already be restored successfully; don't report the undo itself
                    // as failed solely because a listener threw while receiving the closing event.
                    System.err.println("Undo completion event failed: " + eventFailure);
                    if (failure != null) failure.addSuppressed(eventFailure);
                }
            }
            synchronized (this) {
                if (restored && stack.peek() == current) {
                    stack.pop();
                    if (LIMITED) undosLeft--;
                    if (mode == Mode.SINGLE_STEP) {
                        stack.clear();
                        stack.push(target);
                    }
                    randomMark = MyRandom.callCount();
                    justRestored = true;
                }
                restoring = false;
            }
        }

        if (failure != null) {
            System.err.println("Undo failed: " + failure);
            for (Throwable suppressed : failure.getSuppressed()) {
                System.err.println("Undo recovery/event failure: " + suppressed);
            }
            return false;
        }
        return restored;
    }

    private static void refreshViews(Game g) {
        g.getTriggerHandler().resetActiveTriggers();
        for (Player p : g.getPlayers()) {
            p.updateAllZonesForView();
            p.updateManaForView();
        }
        g.updateStackForView();
        g.updatePhaseForView();
        g.updatePlayerTurnForView();
        g.updateTurnForView();
        g.updateCombatForView();
        g.fireEvent(new GameEventCardStatsChanged(g.getCardsInGame()));
        g.fireEvent(new GameEventPlayerStatsChanged(g.getPlayers()));
    }
}
