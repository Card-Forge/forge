package forge.card.mana;


/**
 * <p>Abstract Mana_Part class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public abstract class Mana_Part {
    /** {@inheritDoc} */
    @Override
    abstract public String toString();

    /**
     * <p>reduce.</p>
     *
     * @param mana a {@link java.lang.String} object.
     */
    abstract public void reduce(String mana);

    /**
     * <p>reduce.</p>
     *
     * @param mana a {@link forge.card.mana.Mana} object.
     */
    abstract public void reduce(Mana mana);

    /**
     * <p>isPaid.</p>
     *
     * @return a boolean.
     */
    abstract public boolean isPaid();

    /**
     * <p>isNeeded.</p>
     *
     * @param mana a {@link java.lang.String} object.
     * @return a boolean.
     */
    abstract public boolean isNeeded(String mana);

    /**
     * <p>isNeeded.</p>
     *
     * @param mana a {@link forge.card.mana.Mana} object.
     * @return a boolean.
     */
    abstract public boolean isNeeded(Mana mana);

    /**
     * <p>isColor.</p>
     *
     * @param mana a {@link java.lang.String} object.
     * @return a boolean.
     */
    abstract public boolean isColor(String mana);

    /**
     * <p>isColor.</p>
     *
     * @param mana a {@link forge.card.mana.Mana} object.
     * @return a boolean.
     */
    abstract public boolean isColor(Mana mana);

    /**
     * <p>isEasierToPay.</p>
     *
     * @param mp a {@link forge.card.mana.Mana_Part} object.
     * @return a boolean.
     */
    abstract public boolean isEasierToPay(Mana_Part mp);

    /**
     * <p>getConvertedManaCost.</p>
     *
     * @return a int.
     */
    abstract public int getConvertedManaCost();

    /**
     * <p>checkSingleMana.</p>
     *
     * @param m a {@link java.lang.String} object.
     */
    public static void checkSingleMana(String m) {
        if (m.length() != 1) throw new RuntimeException(
                "Mana_Part : checkMana() error, argument mana is not of length 1, mana - " + m);

        if (!(m.equals("G") || m.equals("U") || m.equals("W") || m.equals("B") || m.equals("R") || m.equals("1") || m.equals("S") || m.startsWith("P")))
            throw new RuntimeException(
                    "Mana_Part : checkMana() error, argument mana is invalid mana, mana - " + m);
    }
}
