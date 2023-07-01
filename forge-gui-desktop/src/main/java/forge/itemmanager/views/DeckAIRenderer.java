
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
import java.util.TreeSet;

import javax.swing.JTable;

import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.util.DeckAIUtils;
import forge.util.Localizer;


public class DeckAIRenderer extends ItemCellRenderer {

    private final Localizer localizer = Localizer.getInstance();

    @Override
    public boolean alwaysShowTooltip() {
        return false;
    }

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        final StringBuilder label = new StringBuilder("");
        final StringBuilder tooltip = new StringBuilder("");
        if (value instanceof Deck.UnplayableAICards) {
            final Deck.UnplayableAICards removedUnplayableCards = ((Deck.UnplayableAICards) value);
            for (final DeckSection s: new TreeSet<>(removedUnplayableCards.unplayable.keySet())) {
                int unplayableSize = removedUnplayableCards.unplayable.get(s).size();
                tooltip.append("[" + DeckAIUtils.getLocalizedDeckSection(localizer, s) + ":" + unplayableSize + "]");
            }
            label.append(removedUnplayableCards.inMainDeck > 0 ? "" + removedUnplayableCards.inMainDeck : "");
        }
        setToolTipText(tooltip.toString());
        return super.getTableCellRendererComponent(table, label.toString(), isSelected, hasFocus, row, row);
    }
}