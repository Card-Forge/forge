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
    /** Tunable: true = after an undo the player can't chain further back until they act again. */
    public static final boolean SINGLE_STEP = false;
    public static final boolean OWN_TURN_ONLY = false;
    private record Entry(GameMemento memento, int logSize) {}

    private final Deque<Entry> stack = new ArrayDeque<>();   // newest first
    private long randomMark = MyRandom.callCount();
    private boolean justRestored;
    private volatile boolean requested;
    private String barrierReason;
    /** The player whose priority prompts the stack was recorded for. Only they may undo. */
    private Player owner;
    private int undosLeft = UNDOS_PER_PHASE;
    private long budgetPhase = -1;
    public synchronized int depth() { return Math.max(0, stack.size() - 1); }

    /** True if {@code p} is the player whose actions are currently recorded (or nobody is recorded yet). */
    public synchronized boolean isOwner(Player p) { return owner == null || owner == p; }

    /** Game thread, right before {@code who} (a human, local or remote) is shown a priority prompt. */
    public void checkpoint(Game game, Player who) {
        synchronized (this) {
            requested = false;
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
            final int cap = SINGLE_STEP ? 2 : MAX;
            while (stack.size() >= cap) stack.removeLast();
        }

        final Entry entry = new Entry(recorder.capture(game), game.getGameLog().size());   // slow part, no lock held

        synchronized (this) {
            stack.push(entry);
            //System.out.println("[undo] objects=" + entry.memento().objectCount() + " new=" + recorder.lastAllocated + " reused=" + recorder.lastReused);
            if (stack.size() >= 2) barrierReason = null;
        }
    }

    /** Anything that reveals hidden info or makes the opponent act. */
    public synchronized void barrier(String why) { stack.clear(); barrierReason = why; }

    public synchronized boolean canUndo(Player who) { return isOwner(who) && canUndo(); }
    public synchronized boolean canUndo() { return stack.size() >= 2 && (!LIMITED || undosLeft > 0); }
    public synchronized int undosLeft() { return LIMITED ? undosLeft : 0; }   // 0 = no count shown on the button
    public synchronized String whyNot() {
        if (canUndo()) return null;
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
            if (SINGLE_STEP) {          // forget everything older than the state we are restoring
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