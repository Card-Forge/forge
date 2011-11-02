package forge.card.mana;

/**
 * <p>
 * Abstract Mana_Part class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class ManaPart {
    /** {@inheritDoc} */
    @Override
    public abstract String toString();

    /**
     * <p>
     * reduce.
     * </p>
     * 
     * @param mana
     *            a {@link java.lang.String} object.
     */
    public abstract void reduce(String mana);

    /**
     * <p>
     * reduce.
     * </p>
     * 
     * @param mana
     *            a {@link forge.card.mana.Mana} object.
     */
    public abstract void reduce(Mana mana);

    /**
     * <p>
     * isPaid.
     * </p>
     * 
     * @return a boolean.
     */
    public abstract boolean isPaid();

    /**
     * <p>
     * isNeeded.
     * </p>
     * 
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public abstract boolean isNeeded(String mana);

    /**
     * <p>
     * isNeeded.
     * </p>
     * 
     * @param mana
     *            a {@link forge.card.mana.Mana} object.
     * @return a boolean.
     */
    public abstract boolean isNeeded(Mana mana);

    /**
     * <p>
     * isColor.
     * </p>
     * 
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public abstract boolean isColor(String mana);

    /**
     * <p>
     * isColor.
     * </p>
     * 
     * @param mana
     *            a {@link forge.card.mana.Mana} object.
     * @return a boolean.
     */
    public abstract boolean isColor(Mana mana);

    /**
     * <p>
     * isEasierToPay.
     * </p>
     * 
     * @param mp
     *            a {@link forge.card.mana.ManaPart} object.
     * @return a boolean.
     */
    public abstract boolean isEasierToPay(ManaPart mp);

    /**
     * <p>
     * getConvertedManaCost.
     * </p>
     * 
     * @return a int.
     */
    public abstract int getConvertedManaCost();

    /**
     * <p>
     * checkSingleMana.
     * </p>
     * 
     * @param m
     *            a {@link java.lang.String} object.
     */
    public static void checkSingleMana(final String m) {
        if (m.length() != 1) {
            throw new RuntimeException("Mana_Part : checkMana() error, argument mana is not of length 1, mana - " + m);
        }

        if (!(m.equals("G") || m.equals("U") || m.equals("W") || m.equals("B") || m.equals("R") || m.equals("1")
                || m.equals("S") || m.startsWith("P"))) {
            throw new RuntimeException("Mana_Part : checkMana() error, argument mana is invalid mana, mana - " + m);
        }
    }
}
