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
package forge.card;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import forge.Graphics;
import forge.assets.FSkinImage;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.gui.error.BugReporter;


public class CardFaceSymbols {
    public static final float FONT_SIZE_FACTOR = 0.85f;
    private static final Map<String, FSkinImage> MANA_IMAGES = new HashMap<>(128);

    public static void loadImages() {
        for (int i = 0; i <= 20; i++) {
            MANA_IMAGES.put(String.valueOf(i), FSkinImage.valueOf("MANA_" + i));
        }
        MANA_IMAGES.put("X", FSkinImage.MANA_X);
        MANA_IMAGES.put("Y", FSkinImage.MANA_Y);
        MANA_IMAGES.put("Z", FSkinImage.MANA_Z);

        MANA_IMAGES.put("C", FSkinImage.MANA_COLORLESS);
        MANA_IMAGES.put("B", FSkinImage.MANA_B);
        MANA_IMAGES.put("BG", FSkinImage.MANA_HYBRID_BG);
        MANA_IMAGES.put("BR", FSkinImage.MANA_HYBRID_BR);
        MANA_IMAGES.put("G", FSkinImage.MANA_G);
        MANA_IMAGES.put("GU", FSkinImage.MANA_HYBRID_GU);
        MANA_IMAGES.put("GW", FSkinImage.MANA_HYBRID_GW);
        MANA_IMAGES.put("R", FSkinImage.MANA_R);
        MANA_IMAGES.put("RG", FSkinImage.MANA_HYBRID_RG);
        MANA_IMAGES.put("RW", FSkinImage.MANA_HYBRID_RW);
        MANA_IMAGES.put("U", FSkinImage.MANA_U);
        MANA_IMAGES.put("UB", FSkinImage.MANA_HYBRID_UB);
        MANA_IMAGES.put("UR", FSkinImage.MANA_HYBRID_UR);
        MANA_IMAGES.put("W", FSkinImage.MANA_W);
        MANA_IMAGES.put("WB", FSkinImage.MANA_HYBRID_WB);
        MANA_IMAGES.put("WU", FSkinImage.MANA_HYBRID_WU);
        MANA_IMAGES.put("P", FSkinImage.MANA_PHRYX);
        MANA_IMAGES.put("PW", FSkinImage.MANA_PHRYX_W);
        MANA_IMAGES.put("PR", FSkinImage.MANA_PHRYX_R);
        MANA_IMAGES.put("PU", FSkinImage.MANA_PHRYX_U);
        MANA_IMAGES.put("PB", FSkinImage.MANA_PHRYX_B);
        MANA_IMAGES.put("PG", FSkinImage.MANA_PHRYX_G);
        MANA_IMAGES.put("2W", FSkinImage.MANA_2W);
        MANA_IMAGES.put("2U", FSkinImage.MANA_2U);
        MANA_IMAGES.put("2R", FSkinImage.MANA_2R);
        MANA_IMAGES.put("2G", FSkinImage.MANA_2G);
        MANA_IMAGES.put("2B", FSkinImage.MANA_2B);

        MANA_IMAGES.put("S", FSkinImage.MANA_SNOW);
        MANA_IMAGES.put("T", FSkinImage.TAP);
        MANA_IMAGES.put("E", FSkinImage.ENERGY);
        MANA_IMAGES.put("slash", FSkinImage.SLASH);
        MANA_IMAGES.put("attack", FSkinImage.ATTACK);
        MANA_IMAGES.put("defend", FSkinImage.DEFEND);
        MANA_IMAGES.put("summonsick", FSkinImage.SUMMONSICK);
        MANA_IMAGES.put("phasing", FSkinImage.PHASING);
        MANA_IMAGES.put("sacrifice", FSkinImage.COSTRESERVED);
        MANA_IMAGES.put("counters1", FSkinImage.COUNTERS1);
        MANA_IMAGES.put("counters2", FSkinImage.COUNTERS2);
        MANA_IMAGES.put("counters3", FSkinImage.COUNTERS3);
        MANA_IMAGES.put("countersMulti", FSkinImage.COUNTERS_MULTI);

        MANA_IMAGES.put("foil01", FSkinImage.FOIL_01);
        MANA_IMAGES.put("foil02", FSkinImage.FOIL_02);
        MANA_IMAGES.put("foil03", FSkinImage.FOIL_03);
        MANA_IMAGES.put("foil04", FSkinImage.FOIL_04);
        MANA_IMAGES.put("foil05", FSkinImage.FOIL_05);
        MANA_IMAGES.put("foil06", FSkinImage.FOIL_06);
        MANA_IMAGES.put("foil07", FSkinImage.FOIL_07);
        MANA_IMAGES.put("foil08", FSkinImage.FOIL_08);
        MANA_IMAGES.put("foil09", FSkinImage.FOIL_09);
        MANA_IMAGES.put("foil10", FSkinImage.FOIL_10);

        MANA_IMAGES.put("foil11", FSkinImage.FOIL_11);
        MANA_IMAGES.put("foil12", FSkinImage.FOIL_12);
        MANA_IMAGES.put("foil13", FSkinImage.FOIL_13);
        MANA_IMAGES.put("foil14", FSkinImage.FOIL_14);
        MANA_IMAGES.put("foil15", FSkinImage.FOIL_15);
        MANA_IMAGES.put("foil16", FSkinImage.FOIL_16);
        MANA_IMAGES.put("foil17", FSkinImage.FOIL_17);
        MANA_IMAGES.put("foil18", FSkinImage.FOIL_18);
        MANA_IMAGES.put("foil19", FSkinImage.FOIL_19);
        MANA_IMAGES.put("foil20", FSkinImage.FOIL_20);

        MANA_IMAGES.put("commander", FSkinImage.IMG_ABILITY_COMMANDER);

        MANA_IMAGES.put("deathtouch", FSkinImage.IMG_ABILITY_DEATHTOUCH);
        MANA_IMAGES.put("defender", FSkinImage.IMG_ABILITY_DEFENDER);
        MANA_IMAGES.put("doublestrike", FSkinImage.IMG_ABILITY_DOUBLE_STRIKE);
        MANA_IMAGES.put("firststrike", FSkinImage.IMG_ABILITY_FIRST_STRIKE);
        MANA_IMAGES.put("fear", FSkinImage.IMG_ABILITY_FEAR);
        MANA_IMAGES.put("flash", FSkinImage.IMG_ABILITY_FLASH);
        MANA_IMAGES.put("flying", FSkinImage.IMG_ABILITY_FLYING);
        MANA_IMAGES.put("haste", FSkinImage.IMG_ABILITY_HASTE);
        MANA_IMAGES.put("hexproof", FSkinImage.IMG_ABILITY_HEXPROOF);
        MANA_IMAGES.put("horsemanship", FSkinImage.IMG_ABILITY_HORSEMANSHIP);
        MANA_IMAGES.put("indestructible", FSkinImage.IMG_ABILITY_INDESTRUCTIBLE);
        MANA_IMAGES.put("intimidate", FSkinImage.IMG_ABILITY_INTIMIDATE);
        MANA_IMAGES.put("landwalk", FSkinImage.IMG_ABILITY_LANDWALK);
        MANA_IMAGES.put("lifelink", FSkinImage.IMG_ABILITY_LIFELINK);
        MANA_IMAGES.put("menace", FSkinImage.IMG_ABILITY_MENACE);
        MANA_IMAGES.put("reach", FSkinImage.IMG_ABILITY_REACH);
        MANA_IMAGES.put("shadow", FSkinImage.IMG_ABILITY_SHADOW);
        MANA_IMAGES.put("shroud", FSkinImage.IMG_ABILITY_SHROUD);
        MANA_IMAGES.put("trample", FSkinImage.IMG_ABILITY_TRAMPLE);
        MANA_IMAGES.put("vigilance", FSkinImage.IMG_ABILITY_VIGILANCE);
        //hexproof from
        MANA_IMAGES.put("hexproofR", FSkinImage.IMG_ABILITY_HEXPROOF_R);
        MANA_IMAGES.put("hexproofG", FSkinImage.IMG_ABILITY_HEXPROOF_G);
        MANA_IMAGES.put("hexproofB", FSkinImage.IMG_ABILITY_HEXPROOF_B);
        MANA_IMAGES.put("hexproofU", FSkinImage.IMG_ABILITY_HEXPROOF_U);
        MANA_IMAGES.put("hexproofW", FSkinImage.IMG_ABILITY_HEXPROOF_W);
        MANA_IMAGES.put("hexproofC", FSkinImage.IMG_ABILITY_HEXPROOF_C);
        MANA_IMAGES.put("hexproofUB", FSkinImage.IMG_ABILITY_HEXPROOF_UB);
        //token icon
        MANA_IMAGES.put("token", FSkinImage.IMG_ABILITY_TOKEN);
        //protection from
        MANA_IMAGES.put("protectAll", FSkinImage.IMG_ABILITY_PROTECT_ALL);
        MANA_IMAGES.put("protectB", FSkinImage.IMG_ABILITY_PROTECT_B);
        MANA_IMAGES.put("protectBU", FSkinImage.IMG_ABILITY_PROTECT_BU);
        MANA_IMAGES.put("protectBW", FSkinImage.IMG_ABILITY_PROTECT_BW);
        MANA_IMAGES.put("protectColoredSpells", FSkinImage.IMG_ABILITY_PROTECT_COLOREDSPELLS);
        MANA_IMAGES.put("protectG", FSkinImage.IMG_ABILITY_PROTECT_G);
        MANA_IMAGES.put("protectGB", FSkinImage.IMG_ABILITY_PROTECT_GB);
        MANA_IMAGES.put("protectGU", FSkinImage.IMG_ABILITY_PROTECT_GU);
        MANA_IMAGES.put("protectGW", FSkinImage.IMG_ABILITY_PROTECT_GW);
        MANA_IMAGES.put("protectGeneric", FSkinImage.IMG_ABILITY_PROTECT_GENERIC);
        MANA_IMAGES.put("protectR", FSkinImage.IMG_ABILITY_PROTECT_R);
        MANA_IMAGES.put("protectRB", FSkinImage.IMG_ABILITY_PROTECT_RB);
        MANA_IMAGES.put("protectRG", FSkinImage.IMG_ABILITY_PROTECT_RG);
        MANA_IMAGES.put("protectRU", FSkinImage.IMG_ABILITY_PROTECT_RU);
        MANA_IMAGES.put("protectRW", FSkinImage.IMG_ABILITY_PROTECT_RW);
        MANA_IMAGES.put("protectU", FSkinImage.IMG_ABILITY_PROTECT_U);
        MANA_IMAGES.put("protectUW", FSkinImage.IMG_ABILITY_PROTECT_UW);
        MANA_IMAGES.put("protectW", FSkinImage.IMG_ABILITY_PROTECT_W);
    }

