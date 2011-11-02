package forge.card.mana;

import forge.Card;
import forge.Constant;
import forge.gui.input.InputPayManaCostUtil;

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
        this.color = col;
        this.amount = amt;
        if (source == null) {
            return;
        }

        this.sourceCard = source;
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String toString() {
        if (this.color.equals(Constant.Color.COLORLESS)) {
            return Integer.toString(this.amount);
        }

        String manaString = "";
        final StringBuilder sbMana = new StringBuilder();

        manaString = InputPayManaCostUtil.getShortColorString(this.color);

        for (int i = 0; i < this.amount; i++) {
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
        if (this.color.equals(Constant.Color.COLORLESS)) {
            return Integer.toString(this.amount);
        }

        String manaString = "";
        final StringBuilder sbMana = new StringBuilder();

        manaString = InputPayManaCostUtil.getShortColorString(this.color);

        for (int i = 0; i < this.amount; i++) {
            sbMana.append(manaString);
        }

        if (this.isSnow()) {
            sbMana.append("(S)");
        }

        sbMana.append(" From ");
        sbMana.append(this.sourceCard.getName());

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
        final Mana[] normalize = new Mana[this.amount];
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
        return this.sourceCard.isSnow();
    }

    /**
     * <p>
     * fromBasicLand.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean fromBasicLand() {
        return this.sourceCard.isBasicLand();
    } // for Imperiosaur

    /**
     * <p>
     * getColorlessAmount.
     * </p>
     * 
     * @return a int.
     */
    public final int getColorlessAmount() {
        return this.color.equals(Constant.Color.COLORLESS) ? this.amount : 0;
    }

    /**
     * <p>
     * Getter for the field <code>amount</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getAmount() {
        return this.amount;
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
        return this.color.equals(col);
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
        for (final String col : colors) {
            if (this.color.equals(col)) {
                return true;
            }
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
        return this.color;
    }

    /**
     * <p>
     * Getter for the field <code>sourceCard</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getSourceCard() {
        return this.sourceCard;
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
        return this.sourceCard.equals(c);
    }

    /**
     * <p>
     * decrementAmount.
     * </p>
     */
    public final void decrementAmount() {
        this.amount--;
    }
}
