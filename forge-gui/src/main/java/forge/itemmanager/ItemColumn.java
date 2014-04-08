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

import com.google.common.base.Function;

import forge.item.InventoryItem;
import java.util.Map.Entry;


public class ItemColumn {
    private final ItemColumnConfig config;
    private final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort;
    private final Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay;

    public ItemColumn(ItemColumnConfig config0) {
        this(config0, config0.getFnSort(), config0.getFnDisplay());
    }
    public ItemColumn(ItemColumnConfig config0,
            Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort0,
            Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay0) {
        if (fnSort0 == null) {
            throw new NullPointerException("A sort function hasn't been set for Column " + this);
        }
        if (fnDisplay0 == null) {
            throw new NullPointerException("A display function hasn't been set for Column " + this);
        }

        config = config0;
        fnSort = fnSort0;
        fnDisplay = fnDisplay0;
    }

    public ItemColumnConfig getConfig() {
        return config;
    }

    public Function<Entry<InventoryItem, Integer>, Comparable<?>> getFnSort() {
        return fnSort;
    }

    public Function<Entry<? extends InventoryItem, Integer>, Object> getFnDisplay() {
        return fnDisplay;
    }

    @Override
    public String toString() {
        return config.getLongName();
    }
}
