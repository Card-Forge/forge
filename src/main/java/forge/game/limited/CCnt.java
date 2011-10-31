package forge.game.limited;

/**
 * <p>
 * CCnt class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
class CCnt {

    /** The Color. */
    private String color;

    /** The Count. */
    private int count;

    /**
     * <p>
     * Constructor for CCnt.
     * </p>
     * 
     * @param clr
     *            a {@link java.lang.String} object.
     * @param cnt
     *            a int.
     */
    /**
     * <p>
     * deckColors class.
     * </p>
     * 
     * @param clr
     *            a {@link java.lang.String} object.
     * @param cnt
     *            a int.
     */
    public CCnt(final String clr, final int cnt) {
        this.setColor(clr);
        this.setCount(cnt);
    }

    /**
     * @return the color
     */
    public String getColor() {
        return this.color;
    }

    /**
     * @param color
     *            the color to set
     */
    public void setColor(final String color) {
        this.color = color; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the count
     */
    public int getCount() {
        return this.count;
    }

    /**
     * @param count
     *            the count to set
     */
    public void setCount(final int count) {
        this.count = count; // TODO: Add 0 to parameter's name.
    }
}
