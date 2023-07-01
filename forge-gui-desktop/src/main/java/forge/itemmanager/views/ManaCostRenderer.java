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

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JTable;

import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.toolbox.CardFaceSymbols;

/**
 * Displays mana cost as symbols.
 */
public class ManaCostRenderer extends ItemCellRenderer {
    private static final long serialVersionUID = 1770527102334163549L;

    private static final int elemtWidth = 13;
    private static final int elemtGap = 0;
    private static final int padding0 = 2;
    private static final int spaceBetweenSplitCosts = 3;

    private ManaCost v1;
    private ManaCost v2;

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent
     * (javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
     */
    @Override
    public final Component getTableCellRendererComponent(final JTable table, final Object value,
            final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        this.v2 = null;
        if (value instanceof CardRules) {
            CardRules v = (CardRules) value;
            this.v1 = v.getMainPart().getManaCost();
            this.v2 = v.getSplitType() == CardSplitType.Split ? v.getOtherPart().getManaCost() : null;
        } else if (value instanceof ManaCost) {
            this.v1 = (ManaCost) value;
        } else {
            this.v1 = ManaCost.NO_COST;
        }
        this.setToolTipText(v2 == null ? v1.toString() : v1.toString() + " // " + v2.toString());
        return super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.JComponent#paint(java.awt.Graphics)
     */
    @Override
    public final void paint(final Graphics g) {
        super.paint(g);

        final int cellWidth = this.getWidth();

        if (v2 == null) {
            drawCost(g, v1, padding0, cellWidth);
        }
        else {
            int shards1 = v1.getGlyphCount();
            int shards2 = v2.getGlyphCount();

            int perGlyph = (cellWidth - padding0 - spaceBetweenSplitCosts) / (shards1 + shards2);
            perGlyph = Math.min(perGlyph, elemtWidth + elemtGap);
            drawCost(g, v1, padding0, padding0 + perGlyph * shards1);
            drawCost(g, v2, cellWidth - perGlyph * shards2 - padding0, cellWidth );
        }
    }

    /**
     * TODO: Write javadoc for this method.
     * @param g
     * @param padding
     * @param cellWidth
     */
    private void drawCost(final Graphics g, ManaCost value, final int padding, final int cellWidth) {
        int x = padding;
        int y = padding0 + 1;
        final int genericManaCost = value.getGenericCost();
        final int xManaCosts = value.countX();
        final boolean hasGeneric = (genericManaCost > 0) || this.v1.isPureGeneric();

        final int cntGlyphs = value.getGlyphCount();
        final int offsetIfNoSpace = cntGlyphs > 1 ? (cellWidth - padding - elemtWidth) / (cntGlyphs - 1)
                : elemtWidth + elemtGap;
        final int dx = Math.min(elemtWidth + elemtGap, offsetIfNoSpace);

        // Display X Mana before any other type of mana
        if (xManaCosts > 0) {
            for (int i = 0; i < xManaCosts; i++) {
                CardFaceSymbols.drawManaSymbol(ManaCostShard.X.getImageKey(), g, x, y);
                x += dx;
            }
        }

        // Display generic mana before colored mana
        if (hasGeneric) {
            final String sGeneric = Integer.toString(genericManaCost);
            CardFaceSymbols.drawManaSymbol(sGeneric, g, x, y);
            x += dx;
        }

        for (final ManaCostShard s : value) {
            if (s.equals(ManaCostShard.X)) {
                // X costs already drawn up above
                continue;
            }
            CardFaceSymbols.drawManaSymbol(s.getImageKey(), g, x, y);
            x += dx;
        }
    }
}
