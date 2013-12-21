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
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.esotericsoftware.minlog.Log;

import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.gui.toolbox.FSkin.ComponentSkin;
import forge.gui.toolbox.FSkin.SkinImage;

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
    private static final Map<String, SkinImage> MANA_IMAGES = new HashMap<String, SkinImage>();
    
    private static final int manaImageSize = 13; 

    /**
     * <p>
     * loadImages.
     * </p>
     */
    public static void loadImages() {
        for (int i = 0; i <= 20; i++) {
            MANA_IMAGES.put(String.valueOf(i), FSkin.getImage(FSkin.ColorlessManaImages.valueOf("IMG_" + i), manaImageSize, manaImageSize));
        }
        MANA_IMAGES.put("X", FSkin.getImage(FSkin.ColorlessManaImages.IMG_X, manaImageSize, manaImageSize));
        MANA_IMAGES.put("Y", FSkin.getImage(FSkin.ColorlessManaImages.IMG_Y, manaImageSize, manaImageSize));
        MANA_IMAGES.put("Z", FSkin.getImage(FSkin.ColorlessManaImages.IMG_Z, manaImageSize, manaImageSize));

        MANA_IMAGES.put("B", FSkin.getImage(FSkin.ManaImages.IMG_BLACK, manaImageSize, manaImageSize));
        MANA_IMAGES.put("BG", FSkin.getImage(FSkin.ManaImages.IMG_BLACK_GREEN, manaImageSize, manaImageSize));
        MANA_IMAGES.put("BR", FSkin.getImage(FSkin.ManaImages.IMG_BLACK_RED, manaImageSize, manaImageSize));
        MANA_IMAGES.put("G", FSkin.getImage(FSkin.ManaImages.IMG_GREEN, manaImageSize, manaImageSize));
        MANA_IMAGES.put("GU", FSkin.getImage(FSkin.ManaImages.IMG_GREEN_BLUE, manaImageSize, manaImageSize));
        MANA_IMAGES.put("GW", FSkin.getImage(FSkin.ManaImages.IMG_GREEN_WHITE, manaImageSize, manaImageSize));
        MANA_IMAGES.put("R", FSkin.getImage(FSkin.ManaImages.IMG_RED, manaImageSize, manaImageSize));
        MANA_IMAGES.put("RG", FSkin.getImage(FSkin.ManaImages.IMG_RED_GREEN, manaImageSize, manaImageSize));
        MANA_IMAGES.put("RW", FSkin.getImage(FSkin.ManaImages.IMG_RED_WHITE, manaImageSize, manaImageSize));
        MANA_IMAGES.put("U", FSkin.getImage(FSkin.ManaImages.IMG_BLUE, manaImageSize, manaImageSize));
        MANA_IMAGES.put("UB", FSkin.getImage(FSkin.ManaImages.IMG_BLUE_BLACK, manaImageSize, manaImageSize));
        MANA_IMAGES.put("UR", FSkin.getImage(FSkin.ManaImages.IMG_BLUE_RED, manaImageSize, manaImageSize));
        MANA_IMAGES.put("W", FSkin.getImage(FSkin.ManaImages.IMG_WHITE, manaImageSize, manaImageSize));
        MANA_IMAGES.put("WB", FSkin.getImage(FSkin.ManaImages.IMG_WHITE_BLACK, manaImageSize, manaImageSize));
        MANA_IMAGES.put("WU", FSkin.getImage(FSkin.ManaImages.IMG_WHITE_BLUE, manaImageSize, manaImageSize));
        MANA_IMAGES.put("PW", FSkin.getImage(FSkin.ManaImages.IMG_PHRYX_WHITE, manaImageSize, manaImageSize));
        MANA_IMAGES.put("PR", FSkin.getImage(FSkin.ManaImages.IMG_PHRYX_RED, manaImageSize, manaImageSize));
        MANA_IMAGES.put("PU", FSkin.getImage(FSkin.ManaImages.IMG_PHRYX_BLUE, manaImageSize, manaImageSize));
        MANA_IMAGES.put("PB", FSkin.getImage(FSkin.ManaImages.IMG_PHRYX_BLACK, manaImageSize, manaImageSize));
        MANA_IMAGES.put("PG", FSkin.getImage(FSkin.ManaImages.IMG_PHRYX_GREEN, manaImageSize, manaImageSize));
        MANA_IMAGES.put("2W", FSkin.getImage(FSkin.ManaImages.IMG_2W, manaImageSize, manaImageSize));
        MANA_IMAGES.put("2U", FSkin.getImage(FSkin.ManaImages.IMG_2U, manaImageSize, manaImageSize));
        MANA_IMAGES.put("2R", FSkin.getImage(FSkin.ManaImages.IMG_2R, manaImageSize, manaImageSize));
        MANA_IMAGES.put("2G", FSkin.getImage(FSkin.ManaImages.IMG_2G, manaImageSize, manaImageSize));
        MANA_IMAGES.put("2B", FSkin.getImage(FSkin.ManaImages.IMG_2B, manaImageSize, manaImageSize));

        MANA_IMAGES.put("S", FSkin.getImage(FSkin.GameplayImages.IMG_SNOW, manaImageSize, manaImageSize));
        MANA_IMAGES.put("T", FSkin.getImage(FSkin.GameplayImages.IMG_TAP, manaImageSize, manaImageSize));
        MANA_IMAGES.put("slash", FSkin.getImage(FSkin.GameplayImages.IMG_SLASH, manaImageSize, manaImageSize));
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

        MANA_IMAGES.put("foil11", FSkin.getImage(FSkin.OldFoils.FOIL_11));
        MANA_IMAGES.put("foil12", FSkin.getImage(FSkin.OldFoils.FOIL_12));
        MANA_IMAGES.put("foil13", FSkin.getImage(FSkin.OldFoils.FOIL_13));
        MANA_IMAGES.put("foil14", FSkin.getImage(FSkin.OldFoils.FOIL_14));
        MANA_IMAGES.put("foil15", FSkin.getImage(FSkin.OldFoils.FOIL_15));
        MANA_IMAGES.put("foil16", FSkin.getImage(FSkin.OldFoils.FOIL_16));
        MANA_IMAGES.put("foil17", FSkin.getImage(FSkin.OldFoils.FOIL_17));
        MANA_IMAGES.put("foil18", FSkin.getImage(FSkin.OldFoils.FOIL_18));
        MANA_IMAGES.put("foil19", FSkin.getImage(FSkin.OldFoils.FOIL_19));
        MANA_IMAGES.put("foil20", FSkin.getImage(FSkin.OldFoils.FOIL_20));
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
    public static void draw(final ComponentSkin<?> skin, Graphics g, ManaCost manaCost, int x, int y) {
        if (manaCost.isNoCost()) {
            return;
        }

        int xpos = x;
        final int offset = 14;
        final int genericManaCost = manaCost.getGenericCost();
        final boolean hasGeneric = (genericManaCost > 0) || manaCost.isPureGeneric();

        if (hasGeneric) {
            for (final ManaCostShard s : manaCost) { //render X shards before generic
                if (s == ManaCostShard.X) {
                    CardFaceSymbols.drawSymbol(s.getImageKey(), skin, g, xpos, y);
                    xpos += offset;
                }
            }

            final String sGeneric = Integer.toString(genericManaCost);
            CardFaceSymbols.drawSymbol(sGeneric, skin, g, xpos, y);
            xpos += offset;
    
            for (final ManaCostShard s : manaCost) { //render non-X shards after generic
                if (s != ManaCostShard.X) {
                    CardFaceSymbols.drawSymbol(s.getImageKey(), skin, g, xpos, y);
                    xpos += offset;
                }
            }
        }
        else { //if no generic, just render shards in order
            for (final ManaCostShard s : manaCost) {
                CardFaceSymbols.drawSymbol(s.getImageKey(), skin, g, xpos, y);
                xpos += offset;
            }
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
    public static void drawOther(final ComponentSkin<?> skin, final Graphics g, String s, int x, final int y, final int w, final int h) {
        if (s.length() == 0) {
            return;
        }

        StringTokenizer tok = new StringTokenizer(s, " ");
        while (tok.hasMoreTokens()) {
            String symbol = tok.nextToken();
            SkinImage image = MANA_IMAGES.get(symbol);
            if (image == null) {
                Log.info("Symbol not recognized \"" + symbol + "\" in string: " + s);
                continue;
            }
            skin.drawImage(g, image, x, y, w, h);
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
    public static void drawAttack(final ComponentSkin<?> skin, final Graphics g, final int x, final int y) {
        skin.drawImage(g, MANA_IMAGES.get("attack"), x, y);
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
    public static void drawSymbol(final String imageName, final ComponentSkin<?> skin, final Graphics g, final int x, final int y) {
        skin.drawImage(g, MANA_IMAGES.get(imageName), x, y);
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
        return manaCost.getGlyphCount() * 14;
    }
    
    public static int getHeight() {
        return 14;
    }
}
