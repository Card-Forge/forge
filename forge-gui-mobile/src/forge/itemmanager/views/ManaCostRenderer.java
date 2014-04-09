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
package forge.itemmanager.views;

import forge.Forge.Graphics;
import forge.assets.CardFaceSymbols;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;

import com.badlogic.gdx.math.Vector2;

/**
 * Displays mana cost as symbols.
 */
public class ManaCostRenderer extends ItemCellRenderer {
    private static final float elemtWidth = 13;
    private static final float elemtGap = 0;
    private static final float padding0 = 2;
    private static final float spaceBetweenSplitCosts = 3;
    
    private ManaCost v1, v2;

    @Override
    public void draw(Graphics g, Object value, Vector2 loc, float itemWidth, float itemHeight) {
        v2 = null;
        if (value instanceof CardRules) {
            CardRules v = (CardRules) value;
            v1 = v.getMainPart().getManaCost();
            v2 = v.getSplitType() == CardSplitType.Split ? v.getOtherPart().getManaCost() : null;
        }
        else if (value instanceof ManaCost) {
            v1 = (ManaCost) value;
        }
        else {
            v1 = ManaCost.NO_COST;
        }

        final float cellWidth = itemWidth;

        if (v2 == null) {
            drawCost(g, v1, padding0, cellWidth);
        }
        else {
            float shards1 = v1.getGlyphCount();
            float shards2 = v2.getGlyphCount();

            float perGlyph = (cellWidth - padding0 - spaceBetweenSplitCosts) / (shards1 + shards2);
            perGlyph = Math.min(perGlyph, elemtWidth + elemtGap);
            drawCost(g, v1, padding0, padding0 + perGlyph * shards1);
            drawCost(g, v2, cellWidth - perGlyph * shards2 - padding0, cellWidth);
        }
    }

    private void drawCost(final Graphics g, ManaCost value, final float padding, final float cellWidth) {
        float x = padding;
        float y = padding0 + 1;
        final int genericManaCost = value.getGenericCost();
        final float xManaCosts = value.countX();
        final boolean hasGeneric = (genericManaCost > 0) || v1.isPureGeneric();

        final int cntGlyphs = value.getGlyphCount();
        final float offsetIfNoSpace = cntGlyphs > 1 ? (cellWidth - padding - elemtWidth) / (cntGlyphs - 1)
                : elemtWidth + elemtGap;
        final float dx = Math.min(elemtWidth + elemtGap, offsetIfNoSpace);

        // Display X Mana before any other type of mana
        if (xManaCosts > 0) {
            for (int i = 0; i < xManaCosts; i++) {
                CardFaceSymbols.drawSymbol(ManaCostShard.X.getImageKey(), g, x, y, elemtWidth);
                x += dx;
            }
        }

        // Display colorless mana before colored mana
        if (hasGeneric) {
            final String sGeneric = Integer.toString(genericManaCost);
            CardFaceSymbols.drawSymbol(sGeneric, g, x, y, elemtWidth);
            x += dx;
        }

        for (final ManaCostShard s : value) {
            if (s.equals(ManaCostShard.X)) {
                // X costs already drawn up above
                continue;
            }
            CardFaceSymbols.drawSymbol(s.getImageKey(), g, x, y, elemtWidth);
            x += dx;
        }
    }
}