    public static void drawManaCost(Graphics g, ManaCost manaCost, float x, float y, final float imageSize) {
        if (manaCost.isNoCost()) {
            return;
        }

        final int genericManaCost = manaCost.getGenericCost();
        final boolean hasGeneric = (genericManaCost > 0) || manaCost.isPureGeneric();
        final float dx = imageSize;

        if (hasGeneric) {
            for (final ManaCostShard s : manaCost) { //render X shards before generic
                if (s == ManaCostShard.X) {
                    drawSymbol(s.getImageKey(), g, x, y, imageSize, imageSize);
                    x += dx;
                }
            }

            final String sGeneric = Integer.toString(genericManaCost);
            drawSymbol(sGeneric, g, x, y, imageSize, imageSize);
            x += dx;
    
            for (final ManaCostShard s : manaCost) { //render non-X shards after generic
                if (s != ManaCostShard.X) {
                    drawSymbol(s.getImageKey(), g, x, y, imageSize, imageSize);
                    x += dx;
                }
            }
        }
        else { //if no generic, just render shards in order
            for (final ManaCostShard s : manaCost) {
                drawSymbol(s.getImageKey(), g, x, y, imageSize, imageSize);
                x += dx;
            }
        }
    }

    public static void drawColorSet(Graphics g, ColorSet colorSet, float x, float y, final float imageSize) {
        drawColorSet(g, colorSet, x, y, imageSize, false);
    }
    public static void drawColorSet(Graphics g, ColorSet colorSet, float x, float y, final float imageSize, boolean vertical) {
        final float dx = imageSize;

        for (final ManaCostShard s : colorSet.getOrderedShards()) {
            drawSymbol(s.getImageKey(), g, x, y, imageSize, imageSize);
            if (!vertical)
                x += dx;
            else
                y += dx;
        }
    }

    public static void drawOther(final Graphics g, String s, float x, final float y, final float w, final float h, boolean rotate) {
        if (s.length() == 0) {
            return;
        }

        final float dx = w;

        StringTokenizer tok = new StringTokenizer(s, " ");
        while (tok.hasMoreTokens()) {
            String symbol = tok.nextToken();
            FSkinImage image = MANA_IMAGES.get(symbol);
            if (image == null) {
                BugReporter.reportBug("Symbol not recognized \"" + symbol + "\" in string: " + s);
                continue;
            }

            if(rotate) {
                g.drawRotatedImage(image.getTextureRegion(), x, y, w, h, x+w /2, y+h /2,90);
            }
            else
                g.drawImage(image, x, y, w, h);

            x += dx;
        }
    }

    public static void drawSymbol(final String imageName, final Graphics g, final float x, final float y, final float w, final float h) {
        g.drawImage(MANA_IMAGES.get(imageName), x, y, w, h);
    }

    public static float getWidth(final ManaCost manaCost, float imageSize) {
        return manaCost.getGlyphCount() * imageSize;
    }

    public static float getWidth(final ColorSet colorSet, float imageSize) {
        return Math.max(colorSet.countColors(), 1) * imageSize;
    }
}
