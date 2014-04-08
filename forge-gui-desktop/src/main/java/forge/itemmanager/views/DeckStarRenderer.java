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
import forge.deck.DeckProxy;
import forge.deck.io.DeckPreferences;
import forge.item.InventoryItem;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;

import javax.swing.*;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Displays favorite icons
 */
@SuppressWarnings("serial")
public class DeckStarRenderer extends ItemCellRenderer {
    private DeckProxy deck;
    private SkinImage skinImage;

    @Override
    public boolean alwaysShowTooltip() {
        return true;
    }

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
        if (value instanceof DeckProxy) {
            deck = (DeckProxy) value;
        }
        else {
            deck = null;
        }
        update();
        return super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
    }

    @Override
    public <T extends InventoryItem> void processMouseEvent(final MouseEvent e, final ItemListView<T> listView, final Object value, final int row, final int column) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == 1 && value instanceof DeckProxy) {
            deck = (DeckProxy) value;
            DeckPreferences prefs = DeckPreferences.getPrefs(deck);
            prefs.setStarCount((prefs.getStarCount() + 1) % 2); //TODO: consider supporting more than 1 star
            update();
            listView.getTable().setRowSelectionInterval(row, row);
            listView.getTable().repaint();
            e.consume();
        }
    }
    
    private void update() {
        if (deck == null) {
            this.setToolTipText(null);
            skinImage = null;
        }
        else if (DeckPreferences.getPrefs(deck).getStarCount() == 0) {
            this.setToolTipText("Click to add " + deck.getName() + " to your favorites");
            skinImage = FSkin.getImage(FSkinProp.IMG_STAR_OUTINE);
        }
        else { //TODO: consider supporting more than 1 star
            this.setToolTipText("Click to remove " + deck.getName() + " from your favorites");
            skinImage = FSkin.getImage(FSkinProp.IMG_STAR_FILLED);
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

        if (skinImage == null) { return; }

        int size = 15;
        int width = this.getWidth();
        int height = this.getHeight();
        if (size > width) {
            size = width;
        }
        FSkin.drawImage(g, skinImage, (width - size) / 2, (height - size) / 2, size, size);
    }
}
