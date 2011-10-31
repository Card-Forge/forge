package forge.card.mana;

import forge.error.ErrorViewer;

/**
 * <p>
 * Mana_PartColorless class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Mana_PartColorless extends Mana_Part {
    private int manaNeeded;

    /**
     * <p>
     * addToManaNeeded.
     * </p>
     * 
     * @param additional
     *            a int.
     */
    public final void addToManaNeeded(final int additional) {
        this.manaNeeded += additional;
    }

    /**
     * <p>
     * Getter for the field <code>manaNeeded</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getManaNeeded() {
        return this.manaNeeded;
    }

    // String manaCostToPay is like "1", "4", but NO COLOR
    /**
     * <p>
     * Constructor for Mana_PartColorless.
     * </p>
     * 
     * @param manaCostToPay
     *            a {@link java.lang.String} object.
     */
    public Mana_PartColorless(final String manaCostToPay) {
        try {
            this.manaNeeded = Integer.parseInt(manaCostToPay);
        } catch (final NumberFormatException ex) {
            ErrorViewer.showError(ex, "mana cost is not a number - %s", manaCostToPay);
            throw new RuntimeException(String.format("mana cost is not a number - %s", manaCostToPay), ex);
        }
    }

    /**
     * <p>
     * Constructor for Mana_PartColorless.
     * </p>
     * 
     * @param manaCostToPay
     *            a int.
     */
    public Mana_PartColorless(final int manaCostToPay) {
        this.manaNeeded = manaCostToPay;
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        if (this.isPaid()) {
            return "";
        }

        return String.valueOf(this.manaNeeded);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isNeeded(final String mana) {
        // ManaPart method
        Mana_Part.checkSingleMana(mana);

        return 0 < this.manaNeeded;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isNeeded(final Mana mana) {
        // ManaPart method
        if (mana.getAmount() > 1) {
            throw new RuntimeException("Mana_PartColorless received Mana type with amount > 1");
        }

        return 0 < this.manaNeeded;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isColor(final String mana) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isColor(final Mana mana) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isEasierToPay(final Mana_Part mp) {
        // Colorless is always easier to Pay for
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void reduce(final String mana) {
        // if mana is needed, then this mana cost is all paid up
        if (!this.isNeeded(mana)) {
            throw new RuntimeException("Mana_PartColorless : reduce() error, argument mana not needed, mana - " + mana
                    + ", toString() - " + this.toString());
        }

        this.manaNeeded--;
    }

    /** {@inheritDoc} */
    @Override
    public final void reduce(final Mana mana) {
        // if mana is needed, then this mana cost is all paid up
        if (!this.isNeeded(mana)) {
            throw new RuntimeException("Mana_PartColorless : reduce() error, argument mana not needed, mana - " + mana
                    + ", toString() - " + this.toString());
        }

        this.manaNeeded--;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isPaid() {
        return this.manaNeeded == 0;
    }

    /** {@inheritDoc} */
    @Override
    public final int getConvertedManaCost() {
        return this.manaNeeded;
    }
}
