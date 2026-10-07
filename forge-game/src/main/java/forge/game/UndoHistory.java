package forge.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import forge.game.phase.PhaseType;
import forge.game.player.Player;

/**
 * The points human players can go back to, to take back a misplay.
 * <p>
 * A point is recorded each time a human is actually asked to act - given priority, or asked to
 * declare attackers or blockers - so each point is one of their decisions, however much the game did
 * in between (auto-passes, AI turns, spells resolving). Only the last few per player are kept: this
 * is for taking back a misplay, not for replaying the game.
 * <p>
 * This is separate from {@link forge.game.zone.MagicStack#undo()}, which takes back mana abilities one
 * at a time by refunding them: that is always allowed, needs nobody's agreement and restores nothing.
 * <p>
 * Undoing is two-step because the game thread is the one that has to do it: {@link #requestUndo}
 * marks the point, the game thread notices at the next human prompt ({@link #hasPendingUndo()}) and
 * unwinds with {@link UndoRequestedException} to the main loop, which calls {@link #applyPendingUndo()}.
 */
public final class UndoHistory {

    public enum Kind {
        /** Given priority. The game resumes by giving it again. */
        PRIORITY,
        /** Asked to declare attackers. The game resumes by starting the step again. */
        DECLARE_ATTACKERS,
        /** Asked to declare blockers. The game resumes by starting the step again. */
        DECLARE_BLOCKERS;

        public boolean resumesAtStepStart() {
            return this != PRIORITY;
        }
    }

    public static final class Point {
        private final long id;
        private final Set<Player> players;
        private final Kind kind;
        private final int turn;
        private final PhaseType phase;
        private final GameCheckpoint checkpoint;
        private volatile String outcome;

        private Point(long id, Set<Player> players, Kind kind, int turn, PhaseType phase, GameCheckpoint checkpoint) {
            this.id = id;
            this.players = players;
            this.kind = kind;
            this.turn = turn;
            this.phase = phase;
            this.checkpoint = checkpoint;
        }

        public long getId() { return id; }
        public Kind getKind() { return kind; }
        public int getTurn() { return turn; }
        public PhaseType getPhase() { return phase; }
        public boolean isFor(Player p) { return players.contains(p); }
        public GameCheckpoint getCheckpoint() { return checkpoint; }

        /**
         * What the player did when asked here, for listing the points. Whoever recorded the point sets it
         * once the player answered; until then the point is the decision being made now.
         */
        public String getOutcome() { return outcome; }
        public void setOutcome(String outcome) { this.outcome = outcome; }

        @Override
        public String toString() {
            return "turn " + turn + ", " + (phase == null ? "?" : phase.nameForUi) + " (" + kind + ")"
                    + (outcome == null ? "" : ": " + outcome);
        }
    }

    /** The decision being made now and the ones before it that can be gone back to, per player. */
    public static final int POINTS_PER_PLAYER = 4;

    private final Game game;
    // oldest first
    private final LinkedList<Point> points = new LinkedList<>();
    private boolean enabled = false;
    private long nextId = 0;
    private Point staged;
    private volatile Point pending;
    private volatile Player pendingBy;

