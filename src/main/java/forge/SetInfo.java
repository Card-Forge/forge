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
    private String code;

    /** The Rarity. */
    private String rarity;

    /** The URL. */
    private String url;

    /** The Pic count. */
    private int picCount;

    /**
     * <p>
     * Constructor for SetInfo.
     * </p>
     */
    public SetInfo() {
        this.setCode("");
        this.setRarity("");
        this.setUrl("");
        this.setPicCount(0);
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
        this.setCode(c);
        this.setRarity(r);
        this.setUrl(u);
        this.setPicCount(0);
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
        this.setCode(c);
        this.setRarity(r);
        this.setUrl(u);
        this.setPicCount(p);
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
        final String[] pp = parse.split("\\|");
        this.setCode(pp[0]);
        this.setRarity(pp[1]);
        this.setUrl(pp[2]);
        if (pp.length > 3) {
            this.setPicCount(Integer.parseInt(pp[3]));
        } else {
            this.setPicCount(0);
        }
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String toString() {
        return this.getCode();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object o) {
        if (o instanceof SetInfo) {
            final SetInfo siO = (SetInfo) o;
            return this.getCode().equals(siO.getCode());
        } else {
            return false;
        }

    }

    /**
     * Gets the code.
     *
     * @return the code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Sets the code.
     *
     * @param code the code to set
     */
    public void setCode(final String code) {
        this.code = code; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the rarity.
     *
     * @return the rarity
     */
    public String getRarity() {
        return this.rarity;
    }

    /**
     * Sets the rarity.
     *
     * @param rarity the rarity to set
     */
    public void setRarity(final String rarity) {
        this.rarity = rarity; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the url.
     *
     * @return the url
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * Sets the url.
     *
     * @param url the url to set
     */
    public void setUrl(final String url) {
        this.url = url; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the pic count.
     *
     * @return the picCount
     */
    public int getPicCount() {
        return this.picCount;
    }

    /**
     * Sets the pic count.
     *
     * @param picCount the picCount to set
     */
    public void setPicCount(final int picCount) {
        this.picCount = picCount; // TODO: Add 0 to parameter's name.
    }
}
