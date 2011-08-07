package forge;

/**
 * <p>HandSizeOp class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class HandSizeOp {
    public String Mode;
    public int hsTimeStamp;
    public int Amount;

    /**
     * <p>Constructor for HandSizeOp.</p>
     *
     * @param M a {@link java.lang.String} object.
     * @param A a int.
     * @param TS a int.
     */
    public HandSizeOp(String M, int A, int TS) {
        Mode = M;
        Amount = A;
        hsTimeStamp = TS;
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString() {
        return "Mode(" + Mode + ") Amount(" + Amount + ") Timestamp(" + hsTimeStamp + ")";
    }
}
