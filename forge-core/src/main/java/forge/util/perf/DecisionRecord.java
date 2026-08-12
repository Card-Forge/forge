/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.util.perf;

/** Everything measured about one AI decision. */
public final class DecisionRecord extends PerfAccumulator {
    private final long id;
    private final DecisionKind kind;
    private final String player;
    private final int turn;
    private final String phase;
    private final long startNanos;

    private volatile long endNanos = -1L;

    DecisionRecord(final long id, final DecisionKind kind, final String player, final int turn, final String phase,
            final long startNanos) {
        this.id = id;
        this.kind = kind;
        this.player = player == null ? "" : player;
        this.turn = turn;
        this.phase = phase == null ? "" : phase;
        this.startNanos = startNanos;
    }

    /** Monotonically increasing within a JVM; usable as a stable ordinal in traces. */
    public long getId() {
        return id;
    }

    public DecisionKind getKind() {
        return kind;
    }

    public String getPlayer() {
        return player;
    }

    public int getTurn() {
        return turn;
    }

    public String getPhase() {
        return phase;
    }

    /** Elapsed wall time of the whole decision, or -1 while it is still running. */
    public long getDurationNanos() {
        final long end = endNanos;
        return end < 0L ? -1L : end - startNanos;
    }

    void finish(final long nanos) {
        endNanos = nanos;
    }
}
