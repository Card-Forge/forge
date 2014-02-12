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

import com.google.common.base.Function;

import forge.item.InventoryItem;

import javax.swing.table.TableColumn;

import java.util.Map.Entry;

/**
 * A column object in a EditorTableModel in the card editor.
 * Requires a sorting function and a display function
 * (to extract information as appropriate for table row data).
 * 
 * @param <T> a generic type
 */
public class ItemColumn extends TableColumn {
    private static final long serialVersionUID = 3749431834643427572L;

    public static enum SortState {
        NONE,
        ASC,
        DESC
    }

    private final ColumnDef def;
    private SortState sortState = SortState.NONE;
    private int sortPriority = 0;
    private boolean visible = true;
    private int index = 0;
    private final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort;
    private final Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay;

    public ItemColumn(ColumnDef def0) {
        this(def0, def0.fnSort, def0.fnDisplay);
    }
    public ItemColumn(ColumnDef def0,
            Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort0,
            Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay0) {
        super();

        if (fnSort0 == null) {
            throw new NullPointerException("A sort function hasn't been set for Column " + this);
        }
        if (fnDisplay0 == null) {
            throw new NullPointerException("A display function hasn't been set for Column " + this);
        }

        this.def = def0;
        this.setIdentifier(def0);
        this.setHeaderValue(def.shortName);

        this.setPreferredWidth(def.preferredWidth);
        if (def.isWidthFixed) {
            this.setMinWidth(def.preferredWidth);
            this.setMaxWidth(def.preferredWidth);
        }
        this.fnSort = fnSort0;
        this.fnDisplay = fnDisplay0;
        this.setCellRenderer(def.cellRenderer);
    }

    public String getShortName() {
        return this.def.shortName;
    }

    public String getLongName() {
        return this.def.longName;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(final int index0) {
        this.index = index0;
    }

    public int getSortPriority() {
        return sortPriority;
    }

    public void setSortPriority(final int sortPriority0) {
        int oldSortPriority = this.sortPriority;
        this.sortPriority = sortPriority0;
        if (sortPriority0 == 0) {
            this.sortState = SortState.NONE;
        }
        else if (oldSortPriority == 0) {
            this.sortState = def.sortState;
        }
    }

    public SortState getSortState() {
        return this.sortState;
    }

    public void setSortState(final SortState state0) {
        this.sortState = state0;
    }

    public SortState getDefaultSortState() {
        return this.def.sortState;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible0) {
        this.visible = visible0;
    }

    public Function<Entry<InventoryItem, Integer>, Comparable<?>> getFnSort() {
        return this.fnSort;
    }

    public Function<Entry<? extends InventoryItem, Integer>, Object> getFnDisplay() {
        return this.fnDisplay;
    }

    public void startResize() {
        //if width fixed, temporarily clear min/max width to allow resize
        if (def.isWidthFixed) {
            this.setMinWidth(0);
            this.setMaxWidth(Integer.MAX_VALUE);
        }
    }

    public void endResize() {
        //restore min/max width after resize to prevent table auto-scaling fixed width columns
        if (def.isWidthFixed) {
            int width = this.getWidth();
            this.setMinWidth(width);
            this.setMaxWidth(width);
        }
    }

    @Override
    public String toString() {
        return this.getLongName();
    }
}
