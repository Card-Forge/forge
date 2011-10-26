package forge.card.mana;

/**
 * <p>
 * Mana_PartPhyrexian class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Mana_PartPhyrexian extends Mana_Part {
    private Mana_PartColor wrappedColor;
    private String color;

    /**
     * <p>
     * Constructor for Mana_PartPhyrexian.
     * </p>
     * 
     * @param manaCostToPay
     *            a {@link java.lang.String} object.
     */
    public Mana_PartPhyrexian(final String manaCostToPay) {
        wrappedColor = new Mana_PartColor(manaCostToPay.substring(1));
        color = manaCostToPay.substring(1);
    }

    /** {@inheritDoc} */
    public final boolean isEasierToPay(final Mana_Part part) {
        return true;
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String toString() {
        return wrappedColor.toString().equals("") ? "" : "P" + wrappedColor.toString();
    }

    /**
     * <p>
     * isPaid.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isPaid() {
        return wrappedColor.isPaid();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * isColor.
     * </p>
     * 
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean isColor(final String mana) {
        return wrappedColor.isColor(mana);
    }

    /** {@inheritDoc} */
    public final boolean isColor(final Mana mana) {
        return wrappedColor.isColor(mana);
    }

    /**
     * <p>
     * isNeeded.
     * </p>
     * 
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean isNeeded(final String mana) {
        return wrappedColor.isNeeded(mana);
    }

    /** {@inheritDoc} */
    public final boolean isNeeded(final Mana mana) {
        return wrappedColor.isNeeded(mana);
    }

    /** {@inheritDoc} */
    public final void reduce(final String mana) {
        wrappedColor.reduce(mana);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * reduce.
     * </p>
     * 
     * @param mana
     *            a {@link forge.card.mana.Mana} object.
     */
    public final void reduce(final Mana mana) {
        wrappedColor.reduce(mana);
    }

    /**
     * <p>
     * getConvertedManaCost.
     * </p>
     * 
     * @return a int.
     */
    public final int getConvertedManaCost() {
        return wrappedColor.getConvertedManaCost();
    }

    /**
     * <p>
     * payLife.
     * </p>
     */
    public final void payLife() {
        wrappedColor.reduce(color);
    }
}
