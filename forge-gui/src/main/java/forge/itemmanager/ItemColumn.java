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
import forge.itemmanager.ItemColumnConfig.SortState;

import java.util.Map;
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

    public String getShortName() {
        return config.getShortName();
    }

    public String getLongName() {
        return config.getLongName();
    }

    public int getIndex() {
        return config.getIndex();
    }

    public void setIndex(final int index0) {
        config.setIndex(index0);
    }

    public int getSortPriority() {
        return config.getSortPriority();
    }

    public void setSortPriority(final int sortPriority0) {
        config.setSortPriority(sortPriority0);
    }

    public SortState getSortState() {
        return config.getSortState();
    }

    public void setSortState(final SortState state0) {
        config.setSortState(state0);
    }

    public SortState getDefaultSortState() {
        return config.getDefaultSortState();
    }

    public boolean isVisible() {
        return config.isVisible();
    }

    public void setVisible(boolean visible0) {
        config.setVisible(visible0);
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

    public static void addColOverride(ItemManagerConfig config, Map<ColumnDef, ItemColumn> colOverrides, ColumnDef colDef) {
        ItemColumnConfig colConfig = config.getCols().get(colDef);
        addColOverride(config, colOverrides, colDef, colConfig.getFnSort(), colConfig.getFnDisplay());
    }
    public static void addColOverride(ItemManagerConfig config, Map<ColumnDef, ItemColumn> colOverrides, ColumnDef colDef,
            Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort0,
            Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay0) {
        colOverrides.put(colDef, new ItemColumn(config.getCols().get(colDef), fnSort0, fnDisplay0));
    }
}
