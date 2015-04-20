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

import forge.assets.FSkinProp;
import forge.item.InventoryItem;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Displays deck quantity with +/- buttons
 */
@SuppressWarnings("serial")
public class DeckQuantityRenderer extends ItemCellRenderer {
    private static final SkinImage imgAdd = FSkin.getIcon(FSkinProp.ICO_PLUS);
    private static final SkinImage imgRemove = FSkin.getIcon(FSkinProp.ICO_MINUS);
    private static final int imgSize = 13;

    public DeckQuantityRenderer() {
        this.setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public <T extends InventoryItem> void processMouseEvent(final MouseEvent e, final ItemListView<T> listView, final Object value, final int row, final int column) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == 1) {
            Rectangle cellBounds = listView.getTable().getCellRect(row, column, false);
            int delta = 0;
            int x = e.getX() - cellBounds.x;

            if (x <= imgSize) { //add button
                delta = 1;
            }
            else if (x >= cellBounds.width - imgSize) { //remove button
                delta = -1;
            }

            if (delta != 0) {
                listView.getTable().setRowSelectionInterval(row, row); //must set selection first so scroll doesn't get messed up
                CDeckEditorUI.SINGLETON_INSTANCE.incrementDeckQuantity(listView.getItemAtIndex(row), delta);
                e.consume();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.JComponent#paint(java.awt.Graphics)
     */
    @Override
    public final void paint(final Graphics g) {
        super.paint(g);

        int y = (this.getHeight() - imgSize) / 2;
        FSkin.drawImage(g, imgAdd, 0, y, imgSize, imgSize);
        FSkin.drawImage(g, imgRemove, this.getWidth() - imgSize, y, imgSize, imgSize);
    }
}
