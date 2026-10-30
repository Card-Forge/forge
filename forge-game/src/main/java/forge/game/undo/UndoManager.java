package forge.game.undo;

import forge.game.Game;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.event.GameEventPlayerStatsChanged;
import forge.game.event.GameEventSnapshotRestored;
import forge.game.player.Player;
import forge.util.MyRandom;

import java.util.ArrayDeque;
import java.util.Deque;

public final class UndoManager {
    private final GameMemento.Recorder recorder = new GameMemento.Recorder();
    /** Safety cap on how many steps are kept (memory); effectively unlimited for normal play. */
    private static final int MAX = 100;
    /** Tunable: false = unlimited undos; true = limited to UNDOS_PER_PHASE each phase. */
    public static final boolean LIMITED = false;
    public static final int UNDOS_PER_PHASE = 2;
    /** OFF: nothing is recorded (no cost). SINGLE_STEP: one undo, then act again. FREE: chain back until a barrier. */
    public enum Mode { OFF, SINGLE_STEP, FREE }
    public static final boolean OWN_TURN_ONLY = false;
    private record Entry(GameMemento memento, int logSize) {}

    private final Deque<Entry> stack = new ArrayDeque<>();   // newest first
    private long randomMark = MyRandom.callCount();
    private boolean justRestored;
    private volatile boolean requested;
    private String barrierReason;
    private Mode mode = Mode.FREE;
    /** The player whose priority prompts the stack was recorded for. Only they may undo. */
    private Player owner;
    private int undosLeft = UNDOS_PER_PHASE;
    private long budgetPhase = -1;
    /** Objects captured by the last snapshot: predicts what the next one will cost. */
    private long lastObjects;
    /** Rough peak bytes per captured object while a capture runs (an estimate; tune from real logs). */
    private static final long BYTES_PER_OBJECT = 220;
    /** Objects the human undo history may hold in total (a few tens of MB of arrays). */
    private static final long MAX_RETAINED_OBJECTS = 6_000_000L;

    /** False when the heap is too full to take another snapshot safely. */
    private boolean heapAllows() {
        if (lastObjects == 0) { return true; }
        final Runtime rt = Runtime.getRuntime();
        final long free = rt.maxMemory() - (rt.totalMemory() - rt.freeMemory());
        return free > lastObjects * BYTES_PER_OBJECT;
    }

    /** Game thread. Frees the undo history and the recorder's registry. */
    private synchronized void dropHistory(String why) {
        stack.clear();
        barrierReason = why;
        justRestored = false;
        recorder.reset();
    }

    private synchronized long retainedObjects() {
        long n = 0;
        for (Entry e : stack) { n += e.memento().objectCount(); }
        return n;
    }

    public synchronized Mode mode() { return mode; }

    /** Game thread; safe to call every prompt. Turning undo off drops the history. */
    public synchronized void setMode(Mode m) {
        if (m == mode) { return; }
        mode = m;
        stack.clear();
        justRestored = false;
        barrierReason = m == Mode.OFF ? "Undo is off" : null;
    }
    public synchronized int depth() { return Math.max(0, stack.size() - 1); }

    /** True if {@code p} is the player whose actions are currently recorded (or nobody is recorded yet). */
    public synchronized boolean isOwner(Player p) { return owner == null || owner == p; }

    /** Game thread, right before {@code who} (a human, local or remote) is shown a priority prompt. */
    public void checkpoint(Game game, Player who) {
        synchronized (this) {
            requested = false;
            if (mode == Mode.OFF) { return; }
            if (owner != null && owner != who) {      // another human is now acting: their history is not ours to rewind
                stack.clear();
                barrierReason = "Another player had priority";
                justRestored = false;
            }
            owner = who;
            if (justRestored) { justRestored = false; return; }
            long r = MyRandom.callCount();
            if (r != randomMark) { barrier("randomness used"); randomMark = r; }
            var ph = game.getPhaseHandler();
            long phaseKey = ph.getTurn() * 100L + (ph.getPhase() == null ? 0 : ph.getPhase().ordinal());
            if (phaseKey > budgetPhase) { budgetPhase = phaseKey; undosLeft = UNDOS_PER_PHASE; }
            final int cap = mode == Mode.SINGLE_STEP ? 2 : MAX;
            while (stack.size() >= cap
                    || (stack.size() > 1 && retainedObjects() + lastObjects > MAX_RETAINED_OBJECTS)) {
                stack.removeLast();
            }
        }

        if (!heapAllows()) {
            dropHistory("Low memory");
            if (!heapAllows()) { return; }
        }
        final Entry entry;
        try {
            final GameMemento m = recorder.capture(game);   // slow part, no lock held
            lastObjects = m.objectCount();
            entry = new Entry(m, game.getGameLog().size());
        } catch (final RuntimeException | OutOfMemoryError ex) {
            dropHistory("Low memory");
            return;
        }

        synchronized (this) {
            stack.push(entry);
            //System.out.println("[undo] objects=" + entry.memento().objectCount() + " new=" + recorder.lastAllocated + " reused=" + recorder.lastReused);
            if (stack.size() >= 2) barrierReason = null;
        }
    }

