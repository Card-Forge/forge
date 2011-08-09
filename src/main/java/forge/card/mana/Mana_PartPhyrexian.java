package forge.card.mana;

/**
 * <p>Mana_PartPhyrexian class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Mana_PartPhyrexian extends Mana_Part {
    private Mana_PartColor wrappedColor;
    private String color;

    /**
     * <p>Constructor for Mana_PartPhyrexian.</p>
     *
     * @param manaCostToPay a {@link java.lang.String} object.
     */
    public Mana_PartPhyrexian(String manaCostToPay) {
        wrappedColor = new Mana_PartColor(manaCostToPay.substring(1));
        color = manaCostToPay.substring(1);
    }

    /** {@inheritDoc} */
    public boolean isEasierToPay(Mana_Part part) {
        return true;
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString() {
        return wrappedColor.toString().equals("") ? "" : "P" + wrappedColor.toString();
    }

    /**
     * <p>isPaid.</p>
     *
     * @return a boolean.
     */
    public boolean isPaid() {
        return wrappedColor.isPaid();
    }

    /**
     * {@inheritDoc}
     *
     * <p>isColor.</p>
     *
     * @param mana a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isColor(String mana) {
        return wrappedColor.isColor(mana);
    }

    /** {@inheritDoc} */
    public boolean isColor(Mana mana) {
        return wrappedColor.isColor(mana);
    }

    /**
     * <p>isNeeded.</p>
     *
     * @param mana a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isNeeded(String mana) {
        return wrappedColor.isNeeded(mana);
    }

    /** {@inheritDoc} */
    public boolean isNeeded(Mana mana) {
        return wrappedColor.isNeeded(mana);
    }

    /** {@inheritDoc} */
    public void reduce(String mana) {
        wrappedColor.reduce(mana);
    }

    /**
     * {@inheritDoc}
     *
     * <p>reduce.</p>
     *
     * @param mana a {@link forge.card.mana.Mana} object.
     */
    public void reduce(Mana mana) {
        wrappedColor.reduce(mana);
    }

    /**
     * <p>getConvertedManaCost.</p>
     *
     * @return a int.
     */
    public int getConvertedManaCost() {
        return wrappedColor.getConvertedManaCost();
    }

    /**
     * <p>payLife.</p>
     */
    public void payLife() {
        wrappedColor.reduce(color);
    }
}
