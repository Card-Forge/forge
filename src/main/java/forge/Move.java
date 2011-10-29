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
    private Move bestMove = null;

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

        Move[] v = move.generateMoves();
        int score = Integer.MAX_VALUE;
        for (int i = 0; i < v.length; i++) {
            score = Math.min(score, max(v[i], depth - 1, false));
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

        Move[] v = move.generateMoves();
        int score = Integer.MIN_VALUE;
        for (int i = 0; i < v.length; i++) {
            score = Math.max(score, min(v[i], depth - 1));

            if (first && bestScore < score) {
                bestScore = score;
                bestMove = v[i];
            }
        } // for
        return score;
    } // max()
}
