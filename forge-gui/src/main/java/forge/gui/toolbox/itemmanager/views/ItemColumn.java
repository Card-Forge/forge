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

import forge.gui.toolbox.itemmanager.views.ItemColumnConfig.SortState;
import forge.item.InventoryItem;

import javax.swing.table.TableColumn;

import java.util.Map.Entry;

/**
 * A column object in a EditorTableModel in the card editor.
 * Requires a sorting function and a display function
 * (to extract information as appropriate for table row data).
 */
public class ItemColumn extends TableColumn {
    private static final long serialVersionUID = 3749431834643427572L;

    private ItemColumnConfig config;
    private final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort;
    private final Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay;

    public ItemColumn(ItemColumnConfig config0) {
        this(config0, config0.getFnSort(), config0.getFnDisplay());
    }
    public ItemColumn(ItemColumnConfig config0,
            Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort0,
            Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay0) {
        super();

        if (fnSort0 == null) {
            throw new NullPointerException("A sort function hasn't been set for Column " + this);
        }
        if (fnDisplay0 == null) {
            throw new NullPointerException("A display function hasn't been set for Column " + this);
        }

        this.config = config0;
        this.fnSort = fnSort0;
        this.fnDisplay = fnDisplay0;

        ColumnDef def = config0.getDef();
        this.setIdentifier(def);
        this.setHeaderValue(def.shortName);

        int width = config0.getPreferredWidth();
        this.setPreferredWidth(width);
        if (def.isWidthFixed) {
            this.setMinWidth(width);
            this.setMaxWidth(width);
        }
        this.setCellRenderer(def.cellRenderer);
    }

    public String getShortName() {
        return this.config.getShortName();
    }

    public String getLongName() {
        return this.config.getLongName();
    }

    public int getIndex() {
        return this.config.getIndex();
    }

    public void setIndex(final int index0) {
        this.config.setIndex(index0);
    }

    public int getSortPriority() {
        return this.config.getSortPriority();
    }

    public void setSortPriority(final int sortPriority0) {
        this.config.setSortPriority(sortPriority0);
    }

    public SortState getSortState() {
        return this.config.getSortState();
    }

    public void setSortState(final SortState state0) {
        this.config.setSortState(state0);
    }

    public SortState getDefaultSortState() {
        return this.config.getDefaultSortState();
    }

    public boolean isVisible() {
        return this.config.isVisible();
    }

    public void setVisible(boolean visible0) {
        this.config.setVisible(visible0);
    }

    public Function<Entry<InventoryItem, Integer>, Comparable<?>> getFnSort() {
        return this.fnSort;
    }

    public Function<Entry<? extends InventoryItem, Integer>, Object> getFnDisplay() {
        return this.fnDisplay;
    }

    public void startResize() {
        //if width fixed, temporarily clear min/max width to allow resize
        if (this.config.getDef().isWidthFixed) {
            this.setMinWidth(0);
            this.setMaxWidth(Integer.MAX_VALUE);
        }
    }

    public void endResize() {
        //restore min/max width after resize to prevent table auto-scaling fixed width columns
        if (this.config.getDef().isWidthFixed) {
            int width = this.getWidth();
            this.setMinWidth(width);
            this.setMaxWidth(width);
        }
    }

    public void updatePreferredWidth() {
        this.config.setPreferredWidth(this.getWidth());
    }

    @Override
    public String toString() {
        return this.config.getLongName();
    }
}
