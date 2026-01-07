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

import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.assets.FSkinImageInterface;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.gui.error.BugReporter;
import forge.localinstance.skin.FSkinProp;


public class CardFaceSymbols {
    public static final float FONT_SIZE_FACTOR = 0.85f;

    public static void loadImages() {
        for (Map.Entry<String, FSkinProp> e : FSkinProp.MANA_IMG.entrySet()) {
            Forge.getAssets().manaImages().put(e.getKey(), Forge.getAssets().images().get(e.getValue()));
        }

        Forge.getAssets().manaImages().put("E", FSkinImage.ENERGY);
        Forge.getAssets().manaImages().put("TK", FSkinImage.TICKET);
        Forge.getAssets().manaImages().put("slash", FSkinImage.SLASH);
        Forge.getAssets().manaImages().put("attack", FSkinImage.ATTACK);
        Forge.getAssets().manaImages().put("defend", FSkinImage.DEFEND);
        Forge.getAssets().manaImages().put("summonsick", FSkinImage.SUMMONSICK);
        Forge.getAssets().manaImages().put("phasing", FSkinImage.PHASING);
        Forge.getAssets().manaImages().put("sacrifice", FSkinImage.COSTRESERVED);
        Forge.getAssets().manaImages().put("counters1", FSkinImage.COUNTERS1);
        Forge.getAssets().manaImages().put("counters2", FSkinImage.COUNTERS2);
        Forge.getAssets().manaImages().put("counters3", FSkinImage.COUNTERS3);
        Forge.getAssets().manaImages().put("countersMulti", FSkinImage.COUNTERS_MULTI);

        Forge.getAssets().manaImages().put("foil01", FSkinImage.FOIL_01);
        Forge.getAssets().manaImages().put("foil02", FSkinImage.FOIL_02);
        Forge.getAssets().manaImages().put("foil03", FSkinImage.FOIL_03);
        Forge.getAssets().manaImages().put("foil04", FSkinImage.FOIL_04);
        Forge.getAssets().manaImages().put("foil05", FSkinImage.FOIL_05);
        Forge.getAssets().manaImages().put("foil06", FSkinImage.FOIL_06);
        Forge.getAssets().manaImages().put("foil07", FSkinImage.FOIL_07);
        Forge.getAssets().manaImages().put("foil08", FSkinImage.FOIL_08);
        Forge.getAssets().manaImages().put("foil09", FSkinImage.FOIL_09);
        Forge.getAssets().manaImages().put("foil10", FSkinImage.FOIL_10);

        Forge.getAssets().manaImages().put("foil11", FSkinImage.FOIL_11);
        Forge.getAssets().manaImages().put("foil12", FSkinImage.FOIL_12);
        Forge.getAssets().manaImages().put("foil13", FSkinImage.FOIL_13);
        Forge.getAssets().manaImages().put("foil14", FSkinImage.FOIL_14);
        Forge.getAssets().manaImages().put("foil15", FSkinImage.FOIL_15);
        Forge.getAssets().manaImages().put("foil16", FSkinImage.FOIL_16);
        Forge.getAssets().manaImages().put("foil17", FSkinImage.FOIL_17);
        Forge.getAssets().manaImages().put("foil18", FSkinImage.FOIL_18);
        Forge.getAssets().manaImages().put("foil19", FSkinImage.FOIL_19);
        Forge.getAssets().manaImages().put("foil20", FSkinImage.FOIL_20);

        Forge.getAssets().manaImages().put("commander", FSkinImage.IMG_ABILITY_COMMANDER);
        Forge.getAssets().manaImages().put("ringbearer", FSkinImage.IMG_ABILITY_RINGBEARER);
        Forge.getAssets().manaImages().put("annihilator", FSkinImage.IMG_ABILITY_ANNIHILATOR);
        Forge.getAssets().manaImages().put("toxic", FSkinImage.IMG_ABILITY_TOXIC);
        Forge.getAssets().manaImages().put("deathtouch", FSkinImage.IMG_ABILITY_DEATHTOUCH);
        Forge.getAssets().manaImages().put("defender", FSkinImage.IMG_ABILITY_DEFENDER);
        Forge.getAssets().manaImages().put("doublestrike", FSkinImage.IMG_ABILITY_DOUBLE_STRIKE);
        Forge.getAssets().manaImages().put("exalted", FSkinImage.IMG_ABILITY_EXALTED);
        Forge.getAssets().manaImages().put("firststrike", FSkinImage.IMG_ABILITY_FIRST_STRIKE);
        Forge.getAssets().manaImages().put("fear", FSkinImage.IMG_ABILITY_FEAR);
        Forge.getAssets().manaImages().put("flash", FSkinImage.IMG_ABILITY_FLASH);
        Forge.getAssets().manaImages().put("flying", FSkinImage.IMG_ABILITY_FLYING);
        Forge.getAssets().manaImages().put("haste", FSkinImage.IMG_ABILITY_HASTE);
        Forge.getAssets().manaImages().put("hexproof", FSkinImage.IMG_ABILITY_HEXPROOF);
        Forge.getAssets().manaImages().put("horsemanship", FSkinImage.IMG_ABILITY_HORSEMANSHIP);
        Forge.getAssets().manaImages().put("indestructible", FSkinImage.IMG_ABILITY_INDESTRUCTIBLE);
        Forge.getAssets().manaImages().put("intimidate", FSkinImage.IMG_ABILITY_INTIMIDATE);
        Forge.getAssets().manaImages().put("landwalk", FSkinImage.IMG_ABILITY_LANDWALK);
        Forge.getAssets().manaImages().put("lifelink", FSkinImage.IMG_ABILITY_LIFELINK);
        Forge.getAssets().manaImages().put("menace", FSkinImage.IMG_ABILITY_MENACE);
        Forge.getAssets().manaImages().put("reach", FSkinImage.IMG_ABILITY_REACH);
        Forge.getAssets().manaImages().put("shadow", FSkinImage.IMG_ABILITY_SHADOW);
        Forge.getAssets().manaImages().put("shroud", FSkinImage.IMG_ABILITY_SHROUD);
        Forge.getAssets().manaImages().put("trample", FSkinImage.IMG_ABILITY_TRAMPLE);
        Forge.getAssets().manaImages().put("ward", FSkinImage.IMG_ABILITY_WARD);
        Forge.getAssets().manaImages().put("wither", FSkinImage.IMG_ABILITY_WITHER);
        Forge.getAssets().manaImages().put("vigilance", FSkinImage.IMG_ABILITY_VIGILANCE);
        //hexproof from
        Forge.getAssets().manaImages().put("hexproofR", FSkinImage.IMG_ABILITY_HEXPROOF_R);
        Forge.getAssets().manaImages().put("hexproofG", FSkinImage.IMG_ABILITY_HEXPROOF_G);
        Forge.getAssets().manaImages().put("hexproofB", FSkinImage.IMG_ABILITY_HEXPROOF_B);
        Forge.getAssets().manaImages().put("hexproofU", FSkinImage.IMG_ABILITY_HEXPROOF_U);
        Forge.getAssets().manaImages().put("hexproofW", FSkinImage.IMG_ABILITY_HEXPROOF_W);
        Forge.getAssets().manaImages().put("hexproofC", FSkinImage.IMG_ABILITY_HEXPROOF_C);
        Forge.getAssets().manaImages().put("hexproofUB", FSkinImage.IMG_ABILITY_HEXPROOF_UB);
        //token icon
        Forge.getAssets().manaImages().put("token", FSkinImage.IMG_ABILITY_TOKEN);
        //protection from
        Forge.getAssets().manaImages().put("protectAll", FSkinImage.IMG_ABILITY_PROTECT_ALL);
        Forge.getAssets().manaImages().put("protectB", FSkinImage.IMG_ABILITY_PROTECT_B);
        Forge.getAssets().manaImages().put("protectBU", FSkinImage.IMG_ABILITY_PROTECT_BU);
        Forge.getAssets().manaImages().put("protectBW", FSkinImage.IMG_ABILITY_PROTECT_BW);
        Forge.getAssets().manaImages().put("protectColoredSpells", FSkinImage.IMG_ABILITY_PROTECT_COLOREDSPELLS);
        Forge.getAssets().manaImages().put("protectG", FSkinImage.IMG_ABILITY_PROTECT_G);
        Forge.getAssets().manaImages().put("protectGB", FSkinImage.IMG_ABILITY_PROTECT_GB);
        Forge.getAssets().manaImages().put("protectGU", FSkinImage.IMG_ABILITY_PROTECT_GU);
        Forge.getAssets().manaImages().put("protectGW", FSkinImage.IMG_ABILITY_PROTECT_GW);
        Forge.getAssets().manaImages().put("protectGeneric", FSkinImage.IMG_ABILITY_PROTECT_GENERIC);
        Forge.getAssets().manaImages().put("protectR", FSkinImage.IMG_ABILITY_PROTECT_R);
        Forge.getAssets().manaImages().put("protectRB", FSkinImage.IMG_ABILITY_PROTECT_RB);
        Forge.getAssets().manaImages().put("protectRG", FSkinImage.IMG_ABILITY_PROTECT_RG);
        Forge.getAssets().manaImages().put("protectRU", FSkinImage.IMG_ABILITY_PROTECT_RU);
        Forge.getAssets().manaImages().put("protectRW", FSkinImage.IMG_ABILITY_PROTECT_RW);
        Forge.getAssets().manaImages().put("protectU", FSkinImage.IMG_ABILITY_PROTECT_U);
        Forge.getAssets().manaImages().put("protectUW", FSkinImage.IMG_ABILITY_PROTECT_UW);
        Forge.getAssets().manaImages().put("protectW", FSkinImage.IMG_ABILITY_PROTECT_W);

        // symbol lookup for text render
        for (Map.Entry<String, FSkinProp> e : FSkinProp.MANA_IMG.entrySet()) {
            Forge.getAssets().symbolLookup().put(e.getKey(), Forge.getAssets().images().get(e.getValue()));
        }

        Forge.getAssets().symbolLookup().put("E", FSkinImage.ENERGY);
        Forge.getAssets().symbolLookup().put("TK", FSkinImage.TICKET);
        Forge.getAssets().symbolLookup().put("AE", FSkinImage.AETHER_SHARD);
        Forge.getAssets().symbolLookup().put("PW", FSkinImage.PW_BADGE_COMMON);
        Forge.getAssets().symbolLookup().put("CS", FSkinImage.QUEST_COINSTACK);
        Forge.getAssets().symbolLookup().put("M", FSkinImage.MANASHARD);
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

        for (final MagicColor.Color s : colorSet.getOrderedColors()) {
            drawSymbol(s.getShortName(), g, x, y, imageSize, imageSize);
            if (!vertical)
                x += dx;
            else
                y += dx;
        }
    }

    public static void drawAttractionLights(Graphics g, Set<Integer> lights, float x, float y, final float imageSize, boolean vertical) {
        for(int i = 1; i <= 6; i++) {
            drawSymbol("AL" + i + (lights.contains(i) ? "ON" : "OFF"), g, x, y, imageSize, imageSize);
            if (!vertical)
                x += imageSize;
            else
                y += imageSize;
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
            FSkinImageInterface image = Forge.getAssets().manaImages().get(symbol);
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
        g.drawImage(Forge.getAssets().manaImages().get(imageName), x, y, w, h);
    }

    public static float getWidth(final ManaCost manaCost, float imageSize) {
        return manaCost.getGlyphCount() * imageSize;
    }

    public static float getWidth(final ColorSet colorSet, float imageSize) {
        return Math.max(colorSet.countColors(), 1) * imageSize;
    }
}
