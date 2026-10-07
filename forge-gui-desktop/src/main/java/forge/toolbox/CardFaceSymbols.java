package forge.toolbox;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.tinylog.Logger;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.gui.GuiBase;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FSkin.SkinImage;

/**
 * <p>
 * CardFaceSymbols class.
 * </p>
 *
 * @author Forge
 * @version $Id: CardFaceSymbols.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class CardFaceSymbols {
    /** Constant <code>manaImages</code>. */
    private static final Map<String, SkinImage> MANA_IMAGES = new HashMap<>();
    private static final Map<String, SkinImage> DECK_COLORSET = new HashMap<>();

    private static final int manaImageSize = 13;

    /**
     * <p>
     * loadImages.
     * </p>
     */
    public static void loadImages() {
        DECK_COLORSET.put("C", FSkin.getImage(FSkinProp.IMG_MANA_COLORLESS));
        DECK_COLORSET.put("R", FSkin.getImage(FSkinProp.IMG_MANA_R));
        DECK_COLORSET.put("G", FSkin.getImage(FSkinProp.IMG_MANA_G));
        DECK_COLORSET.put("B", FSkin.getImage(FSkinProp.IMG_MANA_B));
        DECK_COLORSET.put("U", FSkin.getImage(FSkinProp.IMG_MANA_U));
        DECK_COLORSET.put("W", FSkin.getImage(FSkinProp.IMG_MANA_W));

        for (Map.Entry<String, FSkinProp> e : FSkinProp.MANA_IMG.entrySet()) {
            MANA_IMAGES.put(e.getKey(), FSkin.getImage(e.getValue()));
        }

        MANA_IMAGES.put("E", FSkin.getImage(FSkinProp.IMG_ENERGY, 40, 40));
        MANA_IMAGES.put("TK", FSkin.getImage(FSkinProp.IMG_TICKET, 40, 40));
        MANA_IMAGES.put("EXPERIENCE", FSkin.getImage(FSkinProp.IMG_EXPERIENCE, 40, 30));
        MANA_IMAGES.put("CHAOS", FSkin.getImage(FSkinProp.IMG_CHAOS));
        MANA_IMAGES.put("slash", FSkin.getImage(FSkinProp.IMG_SLASH));
        MANA_IMAGES.put("attack", FSkin.getImage(FSkinProp.IMG_ATTACK, 32, 32));
        MANA_IMAGES.put("defend", FSkin.getImage(FSkinProp.IMG_DEFEND, 32, 32));
        MANA_IMAGES.put("summonsick", FSkin.getImage(FSkinProp.IMG_SUMMONSICK, 32, 32));
        MANA_IMAGES.put("phasing", FSkin.getImage(FSkinProp.IMG_PHASING, 32, 32));
        MANA_IMAGES.put("sacrifice", FSkin.getImage(FSkinProp.IMG_COSTRESERVED, 40, 40));
        MANA_IMAGES.put("counters1", FSkin.getImage(FSkinProp.IMG_COUNTERS1));
        MANA_IMAGES.put("counters2", FSkin.getImage(FSkinProp.IMG_COUNTERS2));
        MANA_IMAGES.put("counters3", FSkin.getImage(FSkinProp.IMG_COUNTERS3));
        MANA_IMAGES.put("countersMulti", FSkin.getImage(FSkinProp.IMG_COUNTERS_MULTI));

        MANA_IMAGES.put("foil01", FSkin.getImage(FSkinProp.FOIL_01));
        MANA_IMAGES.put("foil02", FSkin.getImage(FSkinProp.FOIL_02));
        MANA_IMAGES.put("foil03", FSkin.getImage(FSkinProp.FOIL_03));
        MANA_IMAGES.put("foil04", FSkin.getImage(FSkinProp.FOIL_04));
        MANA_IMAGES.put("foil05", FSkin.getImage(FSkinProp.FOIL_05));
        MANA_IMAGES.put("foil06", FSkin.getImage(FSkinProp.FOIL_06));
        MANA_IMAGES.put("foil07", FSkin.getImage(FSkinProp.FOIL_07));
        MANA_IMAGES.put("foil08", FSkin.getImage(FSkinProp.FOIL_08));
        MANA_IMAGES.put("foil09", FSkin.getImage(FSkinProp.FOIL_09));
        MANA_IMAGES.put("foil10", FSkin.getImage(FSkinProp.FOIL_10));

        MANA_IMAGES.put("foil11", FSkin.getImage(FSkinProp.FOIL_11));
        MANA_IMAGES.put("foil12", FSkin.getImage(FSkinProp.FOIL_12));
        MANA_IMAGES.put("foil13", FSkin.getImage(FSkinProp.FOIL_13));
        MANA_IMAGES.put("foil14", FSkin.getImage(FSkinProp.FOIL_14));
        MANA_IMAGES.put("foil15", FSkin.getImage(FSkinProp.FOIL_15));
        MANA_IMAGES.put("foil16", FSkin.getImage(FSkinProp.FOIL_16));
        MANA_IMAGES.put("foil17", FSkin.getImage(FSkinProp.FOIL_17));
        MANA_IMAGES.put("foil18", FSkin.getImage(FSkinProp.FOIL_18));
        MANA_IMAGES.put("foil19", FSkin.getImage(FSkinProp.FOIL_19));
        MANA_IMAGES.put("foil20", FSkin.getImage(FSkinProp.FOIL_20));

        //token icon
        MANA_IMAGES.put("token", FSkin.getImage(FSkinProp.IMG_ABILITY_TOKEN));
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
    public static void draw(final Graphics g, final ManaCost manaCost, final int x, final int y) {
        draw(g, manaCost, x, y, manaImageSize);
    }
    public static void draw(final Graphics g, final ManaCost manaCost, final int x, final int y, final int size) {
        if (manaCost.isNoCost()) {
            return;
        }

        int xpos = x;
        final int offset = size + 1;
        final int genericManaCost = manaCost.getGenericCost();
        final boolean hasGeneric = (genericManaCost > 0) || manaCost.isPureGeneric();

        if (hasGeneric) {
            for (final ManaCostShard s : manaCost) { //render X shards before generic
                if (s == ManaCostShard.X) {
                    CardFaceSymbols.drawSymbol(s.getImageKey(), g, xpos, y, size);
                    xpos += offset;
                }
            }

            final String sGeneric = Integer.toString(genericManaCost);
            CardFaceSymbols.drawSymbol(sGeneric, g, xpos, y, size);
            xpos += offset;

            for (final ManaCostShard s : manaCost) { //render non-X shards after generic
                if (s != ManaCostShard.X) {
                    CardFaceSymbols.drawSymbol(s.getImageKey(), g, xpos, y, size);
                    xpos += offset;
                }
            }
        }
        else { //if no generic, just render shards in order
            for (final ManaCostShard s : manaCost) {
                CardFaceSymbols.drawSymbol(s.getImageKey(), g, xpos, y, size);
                xpos += offset;
            }
        }
        // Show "negative" mana cost caused by perpetual cost reduction effects
        // This is only relevant for cards with an "X" in the cost
        if (genericManaCost < 0) {
            final String sGenericAdjust = Integer.toString(Math.abs(genericManaCost));
            drawSymbol(sGenericAdjust, g, xpos, y, size);
            // Give it a yellow border so it doesn't look like the regular generic mana symbol
            Stroke oldStroke = ((Graphics2D) g).getStroke();
            ((Graphics2D) g).setStroke(new BasicStroke(2));
            g.setColor(Color.YELLOW);
            g.drawOval(xpos, y, size, size);
            ((Graphics2D) g).setStroke(oldStroke);
            xpos += offset;
        }
    }

    public static void drawColorSet(Graphics g, ColorSet colorSet, int x, int y, int imageSize, boolean vertical) {
        for (final MagicColor.Color s : colorSet.getOrderedColors()) {
            if (DECK_COLORSET.get(s.getShortName())!=null)
                FSkin.drawImage(g, DECK_COLORSET.get(s.getShortName()), x, y, imageSize, imageSize);
            if (!vertical)
                x += imageSize;
            else
                y += imageSize;
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
    public static void drawOther(final Graphics g, final String s, int x, final int y, final int w, final int h) {
        if (s.isEmpty()) {
            return;
        }

        final StringTokenizer tok = new StringTokenizer(s, " ");
        while (tok.hasMoreTokens()) {
            final String symbol = tok.nextToken();
            final SkinImage image = MANA_IMAGES.get(symbol);
            if (image == null) {
                Logger.warn("Symbol not recognized \"" + symbol + "\" in string: " + s);
                continue;
            }
            FSkin.drawImage(g, image, x, y, w, h);
            x += symbol.length() > 2 ? 10 : 14; // slash.png is only 10 pixels wide.
        }
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
        FSkin.drawImage(g, MANA_IMAGES.get(imageName), x, y);
    }
    public static void drawManaSymbol(final String imageName, final Graphics g, final int x, final int y) {
        drawSymbol(imageName, g, x, y, manaImageSize);
    }
    public static void drawSymbol(final String imageName, final Graphics g, final int x, final int y, final int size) {
        // Obtain screen DPI scale
        float screenScale = GuiBase.getInterface().getScreenScale();
        int imageSize = Math.round(size * screenScale);

        FSkin.drawImage(g, MANA_IMAGES.get(imageName).resize(imageSize, imageSize),
            x, y, x + size, y + size, 0, 0, imageSize, imageSize);
    }
    public static void drawWatermark(final FSkinProp skinProp, final Graphics g, final int x, final int y, final int size) {
        if (skinProp == null) {
            return;
        }
        // Obtain screen DPI scale
        float screenScale = GuiBase.getInterface().getScreenScale();
        int imageSize = Math.round(size * screenScale);

        FSkin.drawImage(g, FSkin.getImage(skinProp).resize(imageSize, imageSize),
            x, y, x + size, y + size, 0, 0, imageSize, imageSize);
    }

    /**
     * <p>
     * Return width needed to draw mana symbols
     * </p>
     *
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int getWidth(final ManaCost manaCost) {
        return manaCost.getGlyphCount() * (manaImageSize + 1);
    }

    public static int getHeight() {
        return 14;
    }
}
