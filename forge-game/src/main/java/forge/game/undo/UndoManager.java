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
    /** Set when undo is switched off: the recorder is emptied by the game thread at the next checkpoint. */
    private boolean resetRecorder;
    /** Objects captured by the last snapshot, used only to budget standalone full rollback snapshots. */
    private long lastObjects;
    /** Decaying high-water estimate for standalone AI rollback snapshots. */
    private long lastStandaloneCaptureBytes;
    /** Consecutive denied checkpoints before releasing a stale baseline as well. */
    private int lowMemorySkips;
    /**
     * Heap the undo history may pin in total: the recorder's copy of the current state plus every reverse delta, as
     * counted by the memento classes. About a seventh of the maximum heap (at most 1 GB), so a phone never fills up
     * with undo history; the oldest steps are dropped first.
     */
    private static final long MAX_RETAINED_BYTES = Math.min(1L << 30, Runtime.getRuntime().maxMemory() / 7);

    private static final long MIN_INCREMENTAL_ESTIMATE = 256L << 10;
    private static final long MIN_HEAP_RESERVE = 512L << 10;
    private static final long MAX_HEAP_RESERVE = 16L << 20;
    private static final int MAX_BASELINE_REUSE_SKIPS = 3;

    /**
     * Keep a small heap-scaled reserve and budget the next allocation from recent capture estimates.
     * Standalone AI rollback snapshots are full-graph copies, unlike the incremental human-history checkpoints.
     */
    private boolean heapAllows(boolean standalone) {
        final Runtime rt = Runtime.getRuntime();
        final long headroom = rt.maxMemory() - (rt.totalMemory() - rt.freeMemory());
        final long full = Math.max(recorder.lastFullCaptureBytes(), lastStandaloneCaptureBytes);
        final long estimate;
        if (standalone) {
            estimate = full > 0 ? full : Math.max(1L << 20, lastObjects * 220L);
        } else if (recorder.liveBytes() > 0) {
            if (recorder.needsBaseline()) {
                // A soft low-memory trim detached the chain. This capture refreshes the baseline
                // without recording a reverse delta, so use its separately tracked estimate.
                final long rebase = recorder.lastRebaseCaptureBytes();
                final long fallback = full > 0 ? Math.min(1L << 20, full / 64L) : MIN_INCREMENTAL_ESTIMATE;
                estimate = Math.max(MIN_INCREMENTAL_ESTIMATE, rebase > 0 ? rebase : fallback);
            } else {
                // Seed the first delta from a fraction of a known full capture, but never treat an
                // unknown incremental cost as zero. The floor still permits small edits on phones.
                final long firstDelta = full > 0 ? Math.min(1L << 20, full / 64L) : MIN_INCREMENTAL_ESTIMATE;
                estimate = Math.max(MIN_INCREMENTAL_ESTIMATE, Math.max(firstDelta, recorder.lastCaptureBytes()));
            }
        } else {
            // After a hard reset, retain the last known full-capture cost. On a first-ever capture
            // use a conservative floor; no estimate can guarantee safety for an unknown graph.
            estimate = full > 0 ? full : Math.max(1L << 20, lastObjects * 220L);
        }
        // These counters estimate retained structures and common temporary arrays, not exact JVM
        // peak allocation. Add a margin for overlapping old/new arrays and hash-table growth.
        final long peakEstimate = estimate + Math.max(128L << 10, estimate / 2L);
        // At 512 MiB this is 1 MiB, rather than the previous 8 MiB. This leaves room for modest
        // incremental checkpoints when a phone has only a few MiB of heap headroom.
        final long reserve = Math.max(MIN_HEAP_RESERVE,
                Math.min(MAX_HEAP_RESERVE, rt.maxMemory() / 512L));
        return headroom > peakEstimate + reserve;
    }

    /** Game thread. Frees the undo history and the recorder's registry. */
    private synchronized void dropHistory(String why) {
        stack.clear();
        barrierReason = why;
        justRestored = false;
        recorder.reset();
        lowMemorySkips = 0;
    }

    /** Drops obsolete undo deltas but keeps the latest baseline for a cheaper refresh. */
    private synchronized void discardHistoryKeepingBaseline(String why) {
        stack.clear();
        barrierReason = why;
        justRestored = false;
        recorder.detach();
        if (recorder.liveBytes() > 0) {
            if (++lowMemorySkips >= MAX_BASELINE_REUSE_SKIPS) {
                // A repeatedly stale baseline can pin objects that have since left the game.
                // Release it eventually rather than retaining an obsolete graph indefinitely.
                recorder.reset();
                lowMemorySkips = 0;
            }
        } else {
            lowMemorySkips = 0;
        }
    }

    /** Estimated heap pinned by the history: the recorder's current state plus the delta of every step. */
    private synchronized long retainedBytes() {
        long n = recorder.liveBytes();
        for (Entry e : stack) { n += e.memento().retainedBytes(); }
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
        if (m == Mode.OFF) { resetRecorder = true; }   // the game thread frees the registry at its next checkpoint
    }
    public synchronized int depth() { return Math.max(0, stack.size() - 1); }

    /** True if {@code p} is the player whose actions are currently recorded (or nobody is recorded yet). */
    public synchronized boolean isOwner(Player p) { return owner == null || owner == p; }

    /** Game thread, right before {@code who} (a human, local or remote) is shown a priority prompt. */
    public void checkpoint(Game game, Player who) {
        synchronized (this) {
            requested = false;
            if (resetRecorder) { resetRecorder = false; recorder.reset(); }
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
                    || (stack.size() > 1 && retainedBytes() + recorder.lastDeltaBytes() > MAX_RETAINED_BYTES)) {
                stack.removeLast();
            }
            // The new memento must follow the newest one on the stack. If it does not (the history was cleared, or
            // something is out of step), start a fresh chain instead of attaching a delta to a memento nobody can use.
            if (!stack.isEmpty() && !recorder.isLatest(stack.peek().memento())) {
                stack.clear();
                barrierReason = "Undo history was reset";
            }
            if (stack.isEmpty()) { recorder.detach(); }
        }

        if (!heapAllows(false)) {
            // Discard reverse history first but preserve the current baseline. Recheck with the
            // cheaper baseline-only estimate; if it fits, refresh the baseline now so the next
            // action can already be undoable.
            discardHistoryKeepingBaseline("Low memory");
            if (!heapAllows(false)) { return; }
        }
        final Entry entry;
        try {
            final GameMemento m = recorder.capture(game);   // slow part, no lock held
            lastObjects = m.objectCount();
            entry = new Entry(m, game.getGameLog().size());
            lowMemorySkips = 0;
        } catch (final RuntimeException | OutOfMemoryError ex) {
            dropHistory("Low memory");
            return;
        }

        synchronized (this) {
            stack.push(entry);
            //System.out.println("[undo] objects=" + entry.memento().objectCount() + " delta=" + recorder.lastDeltaEntries() + " retained=" + retainedBytes());
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
     * the snapshot could not be taken (the caller then has no safety net and must not count on one). The snapshot is
     * self-contained: it shares nothing with the human history and keeps nothing alive once the mark is dropped,
     * which matters on phones. Take it only for actions that can fail, it reads the whole game.
     */
    public Mark markForRollback(final Game game) {
        if (mode() == Mode.OFF) {
            return null;
        }
        if (!heapAllows(true)) {
            dropHistory("Low memory");   // the human history is the big freeable chunk
            return null;
        }
        try {
            final GameMemento m = GameMemento.capture(game);
            lastObjects = m.objectCount();
            final long decayed = lastStandaloneCaptureBytes - lastStandaloneCaptureBytes / 8L;
            lastStandaloneCaptureBytes = Math.max(m.captureBytes(), decayed);
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
            final boolean restored = mark.memento().restore();
            game.getGameLog().truncateTo(mark.logSize());
            clean = restored && mark.memento().problems.isEmpty();
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
        if (!target.memento().restore()) {            // refused, nothing was written: the history is unusable
            synchronized (this) {
                stack.clear();
                barrierReason = "Undo history was lost";
                justRestored = false;
            }
            recorder.reset();
            game.fireEvent(new GameEventSnapshotRestored(false));
            return false;
        }
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
