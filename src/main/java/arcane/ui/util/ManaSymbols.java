package arcane.ui.util;

import java.awt.Graphics;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import com.esotericsoftware.minlog.Log;

/**
 * <p>
 * ManaSymbols class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ManaSymbols {
    /** Constant <code>manaImages</code>. */
    private static final Map<String, Image> manaImages = new HashMap<String, Image>();
    /** Constant <code>replaceSymbolsPattern</code>. */
    private static Pattern replaceSymbolsPattern = Pattern.compile("\\{([^}/]*)/?([^}]*)\\}");

    /**
     * <p>
     * loadImages.
     * </p>
     */
    public static void loadImages() {
        String[] symbols = new String[] {"0", "1", "10", "11", "12", "15", "16", "2", "20", "2W", "2U", "2R", "2G",
                "2B", "3", "4", "5", "6", "7", "8", "9", "B", "BG", "BR", "G", "GU", "GW", "R", "RG", "RW", "S", "T",
                "U", "UB", "UR", "W", "WB", "WU", "PW", "PU", "PB", "PR", "PG", "X", "Y", "Z", "slash", "attack",
                "defend", "summonsick", "phasing", "counters1", "counters2", "counters3", "countersMulti", "foil01",
                "foil02", "foil03", "foil04", "foil05", "foil06", "foil07", "foil08", "foil09", "foil10" };
        for (String symbol : symbols) {
            manaImages.put(symbol, UI.getImageIcon("res/images/symbols-13/" + symbol + ".png").getImage());
        }
    }

    /**
     * <p>
     * draw.
     * </p>
     * 
     * @param g
     *            a {@link java.awt.Graphics} object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @param x
     *            a int.
     * @param y
     *            a int.
     */
    public static void draw(Graphics g, String manaCost, int x, int y) {
        if (manaCost.length() == 0) {
            return;
        }
        manaCost = UI.getDisplayManaCost(manaCost);
        StringTokenizer tok = new StringTokenizer(manaCost, " ");
        while (tok.hasMoreTokens()) {
            String symbol = tok.nextToken();
            Image image = manaImages.get(symbol);
            if (image == null) {
                Log.info("Symbol not recognized \"" + symbol + "\" in mana cost: " + manaCost);
                continue;
            }
            g.drawImage(image, x, y, null);
            x += symbol.length() > 2 ? 10 : 14; // slash.png is only 10 pixels
                                                // wide.
        }
    }

    /**
     * 
     * draw.
     * @param g a Graphics
     * @param s a STring
     * @param x an int
     * @param y an int
     * @param w an int
     * @param h and int
     */
    public static void draw(final Graphics g, String s, int x, final int y, final int w, final int h) {
        if (s.length() == 0) {
            return;
        }
        s = UI.getDisplayManaCost(s);
        StringTokenizer tok = new StringTokenizer(s, " ");
        while (tok.hasMoreTokens()) {
            String symbol = tok.nextToken();
            Image image = manaImages.get(symbol);
            if (image == null) {
                Log.info("Symbol not recognized \"" + symbol + "\" in string: " + s);
                continue;
            }
            // g.drawImage(image, x, y, null);
            g.drawImage(image, x, y, w, h, null);
            x += symbol.length() > 2 ? 10 : 14; // slash.png is only 10 pixels
                                                // wide.
        }
    }

    /**
     * <p>
     * drawAttack.
     * </p>
     * 
     * @param g
     *            a {@link java.awt.Graphics} object.
     * @param x
     *            a int.
     * @param y
     *            a int.
     */
    public static void drawAttack(final Graphics g, final int x, final int y) {
        Image image = manaImages.get("attack");
        g.drawImage(image, x, y, null);
    }

    /**
     * <p>
     * drawSymbol.
     * </p>
     * 
     * @param imageName
     *            a {@link java.lang.String} object.
     * @param g
     *            a {@link java.awt.Graphics} object.
     * @param x
     *            a int.
     * @param y
     *            a int.
     */
    public static void drawSymbol(final String imageName, final Graphics g, final int x, final int y) {
        Image image = manaImages.get(imageName);
        g.drawImage(image, x, y, null);
    }

    /**
     * <p>
     * getWidth.
     * </p>
     * 
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int getWidth(final String manaCost) {
        int width = 0;
        StringTokenizer tok = new StringTokenizer(manaCost, " ");
        while (tok.hasMoreTokens()) {
            String symbol = tok.nextToken();
            width += symbol.length() > 2 ? 10 : 14; // slash.png is only 10
                                                    // pixels wide.
        }
        return width;
    }

    /**
     * <p>
     * replaceSymbolsWithHTML.
     * </p>
     * 
     * @param value
     *            a {@link java.lang.String} object.
     * @param small
     *            a boolean.
     * @return a {@link java.lang.String} object.
     */
    public static synchronized String replaceSymbolsWithHTML(String value, final boolean small) {
        if (small) {
            value = value.replace("{C}", "<img src='file:res/images/symbols-11/C.png' width=13 height=11>");
            return replaceSymbolsPattern.matcher(value).replaceAll(
                    "<img src='file:res/images/symbols-11/$1$2.png' width=11 height=11>");
        } else {
            value = value.replace("{slash}", "<img src='file:res/images/symbols-13/slash.png' width=10 height=13>");
            value = value.replace("{C}", "<img src='file:res/images/symbols-13/C.png' width=16 height=13>");
            return replaceSymbolsPattern.matcher(value).replaceAll(
                    "<img src='file:res/images/symbols-13/$1$2.png' width=13 height=13>");
        }
    }
}
