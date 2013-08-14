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
package forge.gui.toolbox.itemmanager;

import javax.swing.JScrollPane;

import forge.gui.toolbox.FScrollPanel;
import forge.item.InventoryItem;


/**
 * Simple container pane meant to contain item managers.
 * 
 */
@SuppressWarnings("serial")
public final class ItemManagerContainer extends FScrollPanel {
    public ItemManagerContainer() {
        this(null);
    }
    
    public ItemManagerContainer(ItemManager<? extends InventoryItem> itemManager) {
        super(itemManager, true, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    /**
     * 
     * Gets the item pool.
     * 
     * @return ItemPoolView
     */
    public void setItemManager(ItemManager<? extends InventoryItem> itemManager) {
        this.setViewport(itemManager);
    }
}
