package forge;

/**
 * <p>
 * HandSizeOp class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class HandSizeOp {

    /** The Mode. */
    private String mode;

    /** The hs time stamp. */
    private int hsTimeStamp;

    /** The Amount. */
    private int amount;

    /**
     * <p>
     * Constructor for HandSizeOp.
     * </p>
     * 
     * @param m
     *            a {@link java.lang.String} object.
     * @param a
     *            a int.
     * @param ts
     *            a int.
     */
    public HandSizeOp(final String m, final int a, final int ts) {
        setMode(m);
        setAmount(a);
        setHsTimeStamp(ts);
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String toString() {
        return "Mode(" + getMode() + ") Amount(" + getAmount() + ") Timestamp(" + getHsTimeStamp() + ")";
    }

    /**
     * @return the amount
     */
    public int getAmount() {
        return amount;
    }

    /**
     * @param amount the amount to set
     */
    public void setAmount(int amount) {
        this.amount = amount; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(String mode) {
        this.mode = mode; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the hsTimeStamp
     */
    public int getHsTimeStamp() {
        return hsTimeStamp;
    }

    /**
     * @param hsTimeStamp the hsTimeStamp to set
     */
    public void setHsTimeStamp(int hsTimeStamp) {
        this.hsTimeStamp = hsTimeStamp; // TODO: Add 0 to parameter's name.
    }
}
