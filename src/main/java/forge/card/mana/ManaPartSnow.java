package forge.card.mana;

/**
 * <p>
 * Mana_PartSnow class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ManaPartSnow extends ManaPart {

    private boolean isPaid = false;

    /** {@inheritDoc} */
    @Override
    public final boolean isNeeded(final String mana) {
        return !this.isPaid && mana.equals("S");
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isNeeded(final Mana mana) {
        return !this.isPaid && mana.isSnow();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isColor(final String mana) {
        // ManaPart method
        return mana.indexOf("S") != -1;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isColor(final Mana mana) {
        return mana.isSnow();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isPaid() {
        return this.isPaid;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isEasierToPay(final ManaPart mp) {
        if (mp instanceof ManaPartColorless) {
            return false;
        }
        return this.toString().length() >= mp.toString().length();
    }

    /** {@inheritDoc} */
    @Override
    public final void reduce(final String mana) {
        if (!mana.equals("S")) {
            throw new RuntimeException("Mana_PartSnow: reduce() error, " + mana + " is not snow mana");
        }
        this.isPaid = true;
    }

    /** {@inheritDoc} */
    @Override
    public final void reduce(final Mana mana) {
        if (!mana.isSnow()) {
            throw new RuntimeException("Mana_PartSnow: reduce() error, " + mana + " is not snow mana");
        }
        this.isPaid = true;
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return (this.isPaid ? "" : "S");
    }

    /** {@inheritDoc} */
    @Override
    public final int getConvertedManaCost() {
        return 1;
    }

}
