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
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;

import com.badlogic.gdx.math.Vector2;

/**
 * Displays mana cost as symbols.
 */
public class ManaCostRenderer extends ItemCellRenderer {
    private static final float ICON_SIZE = 13;
    private static final float PADDING = 2;
    private static final float SPACE_BETWEEN_SPLIT_COSTS = 3;
    
    private ManaCost v1, v2;

    @Override
    public void draw(Graphics g, Object value, FSkinFont font, FSkinColor foreColor, Vector2 loc, float itemWidth, float itemHeight) {
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

        final float cellWidth = itemWidth - loc.x;

        if (v2 == null) {
            drawCost(g, v1, loc.x + PADDING, loc.y, cellWidth);
        }
        else {
            float shards1 = v1.getGlyphCount();
            float shards2 = v2.getGlyphCount();

            float perGlyph = (cellWidth - PADDING - SPACE_BETWEEN_SPLIT_COSTS) / (shards1 + shards2);
            perGlyph = Math.min(perGlyph, ICON_SIZE);
            drawCost(g, v1, loc.x + PADDING, loc.y, PADDING + perGlyph * shards1);
            drawCost(g, v2, loc.x + cellWidth - perGlyph * shards2 - PADDING, loc.y, cellWidth);
        }
        loc.x = 0;
        loc.y += itemHeight / 2;
    }

    private void drawCost(final Graphics g, ManaCost value, float x, float y, final float cellWidth) {
        x += PADDING;
        y += PADDING + 1;
        final int genericManaCost = value.getGenericCost();
        final float xManaCosts = value.countX();
        final boolean hasGeneric = (genericManaCost > 0) || v1.isPureGeneric();

        final int cntGlyphs = value.getGlyphCount();
        final float offsetIfNoSpace = cntGlyphs > 1 ? (cellWidth - PADDING - ICON_SIZE) / (cntGlyphs - 1)
                : ICON_SIZE;
        final float dx = Math.min(ICON_SIZE, offsetIfNoSpace);

        // Display X Mana before any other type of mana
        if (xManaCosts > 0) {
            for (int i = 0; i < xManaCosts; i++) {
                CardFaceSymbols.drawSymbol(ManaCostShard.X.getImageKey(), g, x, y, ICON_SIZE);
                x += dx;
            }
        }

        // Display colorless mana before colored mana
        if (hasGeneric) {
            final String sGeneric = Integer.toString(genericManaCost);
            CardFaceSymbols.drawSymbol(sGeneric, g, x, y, ICON_SIZE);
            x += dx;
        }

        for (final ManaCostShard s : value) {
            if (s.equals(ManaCostShard.X)) {
                // X costs already drawn up above
                continue;
            }
            CardFaceSymbols.drawSymbol(s.getImageKey(), g, x, y, ICON_SIZE);
            x += dx;
        }
    }
}
