/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gui.toolbox;

import java.awt.Graphics;
import java.awt.Image;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


import com.esotericsoftware.minlog.Log;

import forge.card.mana.ManaCostShard;
import forge.card.mana.ManaCost;

/**
 * <p>
 * CardFaceSymbols class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFaceSymbols {
    /** Constant <code>manaImages</code>. */
    private static final Map<String, Image> MANA_IMAGES = new HashMap<String, Image>();

    /**
     * <p>
     * loadImages.
     * </p>
     */
    public static void loadImages() {
        MANA_IMAGES.put("0", FSkin.getImage(FSkin.ColorlessManaImages.IMG_0, 13, 13));
        MANA_IMAGES.put("1", FSkin.getImage(FSkin.ColorlessManaImages.IMG_1, 13, 13));
        MANA_IMAGES.put("2", FSkin.getImage(FSkin.ColorlessManaImages.IMG_2, 13, 13));
        MANA_IMAGES.put("3", FSkin.getImage(FSkin.ColorlessManaImages.IMG_3, 13, 13));
        MANA_IMAGES.put("4", FSkin.getImage(FSkin.ColorlessManaImages.IMG_4, 13, 13));
        MANA_IMAGES.put("5", FSkin.getImage(FSkin.ColorlessManaImages.IMG_5, 13, 13));
        MANA_IMAGES.put("6", FSkin.getImage(FSkin.ColorlessManaImages.IMG_6, 13, 13));
        MANA_IMAGES.put("7", FSkin.getImage(FSkin.ColorlessManaImages.IMG_7, 13, 13));
        MANA_IMAGES.put("8", FSkin.getImage(FSkin.ColorlessManaImages.IMG_8, 13, 13));
        MANA_IMAGES.put("9", FSkin.getImage(FSkin.ColorlessManaImages.IMG_9, 13, 13));
        MANA_IMAGES.put("10", FSkin.getImage(FSkin.ColorlessManaImages.IMG_10, 13, 13));
        MANA_IMAGES.put("11", FSkin.getImage(FSkin.ColorlessManaImages.IMG_11, 13, 13));
        MANA_IMAGES.put("12", FSkin.getImage(FSkin.ColorlessManaImages.IMG_12, 13, 13));
        MANA_IMAGES.put("15", FSkin.getImage(FSkin.ColorlessManaImages.IMG_15, 13, 13));
        MANA_IMAGES.put("16", FSkin.getImage(FSkin.ColorlessManaImages.IMG_16, 13, 13));
        MANA_IMAGES.put("20", FSkin.getImage(FSkin.ColorlessManaImages.IMG_20, 13, 13));
        MANA_IMAGES.put("X", FSkin.getImage(FSkin.ColorlessManaImages.IMG_X, 13, 13));
        MANA_IMAGES.put("Y", FSkin.getImage(FSkin.ColorlessManaImages.IMG_Y, 13, 13));
        MANA_IMAGES.put("Z", FSkin.getImage(FSkin.ColorlessManaImages.IMG_Z, 13, 13));

        MANA_IMAGES.put("B", FSkin.getImage(FSkin.ManaImages.IMG_BLACK, 13, 13));
        MANA_IMAGES.put("BG", FSkin.getImage(FSkin.ManaImages.IMG_BLACK_GREEN, 13, 13));
        MANA_IMAGES.put("BR", FSkin.getImage(FSkin.ManaImages.IMG_BLACK_RED, 13, 13));
        MANA_IMAGES.put("G", FSkin.getImage(FSkin.ManaImages.IMG_GREEN, 13, 13));
        MANA_IMAGES.put("GU", FSkin.getImage(FSkin.ManaImages.IMG_GREEN_BLUE, 13, 13));
        MANA_IMAGES.put("GW", FSkin.getImage(FSkin.ManaImages.IMG_GREEN_WHITE, 13, 13));
        MANA_IMAGES.put("R", FSkin.getImage(FSkin.ManaImages.IMG_RED, 13, 13));
        MANA_IMAGES.put("RG", FSkin.getImage(FSkin.ManaImages.IMG_RED_GREEN, 13, 13));
        MANA_IMAGES.put("RW", FSkin.getImage(FSkin.ManaImages.IMG_RED_WHITE, 13, 13));
        MANA_IMAGES.put("U", FSkin.getImage(FSkin.ManaImages.IMG_BLUE, 13, 13));
        MANA_IMAGES.put("UB", FSkin.getImage(FSkin.ManaImages.IMG_BLUE_BLACK, 13, 13));
        MANA_IMAGES.put("UR", FSkin.getImage(FSkin.ManaImages.IMG_BLUE_RED, 13, 13));
        MANA_IMAGES.put("W", FSkin.getImage(FSkin.ManaImages.IMG_WHITE, 13, 13));
        MANA_IMAGES.put("WB", FSkin.getImage(FSkin.ManaImages.IMG_WHITE_BLACK, 13, 13));
        MANA_IMAGES.put("WU", FSkin.getImage(FSkin.ManaImages.IMG_WHITE_BLUE, 13, 13));
        MANA_IMAGES.put("PW", FSkin.getImage(FSkin.ManaImages.IMG_PHRYX_WHITE, 13, 13));
        MANA_IMAGES.put("PR", FSkin.getImage(FSkin.ManaImages.IMG_PHRYX_RED, 13, 13));
        MANA_IMAGES.put("PU", FSkin.getImage(FSkin.ManaImages.IMG_PHRYX_BLUE, 13, 13));
        MANA_IMAGES.put("PB", FSkin.getImage(FSkin.ManaImages.IMG_PHRYX_BLACK, 13, 13));
        MANA_IMAGES.put("PG", FSkin.getImage(FSkin.ManaImages.IMG_PHRYX_GREEN, 13, 13));
        MANA_IMAGES.put("2W", FSkin.getImage(FSkin.ManaImages.IMG_2W, 13, 13));
        MANA_IMAGES.put("2U", FSkin.getImage(FSkin.ManaImages.IMG_2U, 13, 13));
        MANA_IMAGES.put("2R", FSkin.getImage(FSkin.ManaImages.IMG_2R, 13, 13));
        MANA_IMAGES.put("2G", FSkin.getImage(FSkin.ManaImages.IMG_2G, 13, 13));
        MANA_IMAGES.put("2B", FSkin.getImage(FSkin.ManaImages.IMG_2B, 13, 13));

        MANA_IMAGES.put("S", FSkin.getImage(FSkin.GameplayImages.IMG_SNOW, 13, 13));
        MANA_IMAGES.put("T", FSkin.getImage(FSkin.GameplayImages.IMG_TAP, 13, 13));
        MANA_IMAGES.put("slash", FSkin.getImage(FSkin.GameplayImages.IMG_SLASH, 13, 13));
        MANA_IMAGES.put("attack", FSkin.getImage(FSkin.GameplayImages.IMG_ATTACK, 32, 32));
        MANA_IMAGES.put("defend", FSkin.getImage(FSkin.GameplayImages.IMG_DEFEND, 32, 32));
        MANA_IMAGES.put("summonsick", FSkin.getImage(FSkin.GameplayImages.IMG_SUMMONSICK, 32, 32));
        MANA_IMAGES.put("phasing", FSkin.getImage(FSkin.GameplayImages.IMG_PHASING, 32, 32));
        MANA_IMAGES.put("sacrifice", FSkin.getImage(FSkin.GameplayImages.IMG_COSTRESERVED, 40, 40));
        MANA_IMAGES.put("counters1", FSkin.getImage(FSkin.GameplayImages.IMG_COUNTERS1));
        MANA_IMAGES.put("counters2", FSkin.getImage(FSkin.GameplayImages.IMG_COUNTERS2));
        MANA_IMAGES.put("counters3", FSkin.getImage(FSkin.GameplayImages.IMG_COUNTERS3));
        MANA_IMAGES.put("countersMulti", FSkin.getImage(FSkin.GameplayImages.IMG_COUNTERS_MULTI));

        MANA_IMAGES.put("foil01", FSkin.getImage(FSkin.Foils.FOIL_01));
        MANA_IMAGES.put("foil02", FSkin.getImage(FSkin.Foils.FOIL_02));
        MANA_IMAGES.put("foil03", FSkin.getImage(FSkin.Foils.FOIL_03));
        MANA_IMAGES.put("foil04", FSkin.getImage(FSkin.Foils.FOIL_04));
        MANA_IMAGES.put("foil05", FSkin.getImage(FSkin.Foils.FOIL_05));
        MANA_IMAGES.put("foil06", FSkin.getImage(FSkin.Foils.FOIL_06));
        MANA_IMAGES.put("foil07", FSkin.getImage(FSkin.Foils.FOIL_07));
        MANA_IMAGES.put("foil08", FSkin.getImage(FSkin.Foils.FOIL_08));
        MANA_IMAGES.put("foil09", FSkin.getImage(FSkin.Foils.FOIL_09));
        MANA_IMAGES.put("foil10", FSkin.getImage(FSkin.Foils.FOIL_10));
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
    public static void draw(Graphics g, ManaCost manaCost, int x, int y) {
        if (manaCost.isNoCost()) {
            return;
        }

        final int genericManaCost = manaCost.getGenericCost();
        final boolean hasGeneric = (genericManaCost > 0) || manaCost.isPureGeneric();
        final List<ManaCostShard> shards = manaCost.getShards();

        int xpos = x;
        final int offset = 14;
        if (hasGeneric) {
            final String sGeneric = Integer.toString(genericManaCost);
            CardFaceSymbols.drawSymbol(sGeneric, g, xpos, y);
            xpos += offset;
        }

        for (final ManaCostShard s : shards) {
            CardFaceSymbols.drawSymbol(s.getImageKey(), g, xpos, y);
            xpos += offset;
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
    public static void drawOther(final Graphics g, String s, int x, final int y, final int w, final int h) {
        if (s.length() == 0) {
            return;
        }

        StringTokenizer tok = new StringTokenizer(s, " ");
        while (tok.hasMoreTokens()) {
            String symbol = tok.nextToken();
            Image image = MANA_IMAGES.get(symbol);
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
        Image image = MANA_IMAGES.get("attack");
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
        Image image = MANA_IMAGES.get(imageName);
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
    public static int getWidth(final ManaCost manaCost) {
        int width = manaCost.getShards().size();
        if (manaCost.getGenericCost() > 0 || (manaCost.getGenericCost() == 0 && width == 0)) {
            width++;
        }

        //System.out.println(String.format("%d for %s", width, manaCost.toString()));
        return width * 14;
    }
    
    public static int getHeight() {
        return 14;
    }
}
