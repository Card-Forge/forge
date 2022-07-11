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

import java.util.StringTokenizer;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.gui.error.BugReporter;


public class CardFaceSymbols {
    public static final float FONT_SIZE_FACTOR = 0.85f;

    public static void loadImages() {
        for (int i = 0; i <= 20; i++) {
            Forge.getAssets().MANA_IMAGES.put(String.valueOf(i), FSkinImage.valueOf("MANA_" + i));
        }
        Forge.getAssets().MANA_IMAGES.put("X", FSkinImage.MANA_X);
        Forge.getAssets().MANA_IMAGES.put("Y", FSkinImage.MANA_Y);
        Forge.getAssets().MANA_IMAGES.put("Z", FSkinImage.MANA_Z);

        Forge.getAssets().MANA_IMAGES.put("C", FSkinImage.MANA_COLORLESS);
        Forge.getAssets().MANA_IMAGES.put("B", FSkinImage.MANA_B);
        Forge.getAssets().MANA_IMAGES.put("BG", FSkinImage.MANA_HYBRID_BG);
        Forge.getAssets().MANA_IMAGES.put("BR", FSkinImage.MANA_HYBRID_BR);
        Forge.getAssets().MANA_IMAGES.put("G", FSkinImage.MANA_G);
        Forge.getAssets().MANA_IMAGES.put("GU", FSkinImage.MANA_HYBRID_GU);
        Forge.getAssets().MANA_IMAGES.put("GW", FSkinImage.MANA_HYBRID_GW);
        Forge.getAssets().MANA_IMAGES.put("R", FSkinImage.MANA_R);
        Forge.getAssets().MANA_IMAGES.put("RG", FSkinImage.MANA_HYBRID_RG);
        Forge.getAssets().MANA_IMAGES.put("RW", FSkinImage.MANA_HYBRID_RW);
        Forge.getAssets().MANA_IMAGES.put("U", FSkinImage.MANA_U);
        Forge.getAssets().MANA_IMAGES.put("UB", FSkinImage.MANA_HYBRID_UB);
        Forge.getAssets().MANA_IMAGES.put("UR", FSkinImage.MANA_HYBRID_UR);
        Forge.getAssets().MANA_IMAGES.put("W", FSkinImage.MANA_W);
        Forge.getAssets().MANA_IMAGES.put("WB", FSkinImage.MANA_HYBRID_WB);
        Forge.getAssets().MANA_IMAGES.put("WU", FSkinImage.MANA_HYBRID_WU);
        Forge.getAssets().MANA_IMAGES.put("P", FSkinImage.MANA_PHRYX);
        Forge.getAssets().MANA_IMAGES.put("PW", FSkinImage.MANA_PHRYX_W);
        Forge.getAssets().MANA_IMAGES.put("PR", FSkinImage.MANA_PHRYX_R);
        Forge.getAssets().MANA_IMAGES.put("PU", FSkinImage.MANA_PHRYX_U);
        Forge.getAssets().MANA_IMAGES.put("PB", FSkinImage.MANA_PHRYX_B);
        Forge.getAssets().MANA_IMAGES.put("PG", FSkinImage.MANA_PHRYX_G);
        Forge.getAssets().MANA_IMAGES.put("PBG", FSkinImage.MANA_PHRYX_BG);
        Forge.getAssets().MANA_IMAGES.put("PBR", FSkinImage.MANA_PHRYX_BR);
        Forge.getAssets().MANA_IMAGES.put("PGU", FSkinImage.MANA_PHRYX_GU);
        Forge.getAssets().MANA_IMAGES.put("PGW", FSkinImage.MANA_PHRYX_GW);
        Forge.getAssets().MANA_IMAGES.put("PRG", FSkinImage.MANA_PHRYX_RG);
        Forge.getAssets().MANA_IMAGES.put("PRW", FSkinImage.MANA_PHRYX_RW);
        Forge.getAssets().MANA_IMAGES.put("PUB", FSkinImage.MANA_PHRYX_UB);
        Forge.getAssets().MANA_IMAGES.put("PUR", FSkinImage.MANA_PHRYX_UR);
        Forge.getAssets().MANA_IMAGES.put("PWB", FSkinImage.MANA_PHRYX_WB);
        Forge.getAssets().MANA_IMAGES.put("PWU", FSkinImage.MANA_PHRYX_WU);
        Forge.getAssets().MANA_IMAGES.put("2W", FSkinImage.MANA_2W);
        Forge.getAssets().MANA_IMAGES.put("2U", FSkinImage.MANA_2U);
        Forge.getAssets().MANA_IMAGES.put("2R", FSkinImage.MANA_2R);
        Forge.getAssets().MANA_IMAGES.put("2G", FSkinImage.MANA_2G);
        Forge.getAssets().MANA_IMAGES.put("2B", FSkinImage.MANA_2B);

        Forge.getAssets().MANA_IMAGES.put("S", FSkinImage.MANA_SNOW);
        Forge.getAssets().MANA_IMAGES.put("T", FSkinImage.TAP);
        Forge.getAssets().MANA_IMAGES.put("E", FSkinImage.ENERGY);
        Forge.getAssets().MANA_IMAGES.put("slash", FSkinImage.SLASH);
        Forge.getAssets().MANA_IMAGES.put("attack", FSkinImage.ATTACK);
        Forge.getAssets().MANA_IMAGES.put("defend", FSkinImage.DEFEND);
        Forge.getAssets().MANA_IMAGES.put("summonsick", FSkinImage.SUMMONSICK);
        Forge.getAssets().MANA_IMAGES.put("phasing", FSkinImage.PHASING);
        Forge.getAssets().MANA_IMAGES.put("sacrifice", FSkinImage.COSTRESERVED);
        Forge.getAssets().MANA_IMAGES.put("counters1", FSkinImage.COUNTERS1);
        Forge.getAssets().MANA_IMAGES.put("counters2", FSkinImage.COUNTERS2);
        Forge.getAssets().MANA_IMAGES.put("counters3", FSkinImage.COUNTERS3);
        Forge.getAssets().MANA_IMAGES.put("countersMulti", FSkinImage.COUNTERS_MULTI);

        Forge.getAssets().MANA_IMAGES.put("foil01", FSkinImage.FOIL_01);
        Forge.getAssets().MANA_IMAGES.put("foil02", FSkinImage.FOIL_02);
        Forge.getAssets().MANA_IMAGES.put("foil03", FSkinImage.FOIL_03);
        Forge.getAssets().MANA_IMAGES.put("foil04", FSkinImage.FOIL_04);
        Forge.getAssets().MANA_IMAGES.put("foil05", FSkinImage.FOIL_05);
        Forge.getAssets().MANA_IMAGES.put("foil06", FSkinImage.FOIL_06);
        Forge.getAssets().MANA_IMAGES.put("foil07", FSkinImage.FOIL_07);
        Forge.getAssets().MANA_IMAGES.put("foil08", FSkinImage.FOIL_08);
        Forge.getAssets().MANA_IMAGES.put("foil09", FSkinImage.FOIL_09);
        Forge.getAssets().MANA_IMAGES.put("foil10", FSkinImage.FOIL_10);

        Forge.getAssets().MANA_IMAGES.put("foil11", FSkinImage.FOIL_11);
        Forge.getAssets().MANA_IMAGES.put("foil12", FSkinImage.FOIL_12);
        Forge.getAssets().MANA_IMAGES.put("foil13", FSkinImage.FOIL_13);
        Forge.getAssets().MANA_IMAGES.put("foil14", FSkinImage.FOIL_14);
        Forge.getAssets().MANA_IMAGES.put("foil15", FSkinImage.FOIL_15);
        Forge.getAssets().MANA_IMAGES.put("foil16", FSkinImage.FOIL_16);
        Forge.getAssets().MANA_IMAGES.put("foil17", FSkinImage.FOIL_17);
        Forge.getAssets().MANA_IMAGES.put("foil18", FSkinImage.FOIL_18);
        Forge.getAssets().MANA_IMAGES.put("foil19", FSkinImage.FOIL_19);
        Forge.getAssets().MANA_IMAGES.put("foil20", FSkinImage.FOIL_20);

        Forge.getAssets().MANA_IMAGES.put("commander", FSkinImage.IMG_ABILITY_COMMANDER);

        Forge.getAssets().MANA_IMAGES.put("deathtouch", FSkinImage.IMG_ABILITY_DEATHTOUCH);
        Forge.getAssets().MANA_IMAGES.put("defender", FSkinImage.IMG_ABILITY_DEFENDER);
        Forge.getAssets().MANA_IMAGES.put("doublestrike", FSkinImage.IMG_ABILITY_DOUBLE_STRIKE);
        Forge.getAssets().MANA_IMAGES.put("firststrike", FSkinImage.IMG_ABILITY_FIRST_STRIKE);
        Forge.getAssets().MANA_IMAGES.put("fear", FSkinImage.IMG_ABILITY_FEAR);
        Forge.getAssets().MANA_IMAGES.put("flash", FSkinImage.IMG_ABILITY_FLASH);
        Forge.getAssets().MANA_IMAGES.put("flying", FSkinImage.IMG_ABILITY_FLYING);
        Forge.getAssets().MANA_IMAGES.put("haste", FSkinImage.IMG_ABILITY_HASTE);
        Forge.getAssets().MANA_IMAGES.put("hexproof", FSkinImage.IMG_ABILITY_HEXPROOF);
        Forge.getAssets().MANA_IMAGES.put("horsemanship", FSkinImage.IMG_ABILITY_HORSEMANSHIP);
        Forge.getAssets().MANA_IMAGES.put("indestructible", FSkinImage.IMG_ABILITY_INDESTRUCTIBLE);
        Forge.getAssets().MANA_IMAGES.put("intimidate", FSkinImage.IMG_ABILITY_INTIMIDATE);
        Forge.getAssets().MANA_IMAGES.put("landwalk", FSkinImage.IMG_ABILITY_LANDWALK);
        Forge.getAssets().MANA_IMAGES.put("lifelink", FSkinImage.IMG_ABILITY_LIFELINK);
        Forge.getAssets().MANA_IMAGES.put("menace", FSkinImage.IMG_ABILITY_MENACE);
        Forge.getAssets().MANA_IMAGES.put("reach", FSkinImage.IMG_ABILITY_REACH);
        Forge.getAssets().MANA_IMAGES.put("shadow", FSkinImage.IMG_ABILITY_SHADOW);
        Forge.getAssets().MANA_IMAGES.put("shroud", FSkinImage.IMG_ABILITY_SHROUD);
        Forge.getAssets().MANA_IMAGES.put("trample", FSkinImage.IMG_ABILITY_TRAMPLE);
        Forge.getAssets().MANA_IMAGES.put("vigilance", FSkinImage.IMG_ABILITY_VIGILANCE);
        //hexproof from
        Forge.getAssets().MANA_IMAGES.put("hexproofR", FSkinImage.IMG_ABILITY_HEXPROOF_R);
        Forge.getAssets().MANA_IMAGES.put("hexproofG", FSkinImage.IMG_ABILITY_HEXPROOF_G);
        Forge.getAssets().MANA_IMAGES.put("hexproofB", FSkinImage.IMG_ABILITY_HEXPROOF_B);
        Forge.getAssets().MANA_IMAGES.put("hexproofU", FSkinImage.IMG_ABILITY_HEXPROOF_U);
        Forge.getAssets().MANA_IMAGES.put("hexproofW", FSkinImage.IMG_ABILITY_HEXPROOF_W);
        Forge.getAssets().MANA_IMAGES.put("hexproofC", FSkinImage.IMG_ABILITY_HEXPROOF_C);
        Forge.getAssets().MANA_IMAGES.put("hexproofUB", FSkinImage.IMG_ABILITY_HEXPROOF_UB);
        //token icon
        Forge.getAssets().MANA_IMAGES.put("token", FSkinImage.IMG_ABILITY_TOKEN);
        //protection from
        Forge.getAssets().MANA_IMAGES.put("protectAll", FSkinImage.IMG_ABILITY_PROTECT_ALL);
        Forge.getAssets().MANA_IMAGES.put("protectB", FSkinImage.IMG_ABILITY_PROTECT_B);
        Forge.getAssets().MANA_IMAGES.put("protectBU", FSkinImage.IMG_ABILITY_PROTECT_BU);
        Forge.getAssets().MANA_IMAGES.put("protectBW", FSkinImage.IMG_ABILITY_PROTECT_BW);
        Forge.getAssets().MANA_IMAGES.put("protectColoredSpells", FSkinImage.IMG_ABILITY_PROTECT_COLOREDSPELLS);
        Forge.getAssets().MANA_IMAGES.put("protectG", FSkinImage.IMG_ABILITY_PROTECT_G);
        Forge.getAssets().MANA_IMAGES.put("protectGB", FSkinImage.IMG_ABILITY_PROTECT_GB);
        Forge.getAssets().MANA_IMAGES.put("protectGU", FSkinImage.IMG_ABILITY_PROTECT_GU);
        Forge.getAssets().MANA_IMAGES.put("protectGW", FSkinImage.IMG_ABILITY_PROTECT_GW);
        Forge.getAssets().MANA_IMAGES.put("protectGeneric", FSkinImage.IMG_ABILITY_PROTECT_GENERIC);
        Forge.getAssets().MANA_IMAGES.put("protectR", FSkinImage.IMG_ABILITY_PROTECT_R);
        Forge.getAssets().MANA_IMAGES.put("protectRB", FSkinImage.IMG_ABILITY_PROTECT_RB);
        Forge.getAssets().MANA_IMAGES.put("protectRG", FSkinImage.IMG_ABILITY_PROTECT_RG);
        Forge.getAssets().MANA_IMAGES.put("protectRU", FSkinImage.IMG_ABILITY_PROTECT_RU);
        Forge.getAssets().MANA_IMAGES.put("protectRW", FSkinImage.IMG_ABILITY_PROTECT_RW);
        Forge.getAssets().MANA_IMAGES.put("protectU", FSkinImage.IMG_ABILITY_PROTECT_U);
        Forge.getAssets().MANA_IMAGES.put("protectUW", FSkinImage.IMG_ABILITY_PROTECT_UW);
        Forge.getAssets().MANA_IMAGES.put("protectW", FSkinImage.IMG_ABILITY_PROTECT_W);
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
            FSkinImage image = Forge.getAssets().MANA_IMAGES.get(symbol);
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
        g.drawImage(Forge.getAssets().MANA_IMAGES.get(imageName), x, y, w, h);
    }

    public static float getWidth(final ManaCost manaCost, float imageSize) {
        return manaCost.getGlyphCount() * imageSize;
    }

    public static float getWidth(final ColorSet colorSet, float imageSize) {
        return Math.max(colorSet.countColors(), 1) * imageSize;
    }
}
