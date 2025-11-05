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
import java.awt.event.MouseEvent;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import forge.gui.card.CardPreferences;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import forge.util.CardTranslation;
import forge.util.Localizer;

/**
 * Displays favorite icons
 */
public class StarRenderer extends ItemCellRenderer {
    private int favorite;
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
        IPaperCard card = getCardFromRow(table.getModel(), row);
        if (value instanceof Integer) {
            favorite = (int) value;
        }
        else {
            favorite = 0;
        }
        update(card);
        return super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
    }

    @Override
    public <T extends InventoryItem> void processMouseEvent(final MouseEvent e, final ItemListView<T> listView, final Object value, final int row, final int column) {
        IPaperCard card = getCardFromRow(listView.getTableModel(), row);
        if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == 1 && card != null) {
            CardPreferences prefs = CardPreferences.getPrefs(card);
            this.favorite = (prefs.getStarCount() + 1) % 2;
            prefs.setStarCount(favorite); //TODO: consider supporting more than 1 star
            CardPreferences.save();
            update(card);
            JTable table = listView.getTable();
            table.setRowSelectionInterval(row, row);
            if(table.getModel() instanceof ItemListView<?>.ItemTableModel tableModel)
                tableModel.fireTableRowsUpdated(row, row);
            table.repaint();
            e.consume();
        }
    }

    private IPaperCard getCardFromRow(TableModel model, int row) {
        if(!(model instanceof  ItemListView<?>.ItemTableModel tableModel))
            return null;
        Object cardObj = tableModel.rowToItem(row);
        if(cardObj instanceof Map.Entry<?, ?> entry && entry.getKey() instanceof IPaperCard)
            return (IPaperCard) entry.getKey();
        return null;
    }
    
    private void update(IPaperCard card) {
        final Localizer localizer = Localizer.getInstance();
        if(card == null) {
            this.setToolTipText(null);
            skinImage = null;
            return;
        }
        if (favorite == 0) {
            this.setToolTipText(localizer.getMessage("lblClickToAddTargetToFavorites", CardTranslation.getTranslatedName(card.getDisplayName())));
            skinImage = FSkin.getImage(FSkinProp.IMG_STAR_OUTLINE);
        }
        else { //TODO: consider supporting more than 1 star
            this.setToolTipText(localizer.getMessage("lblClickToRemoveTargetToFavorites", CardTranslation.getTranslatedName(card.getDisplayName())));
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
