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

import java.util.Map;
import java.util.Map.Entry;

import javax.swing.table.TableColumn;

import com.google.common.base.Function;

import forge.item.InventoryItem;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemColumnConfig;
import forge.itemmanager.ItemColumnConfig.SortState;
import forge.itemmanager.ItemManagerConfig;

/**
 * A column object in a EditorTableModel in the card editor.
 * Requires a sorting function and a display function
 * (to extract information as appropriate for table row data).
 */
public class ItemTableColumn extends TableColumn {
    private static final long serialVersionUID = 3749431834643427572L;

    private final ItemColumn itemColumn;

    public ItemTableColumn(final ItemColumn itemColumn0) {
        super();

        itemColumn = itemColumn0;
        final ColumnDef def = itemColumn.getConfig().getDef();
        this.setIdentifier(def);
        this.setHeaderValue(def.shortName);

        final int width = itemColumn.getConfig().getPreferredWidth();
        this.setPreferredWidth(width);
        if (def.isWidthFixed) {
            this.setMinWidth(width);
            this.setMaxWidth(width);
        }
        this.setCellRenderer(ItemCellRenderer.getColumnDefRenderer(def));
    }

    public ItemColumn getItemColumn() {
        return itemColumn;
    }

    public String getShortName() {
        return itemColumn.getConfig().getShortName();
    }

    public String getLongName() {
        return itemColumn.getConfig().getLongName();
    }

    public int getIndex() {
        return itemColumn.getConfig().getIndex();
    }

    public void setIndex(final int index0) {
        itemColumn.getConfig().setIndex(index0);
    }

    public int getSortPriority() {
        return itemColumn.getConfig().getSortPriority();
    }

    public void setSortPriority(final int sortPriority0) {
        itemColumn.getConfig().setSortPriority(sortPriority0);
    }

    public SortState getSortState() {
        return itemColumn.getConfig().getSortState();
    }

    public void setSortState(final SortState state0) {
        itemColumn.getConfig().setSortState(state0);
    }

    public SortState getDefaultSortState() {
        return itemColumn.getConfig().getDefaultSortState();
    }

    public boolean isVisible() {
        return itemColumn.getConfig().isVisible();
    }

    public void setVisible(final boolean visible0) {
        itemColumn.getConfig().setVisible(visible0);
    }

    public Function<Entry<InventoryItem, Integer>, Comparable<?>> getFnSort() {
        return itemColumn.getFnSort();
    }

    public Function<Entry<? extends InventoryItem, Integer>, Object> getFnDisplay() {
        return itemColumn.getFnDisplay();
    }

    public void startResize() {
        //if width fixed, temporarily clear min/max width to allow resize
        if (itemColumn.getConfig().getDef().isWidthFixed) {
            this.setMinWidth(0);
            this.setMaxWidth(Integer.MAX_VALUE);
        }
    }

    public void endResize() {
        //restore min/max width after resize to prevent table auto-scaling fixed width columns
        if (itemColumn.getConfig().getDef().isWidthFixed) {
            final int width = this.getWidth();
            this.setMinWidth(width);
            this.setMaxWidth(width);
        }
    }

    public void updatePreferredWidth() {
        itemColumn.getConfig().setPreferredWidth(this.getWidth());
    }

    @Override
    public String toString() {
        return itemColumn.toString();
    }

    public static void addColOverride(final ItemManagerConfig config, final Map<ColumnDef, ItemTableColumn> colOverrides, final ColumnDef colDef) {
        final ItemColumnConfig colConfig = config.getCols().get(colDef);
        addColOverride(config, colOverrides, colDef, colConfig.getFnSort(), colConfig.getFnDisplay());
    }
    public static void addColOverride(final ItemManagerConfig config, final Map<ColumnDef, ItemTableColumn> colOverrides, final ColumnDef colDef,
            final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort0,
            final Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay0) {
        colOverrides.put(colDef, new ItemTableColumn(new ItemColumn(config.getCols().get(colDef), fnSort0, fnDisplay0)));
    }
}
