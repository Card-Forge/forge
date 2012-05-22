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
package forge.gui.deckeditor.tables;

import java.util.Map.Entry;

import javax.swing.table.TableColumn;

import forge.gui.deckeditor.tables.SColumnUtil.SortState;
import forge.util.closures.Lambda1;

/**
 * A column object in a TableModel in the card editor.
 * Requires a sorting function and a display function
 * (to extract information as appropriate for table row data).
 * 
 * @param <T> a generic type
 */

@SuppressWarnings({ "rawtypes", "serial" })
public class TableColumnInfo<T> extends TableColumn {
    private SortState sortstate = SortState.NONE;
    private int sortPriority = 0;
    private boolean show = true;
    private String enumval;

    private Lambda1<Comparable, Entry<T, Integer>> fnSort;
    private Lambda1<Object, Entry<T, Integer>> fnDisplay;

    /** */
    public TableColumnInfo() {
        super();
    }

    /**
     * Unique identifier in SColumnUtil.ColumnName enum.
     * 
     * @return {@link java.lang.String}
     */
    public String getEnumValue() {
        return enumval;
    }

    /**
     * Unique identifier in SColumnUtil.ColumnName enum.
     * 
     * @param val0 &emsp; {@link java.lang.String}
     */
    public void setEnumValue(final String val0) {
        this.enumval = val0;
    }

    /**
     * Position in sort cascade, 0 for no priority.
     * 
     * @return int
     */
    public int getSortPriority() {
        return sortPriority;
    }

    /**
     * Position in sort cascade, 0 for no priority.
     * 
     * @param position0 &emsp; int
     */
    public void setSortPriority(final int position0) {
        this.sortPriority = position0;
    }

    /** @return {@link forge.gui.deckeditor.tables.TableModel.SortState} */
    public SortState getSortState() {
        return this.sortstate;
    }

     /** @param state0 &emsp; {@link forge.gui.deckeditor.tables.TableColumnInfo.SortState} */
    public void setSortState(final SortState state0) {
        this.sortstate = state0;
    }

    /** @return boolean */
    public boolean isShowing() {
        return this.show;
    }

     /** @param boolean0 &emsp; show/hide this column */
    public void setShowing(final boolean boolean0) {
        this.show = boolean0;
    }

    /**
     * Lambda closure used to sort this column.
     * 
     * @return the fnSort
     */
    public Lambda1<Comparable, Entry<T, Integer>> getFnSort() {
        if (fnSort.equals(null)) {
           throw new NullPointerException("A sort function hasn't been set for "
                   + "Column " + TableColumnInfo.this.getIdentifier());
        }
        return this.fnSort;
    }

    /**
     * Gets the fn display.
     * 
     * @return the fnDisplay
     */
    public Lambda1<Object, Entry<T, Integer>> getFnDisplay() {
        if (fnSort.equals(null)) {
            throw new NullPointerException("A display function hasn't been set for "
                    + "Column " + TableColumnInfo.this.getIdentifier());
         }
        return this.fnDisplay;
    }

    /**
     * Lambda closure used to sort this column, and fn display.
     * 
     * @param lambda0 the fnSort
     * @param lambda1 the fnDisplay
     */
    public void setSortAndDisplayFunctions(final Lambda1<Comparable, Entry<T, Integer>> lambda0, final Lambda1<Object, Entry<T, Integer>> lambda1) {
        this.fnSort = lambda0;
        this.fnDisplay = lambda1;
    }
}
