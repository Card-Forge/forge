package forge;

/**
 * <p>HandSizeOp class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class HandSizeOp {
    public String Mode;
    public int hsTimeStamp;
    public int Amount;

    /**
     * <p>Constructor for HandSizeOp.</p>
     *
     * @param m a {@link java.lang.String} object.
     * @param a a int.
     * @param ts a int.
     */
    public HandSizeOp(final String m, final int a, final int ts) {
        Mode = m;
        Amount = a;
        hsTimeStamp = ts;
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String toString() {
        return "Mode(" + Mode + ") Amount(" + Amount + ") Timestamp(" + hsTimeStamp + ")";
    }
}
