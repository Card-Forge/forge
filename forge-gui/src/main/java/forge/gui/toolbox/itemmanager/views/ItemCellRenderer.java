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
package forge.gui.toolbox.itemmanager.views;

import forge.gui.toolbox.FSkin;
import forge.item.InventoryItem;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Base cell renderer class for item tables
 */
@SuppressWarnings("serial")
public class ItemCellRenderer extends DefaultTableCellRenderer {
    private static final Border DEFAULT_BORDER = new EmptyBorder(1, 1, 1, 1);

    public boolean alwaysShowTooltip() {
        return false;
    }

    public <T extends InventoryItem> void processMouseEvent(final MouseEvent e, final ItemListView<T> listView, final Object value, final int row, final int column) {
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
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
