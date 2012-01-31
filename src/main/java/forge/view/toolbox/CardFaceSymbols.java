/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.view.toolbox;

import java.awt.Graphics;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import arcane.ui.util.UI;

import com.esotericsoftware.minlog.Log;

import forge.Singletons;

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
        final FSkin skin = Singletons.getView().getSkin();
        MANA_IMAGES.put("0", skin.getImage(FSkin.ColorlessManaImages.IMG_0));
        MANA_IMAGES.put("1", skin.getImage(FSkin.ColorlessManaImages.IMG_1));
        MANA_IMAGES.put("2", skin.getImage(FSkin.ColorlessManaImages.IMG_2));
        MANA_IMAGES.put("3", skin.getImage(FSkin.ColorlessManaImages.IMG_3));
        MANA_IMAGES.put("4", skin.getImage(FSkin.ColorlessManaImages.IMG_4));
        MANA_IMAGES.put("5", skin.getImage(FSkin.ColorlessManaImages.IMG_5));
        MANA_IMAGES.put("6", skin.getImage(FSkin.ColorlessManaImages.IMG_6));
        MANA_IMAGES.put("7", skin.getImage(FSkin.ColorlessManaImages.IMG_7));
        MANA_IMAGES.put("8", skin.getImage(FSkin.ColorlessManaImages.IMG_8));
        MANA_IMAGES.put("9", skin.getImage(FSkin.ColorlessManaImages.IMG_9));
        MANA_IMAGES.put("10", skin.getImage(FSkin.ColorlessManaImages.IMG_10));
        MANA_IMAGES.put("11", skin.getImage(FSkin.ColorlessManaImages.IMG_11));
        MANA_IMAGES.put("12", skin.getImage(FSkin.ColorlessManaImages.IMG_12));
        MANA_IMAGES.put("15", skin.getImage(FSkin.ColorlessManaImages.IMG_15));
        MANA_IMAGES.put("16", skin.getImage(FSkin.ColorlessManaImages.IMG_16));
        MANA_IMAGES.put("20", skin.getImage(FSkin.ColorlessManaImages.IMG_20));
        MANA_IMAGES.put("X", skin.getImage(FSkin.ColorlessManaImages.IMG_X));
        MANA_IMAGES.put("Y", skin.getImage(FSkin.ColorlessManaImages.IMG_Y));
        MANA_IMAGES.put("Z", skin.getImage(FSkin.ColorlessManaImages.IMG_Z));

        MANA_IMAGES.put("B", skin.getImage(FSkin.ManaImages.IMG_BLACK));
        MANA_IMAGES.put("BG", skin.getImage(FSkin.ManaImages.IMG_BLACK_GREEN));
        MANA_IMAGES.put("BR", skin.getImage(FSkin.ManaImages.IMG_BLACK_RED));
        MANA_IMAGES.put("G", skin.getImage(FSkin.ManaImages.IMG_GREEN));
        MANA_IMAGES.put("GU", skin.getImage(FSkin.ManaImages.IMG_GREEN_BLUE));
        MANA_IMAGES.put("GW", skin.getImage(FSkin.ManaImages.IMG_GREEN_WHITE));
        MANA_IMAGES.put("R", skin.getImage(FSkin.ManaImages.IMG_RED));
        MANA_IMAGES.put("RG", skin.getImage(FSkin.ManaImages.IMG_RED_GREEN));
        MANA_IMAGES.put("RW", skin.getImage(FSkin.ManaImages.IMG_RED_WHITE));
        MANA_IMAGES.put("U", skin.getImage(FSkin.ManaImages.IMG_BLUE));
        MANA_IMAGES.put("UB", skin.getImage(FSkin.ManaImages.IMG_BLUE_BLACK));
        MANA_IMAGES.put("UR", skin.getImage(FSkin.ManaImages.IMG_BLUE_RED));
        MANA_IMAGES.put("W", skin.getImage(FSkin.ManaImages.IMG_WHITE));
        MANA_IMAGES.put("WB", skin.getImage(FSkin.ManaImages.IMG_WHITE_BLACK));
        MANA_IMAGES.put("WU", skin.getImage(FSkin.ManaImages.IMG_WHITE_BLUE));
        MANA_IMAGES.put("PW", skin.getImage(FSkin.ManaImages.IMG_PHRYX_WHITE));
        MANA_IMAGES.put("PR", skin.getImage(FSkin.ManaImages.IMG_PHRYX_RED));
        MANA_IMAGES.put("PU", skin.getImage(FSkin.ManaImages.IMG_PHRYX_BLUE));
        MANA_IMAGES.put("PB", skin.getImage(FSkin.ManaImages.IMG_PHRYX_BLACK));
        MANA_IMAGES.put("PG", skin.getImage(FSkin.ManaImages.IMG_PHRYX_GREEN));
        MANA_IMAGES.put("2W", skin.getImage(FSkin.ManaImages.IMG_2W));
        MANA_IMAGES.put("2U", skin.getImage(FSkin.ManaImages.IMG_2U));
        MANA_IMAGES.put("2R", skin.getImage(FSkin.ManaImages.IMG_2R));
        MANA_IMAGES.put("2G", skin.getImage(FSkin.ManaImages.IMG_2G));
        MANA_IMAGES.put("2B", skin.getImage(FSkin.ManaImages.IMG_2B));

        MANA_IMAGES.put("S", skin.getImage(FSkin.GameplayImages.IMG_SNOW));
        MANA_IMAGES.put("T", skin.getImage(FSkin.GameplayImages.IMG_TAP));
        MANA_IMAGES.put("slash", skin.getImage(FSkin.GameplayImages.IMG_SLASH));
        MANA_IMAGES.put("attack", skin.getImage(FSkin.GameplayImages.IMG_ATTACK));
        MANA_IMAGES.put("defend", skin.getImage(FSkin.GameplayImages.IMG_DEFEND));
        MANA_IMAGES.put("summonsick", skin.getImage(FSkin.GameplayImages.IMG_SUMMONSICK));
        MANA_IMAGES.put("phasing", skin.getImage(FSkin.GameplayImages.IMG_PHASING));
        MANA_IMAGES.put("counters1", skin.getImage(FSkin.GameplayImages.IMG_COUNTERS1));
        MANA_IMAGES.put("counters2", skin.getImage(FSkin.GameplayImages.IMG_COUNTERS2));
        MANA_IMAGES.put("counters3", skin.getImage(FSkin.GameplayImages.IMG_COUNTERS3));
        MANA_IMAGES.put("countersMulti", skin.getImage(FSkin.GameplayImages.IMG_COUNTERS_MULTI));

        MANA_IMAGES.put("foil01", skin.getImage(FSkin.Foils.FOIL_01));
        MANA_IMAGES.put("foil02", skin.getImage(FSkin.Foils.FOIL_02));
        MANA_IMAGES.put("foil03", skin.getImage(FSkin.Foils.FOIL_03));
        MANA_IMAGES.put("foil04", skin.getImage(FSkin.Foils.FOIL_04));
        MANA_IMAGES.put("foil05", skin.getImage(FSkin.Foils.FOIL_05));
        MANA_IMAGES.put("foil06", skin.getImage(FSkin.Foils.FOIL_06));
        MANA_IMAGES.put("foil07", skin.getImage(FSkin.Foils.FOIL_07));
        MANA_IMAGES.put("foil08", skin.getImage(FSkin.Foils.FOIL_08));
        MANA_IMAGES.put("foil09", skin.getImage(FSkin.Foils.FOIL_09));
        MANA_IMAGES.put("foil10", skin.getImage(FSkin.Foils.FOIL_10));
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
            Image image = MANA_IMAGES.get(symbol);
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
}
