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
package forge.itemmanager;

import forge.toolbox.FSkin.SkinnedScrollPane;

import javax.swing.*;
import javax.swing.border.Border;


/**
 * Simple container pane meant to contain item managers.
 * 
 */
@SuppressWarnings("serial")
public final class ItemManagerContainer extends SkinnedScrollPane {
    public ItemManagerContainer() {
        this(null);
    }

    public ItemManagerContainer(ItemManager<?> itemManager) {
        super(null, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        setBorder((Border)null);
        setOpaque(false);
        getViewport().setOpaque(false);
        setItemManager(itemManager);
    }

    /**
     * 
     * Gets the item pool.
     * 
     * @return ItemPoolView
     */
    public void setItemManager(ItemManager<?> itemManager) {
        if (itemManager != null) {
            itemManager.initialize(); //ensure item manager is initialized
        }
        this.getViewport().setView(itemManager);
    }
}
