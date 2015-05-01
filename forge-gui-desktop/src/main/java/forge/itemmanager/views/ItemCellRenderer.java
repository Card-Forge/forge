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
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;

import forge.item.InventoryItem;
import forge.itemmanager.ColumnDef;
import forge.toolbox.FSkin;

/**
 * Base cell renderer class for item tables
 */
@SuppressWarnings("serial")
public class ItemCellRenderer extends DefaultTableCellRenderer {
    private static final Border DEFAULT_BORDER = new EmptyBorder(1, 1, 1, 1);

    public static ItemCellRenderer getColumnDefRenderer(final ColumnDef columnDef) {
        switch (columnDef) {
        case POWER:
        case TOUGHNESS:
        case CMC:
        case DECK_MAIN:
        case DECK_SIDE:
            return new IntegerRenderer();
        case SET:
        case DECK_EDITION:
            return new SetCodeRenderer();
        case COST:
            return new ManaCostRenderer();
        case DECK_COLOR:
            return new ColorSetRenderer();
        case FAVORITE:
            return new StarRenderer();
        case DECK_FAVORITE:
            return new DeckStarRenderer();
        case DECK_QUANTITY:
            return new DeckQuantityRenderer();
        default:
            return new ItemCellRenderer();
        }
    }

    public boolean alwaysShowTooltip() {
        return false;
    }

    protected <T extends InventoryItem> void processMouseEvent(final MouseEvent e, final ItemListView<T> listView, final Object value, final int row, final int column) {
    }

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        final JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        lbl.setBorder(DEFAULT_BORDER); //prevent selected cell having inner border
        if (isSelected) {
            lbl.setBackground(table.getSelectionBackground());
        }
        else {
            if (row % 2 == 0) {
                lbl.setBackground(table.getBackground());
            }
            else {
                FSkin.setTempBackground(lbl, ItemListView.ALT_ROW_COLOR);
            }
        }
        return lbl;
    }
}
