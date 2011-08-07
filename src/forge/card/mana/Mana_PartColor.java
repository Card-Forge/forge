package forge.card.mana;

import forge.gui.input.Input_PayManaCostUtil;


/**
 * <p>Mana_PartColor class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class Mana_PartColor extends Mana_Part {
    private String manaCost;

    //String manaCostToPay is either "G" or "GW" NOT "3 G"
    //ManaPartColor only needs 1 mana in order to be paid
    //GW means it will accept either G or W like Selesnya Guildmage
    /**
     * <p>Constructor for Mana_PartColor.</p>
     *
     * @param manaCostToPay a {@link java.lang.String} object.
     */
    public Mana_PartColor(String manaCostToPay) {
        char[] c = manaCostToPay.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (i == 0 && c[i] == ' ') ;
            else checkSingleMana("" + c[i]);
        }

        manaCost = manaCostToPay;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return manaCost;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNeeded(String mana) {
        //ManaPart method
        checkSingleMana(mana);

        return !isPaid() && isColor(mana);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNeeded(Mana mana) {
        return (!isPaid() && isColor(mana));
    }

    /** {@inheritDoc} */
    @Override
    public boolean isColor(String mana) {
        //ManaPart method
        checkSingleMana(mana);

        return manaCost.indexOf(mana) != -1;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isColor(Mana mana) {
        String color = Input_PayManaCostUtil.getShortColorString(mana.getColor());

        return manaCost.indexOf(color) != -1;
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
        //if mana is needed, then this mana cost is all paid up
        if (!isNeeded(mana)) throw new RuntimeException(
                "Mana_PartColor : reduce() error, argument mana not needed, mana - " + mana + ", toString() - "
                        + toString());

        manaCost = "";
    }

    /** {@inheritDoc} */
    @Override
    public void reduce(Mana mana) {
        //if mana is needed, then this mana cost is all paid up
        if (!isNeeded(mana)) throw new RuntimeException(
                "Mana_PartColor : reduce() error, argument mana not needed, mana - " + mana + ", toString() - "
                        + toString());

        manaCost = "";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPaid() {
        return manaCost.length() == 0;
    }

    /** {@inheritDoc} */
    @Override
    public int getConvertedManaCost() {
        return 1;
    }
}
