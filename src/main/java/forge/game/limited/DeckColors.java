package forge.game.limited;

import forge.Constant;

/**
 * Created by IntelliJ IDEA. User: dhudson Date: 6/24/11 Time: 8:42 PM To change
 * this template use File | Settings | File Templates.
 */
class DeckColors {

    /** The Color1. */
    private String color1 = "none";

    /** The Color2. */
    private String color2 = "none";
    // public String Splash = "none";
    /** The Mana1. */
    private String mana1 = "";

    /** The Mana2. */
    private String mana2 = "";

    // public String ManaS = "";

    /**
     * <p>
     * Constructor for deckColors.
     * </p>
     * 
     * @param c1
     *            a {@link java.lang.String} object.
     * @param c2
     *            a {@link java.lang.String} object.
     * @param sp
     *            a {@link java.lang.String} object.
     */
    public DeckColors(final String c1, final String c2, final String sp) {
        this.setColor1(c1);
        this.setColor2(c2);
        // Splash = sp;
    }

    /**
     * <p>
     * Constructor for DeckColors.
     * </p>
     */
    public DeckColors() {

    }

    /**
     * <p>
     * ColorToMana.
     * </p>
     * 
     * @param color
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String colorToMana(final String color) {
        final String[] mana = { "W", "U", "B", "R", "G" };

        for (int i = 0; i < Constant.Color.ONLY_COLORS.length; i++) {
            if (Constant.Color.ONLY_COLORS[i].equals(color)) {
                return mana[i];
            }
        }

        return "";
    }

    /**
     * @return the color1
     */
    public String getColor1() {
        return this.color1;
    }

    /**
     * @param color1
     *            the color1 to set
     */
    public void setColor1(final String color1) {
        this.color1 = color1; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the mana1
     */
    public String getMana1() {
        return this.mana1;
    }

    /**
     * @param mana1
     *            the mana1 to set
     */
    public void setMana1(final String mana1) {
        this.mana1 = mana1; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the mana2
     */
    public String getMana2() {
        return this.mana2;
    }

    /**
     * @param mana2
     *            the mana2 to set
     */
    public void setMana2(final String mana2) {
        this.mana2 = mana2; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the color2
     */
    public String getColor2() {
        return this.color2;
    }

    /**
     * @param color2
     *            the color2 to set
     */
    public void setColor2(final String color2) {
        this.color2 = color2; // TODO: Add 0 to parameter's name.
    }

}
