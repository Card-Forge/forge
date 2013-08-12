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
import forge.item.InventoryItem;


/**
 * Simple container pane meant to contain item managers.
 * 
 */
public final class ItemManagerContainer extends JScrollPane {
    private static final long serialVersionUID = 5537189102601508207L;

    public ItemManagerContainer() {
        this(null);
    }
    
    public ItemManagerContainer(ItemManager<? extends InventoryItem> itemManager) {
        super(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.setOpaque(false);
        this.setViewport(itemManager);
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