    public UndoHistory(Game game) {
        this.game = game;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clear();
        }
    }

    /**
     * Records that this player is about to be given priority. Game thread only.
     * @return the point, to note what they did with it; null if nothing was recorded
     */
    public synchronized Point recordPriority(Player player) {
        if (!canRecord()) {
            return null;
        }
        staged = null;
        // The game loop may have stashed this very state already, for cancelling what the player starts
        final GameCheckpoint stashed = game.getStashedState();
        return add(new Point(nextId++, ImmutableSet.of(player), Kind.PRIORITY,
                game.getPhaseHandler().getTurn(), game.getPhaseHandler().getPhase(),
                stashed != null ? stashed : GameCheckpoint.capture(game)));
    }

    /**
     * Takes a checkpoint at the start of a turn-based step a human may be asked to act in. It only
     * becomes a point for a player once they actually are, see {@link #commitStepStart}; a step they
     * were never asked anything in isn't a decision to take back. Game thread only.
     */
    public synchronized void stageStepStart(Kind kind) {
        staged = null;
        if (!canRecord() || !hasHumanPlayer()) {
            return;
        }
        staged = new Point(-1, ImmutableSet.of(), kind,
                game.getPhaseHandler().getTurn(), game.getPhaseHandler().getPhase(), GameCheckpoint.capture(game));
    }

    private boolean hasHumanPlayer() {
        for (final Player p : game.getPlayers()) {
            if (!p.getController().isAI()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The player is being asked to act in the step staged by {@link #stageStepStart}. Game thread only.
     * @return the point, to note what they did with it; null if nothing was recorded
     */
    public synchronized Point commitStepStart(Player player, Kind kind) {
        if (staged == null || staged.kind != kind || !canRecord()) {
            return null;
        }
        return add(new Point(nextId++, ImmutableSet.of(player), kind, staged.turn, staged.phase, staged.checkpoint));
    }

    private boolean canRecord() {
        // Nothing new is recorded while an undo waits to be carried out: it could crowd out its target.
        return enabled && pending == null && !game.isGameOver();
    }

    private Point add(Point point) {
        points.addLast(point);
        // Keep a point while it is among the newest few of some player it belongs to.
        final Iterator<Point> it = points.iterator();
        while (it.hasNext()) {
            final Point p = it.next();
            boolean needed = false;
            for (final Player pl : p.players) {
                if (countFrom(pl, p) <= POINTS_PER_PLAYER) {
                    needed = true;
                    break;
                }
            }
            if (!needed) {
                it.remove();
            }
        }
        return point;
    }

    /** Points for this player from the given one on. */
    private int countFrom(Player player, Point from) {
        int n = 0;
        for (final Point p : points) {
            if (p.id >= from.id && p.isFor(player)) {
                n++;
            }
        }
        return n;
    }

    /**
     * The earlier decisions this player can go back to, most recent first. A decision still being made
     * isn't one of them: going back to it would change nothing.
     */
    public synchronized List<Point> getUndoTargets(Player player) {
        final List<Point> result = new ArrayList<>();
        if (!enabled) {
            return result;
        }
        final Iterator<Point> it = points.descendingIterator();
        while (it.hasNext()) {
            final Point p = it.next();
            if (p.isFor(player) && p.outcome != null) {
                result.add(p);
            }
        }
        return result;
    }

    public boolean canUndo(Player player) {
        return !getUndoTargets(player).isEmpty();
    }

    public synchronized Point findPoint(long id) {
        for (final Point p : points) {
            if (p.id == id) {
                return p;
            }
        }
        return null;
    }

    /** Asks for the game to go back to one of this player's earlier decisions. */
    public synchronized boolean requestUndo(Player player, Point target) {
        if (target == null || !getUndoTargets(player).contains(target)) {
            return false;
        }
        pending = target;
        pendingBy = player;
        return true;
    }

    /** Asks for the game to go back to this player's most recent earlier decision. */
    public synchronized boolean requestUndo(Player player) {
        final List<Point> targets = getUndoTargets(player);
        return !targets.isEmpty() && requestUndo(player, targets.get(0));
    }

    public boolean hasPendingUndo() {
        return pending != null;
    }

    /** Who asked for the pending undo. */
    public Player getPendingUndoBy() {
        return pendingBy;
    }

    public synchronized void cancelPendingUndo() {
        pending = null;
        pendingBy = null;
    }

    /**
     * Puts the game back to the requested point, forgetting every point taken after it - and the point
     * itself, which is recorded again as the player is asked once more. Game thread only.
     *
     * @return the point restored, or null if nothing was pending
     */
    public synchronized Point applyPendingUndo() {
        final Point target = pending;
        pending = null;
        pendingBy = null;
        if (target == null || !points.contains(target)) {
            return null;
        }
        target.checkpoint.restore();
        points.removeIf(p -> p.id >= target.id);
        staged = null;
        return target;
    }

    public synchronized void clear() {
        points.clear();
        staged = null;
        pending = null;
        pendingBy = null;
    }

    /** Every point held, oldest first. For tests and diagnostics. */
    public synchronized List<Point> getPoints() {
        return new ArrayList<>(points);
    }
}
