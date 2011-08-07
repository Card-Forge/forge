package forge;

import forge.card.mana.ManaCost;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * <p>Card_Color class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class Card_Color {
    // takes care of individual card color, for global color change effects use AllZone.getGameInfo().getColorChanges()
    private EnumSet<Color> col;
    private boolean additional;

    /**
     * <p>Getter for the field <code>additional</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getAdditional() {
        return additional;
    }

    private Card effectingCard = null;
    private long stamp = 0;

    /**
     * <p>Getter for the field <code>stamp</code>.</p>
     *
     * @return a long.
     */
    public long getStamp() {
        return stamp;
    }

    /**
     * Constant <code>timeStamp=0</code>
     */
    private static long timeStamp = 0;

    /**
     * <p>getTimestamp.</p>
     *
     * @return a long.
     */
    public static long getTimestamp() {
        return timeStamp;
    }

    /**
     * <p>Constructor for Card_Color.</p>
     *
     * @param mc          a {@link forge.card.mana.ManaCost} object.
     * @param c           a {@link forge.Card} object.
     * @param addToColors a boolean.
     * @param baseColor   a boolean.
     */
    Card_Color(ManaCost mc, Card c, boolean addToColors, boolean baseColor) {
        additional = addToColors;
        col = Color.ConvertManaCostToColor(mc);
        effectingCard = c;
        if (baseColor)
            stamp = 0;
        else
            stamp = timeStamp;
    }

    /**
     * <p>Constructor for Card_Color.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public Card_Color(Card c) {
        col = Color.Colorless();
        additional = false;
        stamp = 0;
        effectingCard = c;
    }

    /**
     * <p>addToCardColor.</p>
     *
     * @param s a {@link java.lang.String} object.
     * @return a boolean.
     */
    boolean addToCardColor(String s) {
        Color c = Color.ConvertFromString(s);
        if (!col.contains(c)) {
            col.add(c);
            return true;
        }
        return false;
    }

    /**
     * <p>fixColorless.</p>
     */
    void fixColorless() {
        if (col.size() > 1 && col.contains(Color.Colorless))
            col.remove(Color.Colorless);
    }

    /**
     * <p>increaseTimestamp.</p>
     */
    static void increaseTimestamp() {
        timeStamp++;
    }

    /**
     * <p>equals.</p>
     *
     * @param cost        a {@link java.lang.String} object.
     * @param c           a {@link forge.Card} object.
     * @param addToColors a boolean.
     * @param time        a long.
     * @return a boolean.
     */
    public boolean equals(String cost, Card c, boolean addToColors, long time) {
        return effectingCard == c && addToColors == additional && stamp == time;
    }

    /**
     * <p>toStringArray.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<String> toStringArray() {
        ArrayList<String> list = new ArrayList<String>();
        for (Color c : col)
            list.add(c.toString());
        return list;
    }
}
