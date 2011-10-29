package forge.card.mana;

import forge.Card;
import forge.Constant;
import forge.gui.input.Input_PayManaCostUtil;

/**
 * <p>
 * Mana class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Mana {
    private String color;
    private int amount = 0;
    private Card sourceCard = null;

    /**
     * <p>
     * Constructor for Mana.
     * </p>
     * 
     * @param col
     *            a {@link java.lang.String} object.
     * @param amt
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     */
    public Mana(final String col, final int amt, final Card source) {
        color = col;
        amount = amt;
        if (source == null) {
            return;
        }

        sourceCard = source;
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String toString() {
        if (color.equals(Constant.Color.COLORLESS)) {
            return Integer.toString(amount);
        }

        String manaString = "";
        StringBuilder sbMana = new StringBuilder();

        manaString = Input_PayManaCostUtil.getShortColorString(color);

        for (int i = 0; i < amount; i++) {
            sbMana.append(manaString);
        }
        return sbMana.toString();
    }

    /**
     * <p>
     * toDescriptiveString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String toDescriptiveString() {
        // this will be used for advanced choice box
        if (color.equals(Constant.Color.COLORLESS)) {
            return Integer.toString(amount);
        }

        String manaString = "";
        StringBuilder sbMana = new StringBuilder();

        manaString = Input_PayManaCostUtil.getShortColorString(color);

        for (int i = 0; i < amount; i++) {
            sbMana.append(manaString);
        }

        if (isSnow()) {
            sbMana.append("(S)");
        }

        sbMana.append(" From ");
        sbMana.append(sourceCard.getName());

        return sbMana.toString();
    }

    /**
     * <p>
     * toSingleArray.
     * </p>
     * 
     * @return an array of {@link forge.card.mana.Mana} objects.
     */
    public final Mana[] toSingleArray() {
        Mana[] normalize = new Mana[amount];
        for (int i = 0; i < normalize.length; i++) {
            normalize[i] = new Mana(this.color, 1, this.sourceCard);
        }
        return normalize;
    }

    /**
     * <p>
     * isSnow.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isSnow() {
        return sourceCard.isSnow();
    }

    /**
     * <p>
     * fromBasicLand.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean fromBasicLand() {
        return sourceCard.isBasicLand();
    } // for Imperiosaur

    /**
     * <p>
     * getColorlessAmount.
     * </p>
     * 
     * @return a int.
     */
    public final int getColorlessAmount() {
        return color.equals(Constant.Color.COLORLESS) ? amount : 0;
    }

    /**
     * <p>
     * Getter for the field <code>amount</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getAmount() {
        return amount;
    }

    /**
     * <p>
     * isColor.
     * </p>
     * 
     * @param col
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean isColor(final String col) {
        return color.equals(col);
    }

    /**
     * <p>
     * isColor.
     * </p>
     * 
     * @param colors
     *            an array of {@link java.lang.String} objects.
     * @return a boolean.
     */
    public final boolean isColor(final String[] colors) {
        for (String col : colors)
            if (color.equals(col)) {
                return true;
            }

        return false;
    }

    /**
     * <p>
     * Getter for the field <code>color</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getColor() {
        return color;
    }

    /**
     * <p>
     * Getter for the field <code>sourceCard</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getSourceCard() {
        return sourceCard;
    }

    /**
     * <p>
     * fromSourceCard.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean fromSourceCard(final Card c) {
        return sourceCard.equals(c);
    }

    /**
     * <p>
     * decrementAmount.
     * </p>
     */
    public final void decrementAmount() {
        amount--;
    }
}
