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
package forge;

/**
 * <p>
 * Abstract Move class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Move {
    /**
     * <p>
     * generateMoves.
     * </p>
     * 
     * @return an array of {@link forge.Move} objects.
     */
    public abstract Move[] generateMoves();

    /**
     * <p>
     * getScore.
     * </p>
     * 
     * @return a int.
     */
    public abstract int getScore();

    /** The best move. */
    // private Move bestMove = null;

    /** The best score. */
    private int bestScore = Integer.MIN_VALUE;

    /**
     * <p>
     * min.
     * </p>
     * 
     * @param move
     *            a {@link forge.Move} object.
     * @param depth
     *            a int.
     * @return a int.
     */
    public final int min(final Move move, final int depth) {
        if (depth == 0) {
            return move.getScore();
        }

        final Move[] v = move.generateMoves();
        int score = Integer.MAX_VALUE;
        for (final Move element : v) {
            score = Math.min(score, this.max(element, depth - 1, false));
        }
        return score;
    }

    /**
     * <p>
     * max.
     * </p>
     * 
     * @param move
     *            a {@link forge.Move} object.
     * @param depth
     *            a int.
     * @param first
     *            a boolean.
     * @return a int.
     */
    public final int max(final Move move, final int depth, final boolean first) {
        if (depth == 0) {
            return move.getScore();
        }

        final Move[] v = move.generateMoves();
        int score = Integer.MIN_VALUE;
        for (final Move element : v) {
            score = Math.max(score, this.min(element, depth - 1));

            if (first && (this.bestScore < score)) {
                this.bestScore = score;
                // bestMove = v[i];
            }
        } // for
        return score;
    } // max()
}
