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

/**
 * Receives finished decision records.
 *
 * <p>This is the seam that keeps platform-specific recorders out of the shared modules: the JFR
 * recorder lives in the desktop module and registers itself here, so {@code forge-core},
 * {@code forge-game} and {@code forge-ai} never reference {@code jdk.jfr}, which does not exist on
 * Android.</p>
 *
 * <p>Implementations are called on whichever thread finished the decision and must not throw;
 * {@link PerfProbe} isolates them, but a sink that blocks will distort the very measurement it is
 * collecting.</p>
 */
public interface PerfSink {
    /** Called once per decision, after its final timer has been stopped. */
    void onDecision(DecisionRecord record);
}
