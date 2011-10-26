package forge;

/**
 * <p>
 * SetInfo class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class SetInfo {

    /** The Code. */
    public String Code;

    /** The Rarity. */
    public String Rarity;

    /** The URL. */
    public String URL;

    /** The Pic count. */
    public int PicCount;

    /**
     * <p>
     * Constructor for SetInfo.
     * </p>
     */
    public SetInfo() {
        Code = "";
        Rarity = "";
        URL = "";
        PicCount = 0;
    }

    /**
     * <p>
     * Constructor for SetInfo.
     * </p>
     * 
     * @param c
     *            a {@link java.lang.String} object.
     * @param r
     *            a {@link java.lang.String} object.
     * @param u
     *            a {@link java.lang.String} object.
     */
    public SetInfo(final String c, final String r, final String u) {
        Code = c;
        Rarity = r;
        URL = u;
        PicCount = 0;
    }

    /**
     * <p>
     * Constructor for SetInfo.
     * </p>
     * 
     * @param c
     *            a {@link java.lang.String} object.
     * @param r
     *            a {@link java.lang.String} object.
     * @param u
     *            a {@link java.lang.String} object.
     * @param p
     *            a int.
     */
    public SetInfo(final String c, final String r, final String u, final int p) {
        Code = c;
        Rarity = r;
        URL = u;
        PicCount = p;
    }

    /**
     * <p>
     * Constructor for SetInfo.
     * </p>
     * 
     * @param parse
     *            a {@link java.lang.String} object.
     */
    public SetInfo(final String parse) {
        String[] pp = parse.split("\\|");
        Code = pp[0];
        Rarity = pp[1];
        URL = pp[2];
        if (pp.length > 3) {
            PicCount = Integer.parseInt(pp[3]);
        } else {
            PicCount = 0;
        }
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String toString() {
        return Code;
    }

    /** {@inheritDoc} */
    public final boolean equals(final Object o) {
        if (o instanceof SetInfo) {
            SetInfo siO = (SetInfo) o;
            return Code.equals(siO.Code);
        } else {
            return false;
        }

    }
}
