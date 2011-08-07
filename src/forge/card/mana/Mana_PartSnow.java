package forge.card.mana;

/**
 * <p>Mana_PartSnow class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class Mana_PartSnow extends Mana_Part {

    private boolean isPaid = false;

    /** {@inheritDoc} */
    @Override
    public boolean isNeeded(String mana) {
        return !isPaid && mana.equals("S");
    }

    /** {@inheritDoc} */
    public boolean isNeeded(Mana mana) {
        return !isPaid && mana.isSnow();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isColor(String mana) {
        //ManaPart method
        return mana.indexOf("S") != -1;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isColor(Mana mana) {
        return mana.isSnow();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPaid() {
        return isPaid;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEasierToPay(Mana_Part mp) {
        if (mp instanceof Mana_PartColorless) return false;
        return toString().length() >= mp.toString().length();
    }

    /** {@inheritDoc} */
    @Override
    public void reduce(String mana) {
        if (!mana.equals("S"))
            throw new RuntimeException("Mana_PartSnow: reduce() error, "
                    + mana + " is not snow mana");
        isPaid = true;
    }

    /** {@inheritDoc} */
    @Override
    public void reduce(Mana mana) {
        if (!mana.isSnow())
            throw new RuntimeException("Mana_PartSnow: reduce() error, "
                    + mana + " is not snow mana");
        isPaid = true;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return (isPaid ? "" : "S");
    }

    /** {@inheritDoc} */
    @Override
    public int getConvertedManaCost() {
        return 1;
    }

}
