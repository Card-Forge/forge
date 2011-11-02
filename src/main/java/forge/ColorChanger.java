package forge;

import java.util.ArrayList;

import forge.card.mana.ManaCost;

/**
 * class ColorChanger. TODO Write javadoc for this type.
 * 
 */
public class ColorChanger {
    private ArrayList<CardColor> globalColorChanges = new ArrayList<CardColor>();

    /**
     * <p>
     * addColorChanges.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param addToColors
     *            a boolean.
     * @param bIncrease
     *            a boolean.
     * @return a long.
     */
    public final long addColorChanges(final String s, final Card c,
            final boolean addToColors, final boolean bIncrease) {
        if (bIncrease) {
            CardColor.increaseTimestamp();
        }
        globalColorChanges.add(new CardColor(new ManaCost(s), c, addToColors, false));
        return CardColor.getTimestamp();
    }

    /**
     * <p>
     * removeColorChanges.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param addTo
     *            a boolean.
     * @param timestamp
     *            a long.
     */
    public final void removeColorChanges(final String s, final Card c, final boolean addTo, final long timestamp) {
        CardColor removeCol = null;
        for (CardColor cc : globalColorChanges) {
            if (cc.equals(s, c, addTo, timestamp)) {
                removeCol = cc;
            }
        }

        if (removeCol != null) {
            globalColorChanges.remove(removeCol);
        }
    }

    /**
     * <p>
     * reset = clearColorChanges.
     * </p>
     */
    public final void reset() {
        clearColorChanges();
    }

    /**
     * <p>
     * clearColorChanges.
     * </p>
     */
    public final void clearColorChanges() {
        // clear the global color changes at end of each game
        globalColorChanges.clear();
    }

    /**
     * <p>
     * getColorChanges.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<CardColor> getColorChanges() {
        return globalColorChanges;
    }
}
