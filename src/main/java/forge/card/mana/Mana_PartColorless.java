package forge.card.mana;

import forge.error.ErrorViewer;


/**
 * <p>Mana_PartColorless class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Mana_PartColorless extends Mana_Part {
    private int manaNeeded;

    /**
     * <p>addToManaNeeded.</p>
     *
     * @param additional a int.
     */
    public void addToManaNeeded(int additional) {
        manaNeeded += additional;
    }

    /**
     * <p>Getter for the field <code>manaNeeded</code>.</p>
     *
     * @return a int.
     */
    public int getManaNeeded() {
        return manaNeeded;
    }

    //String manaCostToPay is like "1", "4", but NO COLOR
    /**
     * <p>Constructor for Mana_PartColorless.</p>
     *
     * @param manaCostToPay a {@link java.lang.String} object.
     */
    public Mana_PartColorless(String manaCostToPay) {
        try {
            manaNeeded = Integer.parseInt(manaCostToPay);
        } catch (NumberFormatException ex) {
            ErrorViewer.showError(ex, "mana cost is not a number - %s", manaCostToPay);
            throw new RuntimeException(String.format("mana cost is not a number - %s", manaCostToPay), ex);
        }
    }

    /**
     * <p>Constructor for Mana_PartColorless.</p>
     *
     * @param manaCostToPay a int.
     */
    public Mana_PartColorless(int manaCostToPay) {
        manaNeeded = manaCostToPay;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        if (isPaid()) return "";

        return String.valueOf(manaNeeded);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNeeded(String mana) {
        //ManaPart method
        checkSingleMana(mana);

        return 0 < manaNeeded;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNeeded(Mana mana) {
        //ManaPart method
        if (mana.getAmount() > 1) throw new RuntimeException("Mana_PartColorless received Mana type with amount > 1");

        return 0 < manaNeeded;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isColor(String mana) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isColor(Mana mana) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEasierToPay(Mana_Part mp) {
        // Colorless is always easier to Pay for
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void reduce(String mana) {
        //if mana is needed, then this mana cost is all paid up
        if (!isNeeded(mana)) throw new RuntimeException(
                "Mana_PartColorless : reduce() error, argument mana not needed, mana - " + mana
                        + ", toString() - " + toString());

        manaNeeded--;
    }

    /** {@inheritDoc} */
    @Override
    public void reduce(Mana mana) {
        //if mana is needed, then this mana cost is all paid up
        if (!isNeeded(mana)) throw new RuntimeException(
                "Mana_PartColorless : reduce() error, argument mana not needed, mana - " + mana
                        + ", toString() - " + toString());

        manaNeeded--;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPaid() {
        return manaNeeded == 0;
    }

    /** {@inheritDoc} */
    @Override
    public int getConvertedManaCost() {
        return manaNeeded;
    }
}