    /**
     * A snapshot taken just before an action of a player whose actions are not on the undo stack (the AI), so that the
     * action can be taken back if it turns out to be illegal. It is not part of the human undo history.
     */
    public record Mark(GameMemento memento, int logSize) {}

    /**
     * Game thread. Takes a snapshot to roll one action back to with {@link #rollbackTo}, or null when undo is off or
     * the snapshot could not be taken (the caller then has no safety net and must not count on one). A fresh recorder
     * is used on purpose: it shares nothing with the human history and keeps nothing alive once the mark is dropped,
     * which matters on phones. Take it only for actions that can fail, it reads the whole game.
     */
    public Mark markForRollback(final Game game) {
        if (mode() == Mode.OFF) {
            return null;
        }
        if (!heapAllows()) {
            dropHistory("Low memory");   // the human history is the big freeable chunk
            if (!heapAllows()) {
                return null;
            }
        }
        try {
            final GameMemento m = new GameMemento.Recorder().capture(game);
            lastObjects = m.objectCount();
            return new Mark(m, game.getGameLog().size());
        } catch (final RuntimeException | OutOfMemoryError ex) {
            dropHistory("Low memory");
            System.err.println("Could not take a rollback snapshot: " + ex);
            return null;
        }
    }

    /**
     * Game thread. Puts the game back as it was at {@code mark}. Returns false when nothing was restored or the
     * restore reported problems (the game may then be only partly restored).
     */
    public boolean rollbackTo(final Game game, final Mark mark) {
        if (mark == null) {
            return false;
        }
        boolean clean;
        try {
            game.fireEvent(new GameEventSnapshotRestored(true));
            mark.memento().restore();
            game.getGameLog().truncateTo(mark.logSize());
            clean = mark.memento().problems.isEmpty();
            if (!clean) {
                System.err.println("Rollback problems: " + mark.memento().problems);
            }
            refreshViews(game);
            game.fireEvent(new GameEventSnapshotRestored(false));
        } catch (final RuntimeException ex) {
            System.err.println("Rollback failed: " + ex);
            return false;
        }
        synchronized (this) {
            randomMark = MyRandom.callCount();
            stack.clear();                       // whatever a human could undo to is older than this now
            barrierReason = "An action was taken back";
        }
        return clean;
    }

    /** Anything that reveals hidden info or makes the opponent act. */
    public synchronized void barrier(String why) { stack.clear(); barrierReason = why; }

    public synchronized boolean canUndo(Player who) { return isOwner(who) && canUndo(); }
    public synchronized boolean canUndo() { return mode != Mode.OFF && stack.size() >= 2 && (!LIMITED || undosLeft > 0); }
    public synchronized int undosLeft() { return LIMITED ? undosLeft : 0; }   // 0 = no count shown on the button
    public synchronized String whyNot() {
        if (canUndo()) return null;
        if (mode == Mode.OFF) return "Undo is off";
        return stack.size() >= 2 ? "No undos left this phase" : barrierReason;
    }
    public void    requestUndo()  { requested = true; }
    public boolean consumeRequest(){ boolean r = requested; requested = false; return r; }

    /** Game thread. Drops the current prompt's checkpoint, restores the previous one. */
    public boolean undo(Game game) {
        Entry target;
        synchronized (this) {
            if (!canUndo()) return false;
            stack.pop();
            if (LIMITED) undosLeft--;
            target = stack.peek();
            if (mode == Mode.SINGLE_STEP) {          // forget everything older than the state we are restoring
                stack.clear();
                stack.push(target);
            }
        }
        game.fireEvent(new GameEventSnapshotRestored(true));
        target.memento().restore();
        game.getGameLog().truncateTo(target.logSize());   // drop log lines written after the checkpoint
        if (!target.memento().problems.isEmpty()) System.err.println("Undo problems: " + target.memento().problems);
        refreshViews(game);
        game.fireEvent(new GameEventSnapshotRestored(false));
        randomMark = MyRandom.callCount();
        justRestored = true;
        return true;
    }

    private static void refreshViews(Game g) {
        g.getTriggerHandler().resetActiveTriggers();
        for (Player p : g.getPlayers()) { p.updateAllZonesForView(); p.updateManaForView(); }
        g.updateStackForView(); g.updatePhaseForView(); g.updatePlayerTurnForView();
        g.updateTurnForView(); g.updateCombatForView();
        g.fireEvent(new GameEventCardStatsChanged(g.getCardsInGame()));
        g.fireEvent(new GameEventPlayerStatsChanged(g.getPlayers()));
    }
}