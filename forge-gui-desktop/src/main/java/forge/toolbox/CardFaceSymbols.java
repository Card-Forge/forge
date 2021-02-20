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
package forge.toolbox;

import com.esotericsoftware.minlog.Log;
import forge.assets.FSkinProp;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.toolbox.FSkin.SkinImage;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

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

    private static final int manaImageSize = 13;

    /**
     * <p>
     * loadImages.
     * </p>
     */
    public static void loadImages() {
        for (int i = 0; i <= 20; i++) {
            MANA_IMAGES.put(String.valueOf(i), FSkin.getImage(FSkinProp.valueOf("IMG_MANA_" + i), manaImageSize, manaImageSize));
        }
        MANA_IMAGES.put("X", FSkin.getImage(FSkinProp.IMG_MANA_X, manaImageSize, manaImageSize));
        MANA_IMAGES.put("Y", FSkin.getImage(FSkinProp.IMG_MANA_Y, manaImageSize, manaImageSize));
        MANA_IMAGES.put("Z", FSkin.getImage(FSkinProp.IMG_MANA_Z, manaImageSize, manaImageSize));
        MANA_IMAGES.put("C", FSkin.getImage(FSkinProp.IMG_MANA_COLORLESS, manaImageSize, manaImageSize));

        MANA_IMAGES.put("B",  FSkin.getImage(FSkinProp.IMG_MANA_B, manaImageSize, manaImageSize));
        MANA_IMAGES.put("BG", FSkin.getImage(FSkinProp.IMG_MANA_HYBRID_BG, manaImageSize, manaImageSize));
        MANA_IMAGES.put("BR", FSkin.getImage(FSkinProp.IMG_MANA_HYBRID_BR, manaImageSize, manaImageSize));
        MANA_IMAGES.put("G",  FSkin.getImage(FSkinProp.IMG_MANA_G, manaImageSize, manaImageSize));
        MANA_IMAGES.put("GU", FSkin.getImage(FSkinProp.IMG_MANA_HYBRID_GU, manaImageSize, manaImageSize));
        MANA_IMAGES.put("GW", FSkin.getImage(FSkinProp.IMG_MANA_HYBRID_GW, manaImageSize, manaImageSize));
        MANA_IMAGES.put("R",  FSkin.getImage(FSkinProp.IMG_MANA_R, manaImageSize, manaImageSize));
        MANA_IMAGES.put("RG", FSkin.getImage(FSkinProp.IMG_MANA_HYBRID_RG, manaImageSize, manaImageSize));
        MANA_IMAGES.put("RW", FSkin.getImage(FSkinProp.IMG_MANA_HYBRID_RW, manaImageSize, manaImageSize));
        MANA_IMAGES.put("U",  FSkin.getImage(FSkinProp.IMG_MANA_U, manaImageSize, manaImageSize));
        MANA_IMAGES.put("UB", FSkin.getImage(FSkinProp.IMG_MANA_HYBRID_UB, manaImageSize, manaImageSize));
        MANA_IMAGES.put("UR", FSkin.getImage(FSkinProp.IMG_MANA_HYBRID_UR, manaImageSize, manaImageSize));
        MANA_IMAGES.put("W",  FSkin.getImage(FSkinProp.IMG_MANA_W, manaImageSize, manaImageSize));
        MANA_IMAGES.put("WB", FSkin.getImage(FSkinProp.IMG_MANA_HYBRID_WB, manaImageSize, manaImageSize));
        MANA_IMAGES.put("WU", FSkin.getImage(FSkinProp.IMG_MANA_HYBRID_WU, manaImageSize, manaImageSize));
        MANA_IMAGES.put("PW", FSkin.getImage(FSkinProp.IMG_MANA_PHRYX_W, manaImageSize, manaImageSize));
        MANA_IMAGES.put("PR", FSkin.getImage(FSkinProp.IMG_MANA_PHRYX_R, manaImageSize, manaImageSize));
        MANA_IMAGES.put("PU", FSkin.getImage(FSkinProp.IMG_MANA_PHRYX_U, manaImageSize, manaImageSize));
        MANA_IMAGES.put("PB", FSkin.getImage(FSkinProp.IMG_MANA_PHRYX_B, manaImageSize, manaImageSize));
        MANA_IMAGES.put("PG", FSkin.getImage(FSkinProp.IMG_MANA_PHRYX_G, manaImageSize, manaImageSize));
        MANA_IMAGES.put("2W", FSkin.getImage(FSkinProp.IMG_MANA_2W, manaImageSize, manaImageSize));
        MANA_IMAGES.put("2U", FSkin.getImage(FSkinProp.IMG_MANA_2U, manaImageSize, manaImageSize));
        MANA_IMAGES.put("2R", FSkin.getImage(FSkinProp.IMG_MANA_2R, manaImageSize, manaImageSize));
        MANA_IMAGES.put("2G", FSkin.getImage(FSkinProp.IMG_MANA_2G, manaImageSize, manaImageSize));
        MANA_IMAGES.put("2B", FSkin.getImage(FSkinProp.IMG_MANA_2B, manaImageSize, manaImageSize));

        MANA_IMAGES.put("S", FSkin.getImage(FSkinProp.IMG_MANA_SNOW, manaImageSize, manaImageSize));
        MANA_IMAGES.put("T", FSkin.getImage(FSkinProp.IMG_TAP, manaImageSize, manaImageSize));
        MANA_IMAGES.put("E", FSkin.getImage(FSkinProp.IMG_ENERGY, 40, 40));
        MANA_IMAGES.put("EXPERIENCE", FSkin.getImage(FSkinProp.IMG_EXPERIENCE, 40, 30));
        MANA_IMAGES.put("slash", FSkin.getImage(FSkinProp.IMG_SLASH, manaImageSize, manaImageSize));
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

        //ability icons
        MANA_IMAGES.put("commander", FSkin.getImage(FSkinProp.IMG_ABILITY_COMMANDER));
        MANA_IMAGES.put("deathtouch", FSkin.getImage(FSkinProp.IMG_ABILITY_DEATHTOUCH));
        MANA_IMAGES.put("defender", FSkin.getImage(FSkinProp.IMG_ABILITY_DEFENDER));
        MANA_IMAGES.put("doublestrike", FSkin.getImage(FSkinProp.IMG_ABILITY_DOUBLE_STRIKE));
        MANA_IMAGES.put("firststrike", FSkin.getImage(FSkinProp.IMG_ABILITY_FIRST_STRIKE));
        MANA_IMAGES.put("fear", FSkin.getImage(FSkinProp.IMG_ABILITY_FEAR));
        MANA_IMAGES.put("flash", FSkin.getImage(FSkinProp.IMG_ABILITY_FLASH));
        MANA_IMAGES.put("flying", FSkin.getImage(FSkinProp.IMG_ABILITY_FLYING));
        MANA_IMAGES.put("haste", FSkin.getImage(FSkinProp.IMG_ABILITY_HASTE));
        MANA_IMAGES.put("hexproof", FSkin.getImage(FSkinProp.IMG_ABILITY_HEXPROOF));
        MANA_IMAGES.put("indestructible", FSkin.getImage(FSkinProp.IMG_ABILITY_INDESTRUCTIBLE));
        MANA_IMAGES.put("intimidate", FSkin.getImage(FSkinProp.IMG_ABILITY_INTIMIDATE));
        MANA_IMAGES.put("lifelink", FSkin.getImage(FSkinProp.IMG_ABILITY_LIFELINK));
        MANA_IMAGES.put("menace", FSkin.getImage(FSkinProp.IMG_ABILITY_MENACE));
        MANA_IMAGES.put("reach", FSkin.getImage(FSkinProp.IMG_ABILITY_REACH));
        MANA_IMAGES.put("shroud", FSkin.getImage(FSkinProp.IMG_ABILITY_SHROUD));
        MANA_IMAGES.put("trample", FSkin.getImage(FSkinProp.IMG_ABILITY_TRAMPLE));
        MANA_IMAGES.put("vigilance", FSkin.getImage(FSkinProp.IMG_ABILITY_VIGILANCE));
        //hexproof from
        MANA_IMAGES.put("hexproofR", FSkin.getImage(FSkinProp.IMG_ABILITY_HEXPROOF_R));
        MANA_IMAGES.put("hexproofG", FSkin.getImage(FSkinProp.IMG_ABILITY_HEXPROOF_G));
        MANA_IMAGES.put("hexproofB", FSkin.getImage(FSkinProp.IMG_ABILITY_HEXPROOF_B));
        MANA_IMAGES.put("hexproofU", FSkin.getImage(FSkinProp.IMG_ABILITY_HEXPROOF_U));
        MANA_IMAGES.put("hexproofW", FSkin.getImage(FSkinProp.IMG_ABILITY_HEXPROOF_W));
        MANA_IMAGES.put("hexproofC", FSkin.getImage(FSkinProp.IMG_ABILITY_HEXPROOF_C));
        MANA_IMAGES.put("hexproofUB", FSkin.getImage(FSkinProp.IMG_ABILITY_HEXPROOF_UB));
        //token icon
        MANA_IMAGES.put("token", FSkin.getImage(FSkinProp.IMG_ABILITY_TOKEN));
        //protection from
        MANA_IMAGES.put("protectAll", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_ALL));
        MANA_IMAGES.put("protectB", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_B));
        MANA_IMAGES.put("protectBU", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_BU));
        MANA_IMAGES.put("protectBW", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_BW));
        MANA_IMAGES.put("protectColoredSpells", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_COLOREDSPELLS));
        MANA_IMAGES.put("protectG", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_G));
        MANA_IMAGES.put("protectGB", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_GB));
        MANA_IMAGES.put("protectGU", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_GU));
        MANA_IMAGES.put("protectGW", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_GW));
        MANA_IMAGES.put("protectGeneric", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_GENERIC));
        MANA_IMAGES.put("protectR", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_R));
        MANA_IMAGES.put("protectRB", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_RB));
        MANA_IMAGES.put("protectRG", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_RG));
        MANA_IMAGES.put("protectRU", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_RU));
        MANA_IMAGES.put("protectRW", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_RW));
        MANA_IMAGES.put("protectU", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_U));
        MANA_IMAGES.put("protectUW", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_UW));
        MANA_IMAGES.put("protectW", FSkin.getImage(FSkinProp.IMG_ABILITY_PROTECT_W));
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
                    CardFaceSymbols.drawSymbol(s.getImageKey(), g, xpos, y);
                    xpos += offset;
                }
            }

            final String sGeneric = Integer.toString(genericManaCost);
            CardFaceSymbols.drawSymbol(sGeneric, g, xpos, y);
            xpos += offset;

            for (final ManaCostShard s : manaCost) { //render non-X shards after generic
                if (s != ManaCostShard.X) {
                    CardFaceSymbols.drawSymbol(s.getImageKey(), g, xpos, y);
                    xpos += offset;
                }
            }
        }
        else { //if no generic, just render shards in order
            for (final ManaCostShard s : manaCost) {
                CardFaceSymbols.drawSymbol(s.getImageKey(), g, xpos, y);
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
    public static void drawOther(final Graphics g, final String s, int x, final int y, final int w, final int h) {
        if (s.isEmpty()) {
            return;
        }

        final StringTokenizer tok = new StringTokenizer(s, " ");
        while (tok.hasMoreTokens()) {
            final String symbol = tok.nextToken();
            final SkinImage image = MANA_IMAGES.get(symbol);
            if (image == null) {
                Log.info("Symbol not recognized \"" + symbol + "\" in string: " + s);
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
    public static void drawAbilitySymbol(final String imageName, final Graphics g, final int x, final int y, final int w, final int h) {
        FSkin.drawImage(g, MANA_IMAGES.get(imageName), x, y, w, h);
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
