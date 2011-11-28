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
package forge.gui.deckeditor;

import java.util.Map.Entry;

import javax.swing.table.TableCellRenderer;

import net.slightlymagic.braids.util.lambda.Lambda1;

/**
 * Holds single column set up for TableModel. Contains name, width + functions
 * to retrieve column's value for compare and for display (they are different,
 * in case of sets for instance)
 * 
 * @param <T>
 *            the generic type
 */

@SuppressWarnings("rawtypes")
public class TableColumnInfo<T> {
    private final String name;

    /** The min width. */
    private int minWidth;

    /** The max width. */
    private int maxWidth;

    /** The nominal width. */
    private int nominalWidth;

    /** The is min max applied. */
    private boolean isMinMaxApplied = true;

    /** The fn sort. */
    private final Lambda1<Comparable, Entry<T, Integer>> fnSort; // this will be
                                                                 // used for
                                                                 // sorting

    /** The fn display. */
    private final Lambda1<Object, Entry<T, Integer>> fnDisplay; // this is used
                                                                // to display

    private TableCellRenderer cellRenderer = null;

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Instantiates a new table column info.
     * 
     * @param colName
     *            the col name
     * @param fieldSort
     *            the field sort
     * @param fieldDisplay
     *            the field display
     */
    public TableColumnInfo(final String colName, final Lambda1<Comparable, Entry<T, Integer>> fieldSort,
            final Lambda1<Object, Entry<T, Integer>> fieldDisplay) {
        this.fnSort = fieldSort;
        this.fnDisplay = fieldDisplay;
        this.name = colName;
    }

    /**
     * Instantiates a new table column info.
     * 
     * @param colName
     *            the col name
     * @param width
     *            the width
     * @param fieldSort
     *            the field sort
     * @param fieldDisplay
     *            the field display
     */
    public TableColumnInfo(final String colName, final int width,
            final Lambda1<Comparable, Entry<T, Integer>> fieldSort,
            final Lambda1<Object, Entry<T, Integer>> fieldDisplay) {
        this(colName, fieldSort, fieldDisplay);
        this.setMaxWidth(width);
        this.setMinWidth(width);
        this.setNominalWidth(width);
    }

    /**
     * Instantiates a new table column info.
     * 
     * @param colName
     *            the col name
     * @param wMin
     *            the w min
     * @param width
     *            the width
     * @param wMax
     *            the w max
     * @param fieldSort
     *            the field sort
     * @param fieldDisplay
     *            the field display
     */
    public TableColumnInfo(final String colName, final int wMin, final int width, final int wMax,
            final Lambda1<Comparable, Entry<T, Integer>> fieldSort,
            final Lambda1<Object, Entry<T, Integer>> fieldDisplay) {
        this(colName, fieldSort, fieldDisplay);
        this.setMaxWidth(wMax);
        this.setMinWidth(wMin);
        this.setNominalWidth(width);
    }

    /**
     * Sets the cell renderer.
     * 
     * @param renderer
     *            the new cell renderer
     */
    public final void setCellRenderer(final TableCellRenderer renderer) {
        this.cellRenderer = renderer;
    }

    /**
     * Gets the cell renderer.
     * 
     * @return the cell renderer
     */
    public final TableCellRenderer getCellRenderer() {
        return this.cellRenderer;
    }

    /**
     * Gets the min width.
     * 
     * @return the minWidth
     */
    public int getMinWidth() {
        return this.minWidth;
    }

    /**
     * Sets the min width.
     * 
     * @param minWidth
     *            the minWidth to set
     */
    public void setMinWidth(final int minWidth) {
        this.minWidth = minWidth; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the max width.
     * 
     * @return the maxWidth
     */
    public int getMaxWidth() {
        return this.maxWidth;
    }

    /**
     * Sets the max width.
     * 
     * @param maxWidth
     *            the maxWidth to set
     */
    public void setMaxWidth(final int maxWidth) {
        this.maxWidth = maxWidth; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the nominal width.
     * 
     * @return the nominalWidth
     */
    public int getNominalWidth() {
        return this.nominalWidth;
    }

    /**
     * Sets the nominal width.
     * 
     * @param nominalWidth
     *            the nominalWidth to set
     */
    public void setNominalWidth(final int nominalWidth) {
        this.nominalWidth = nominalWidth; // TODO: Add 0 to parameter's name.
    }

    /**
     * Checks if is min max applied.
     * 
     * @return the isMinMaxApplied
     */
    public boolean isMinMaxApplied() {
        return this.isMinMaxApplied;
    }

    /**
     * Sets the min max applied.
     * 
     * @param isMinMaxApplied
     *            the isMinMaxApplied to set
     */
    public void setMinMaxApplied(final boolean isMinMaxApplied) {
        this.isMinMaxApplied = isMinMaxApplied; // TODO: Add 0 to parameter's
                                                // name.
    }

    /**
     * Gets the fn sort.
     * 
     * @return the fnSort
     */
    public Lambda1<Comparable, Entry<T, Integer>> getFnSort() {
        return this.fnSort;
    }

    /**
     * Gets the fn display.
     * 
     * @return the fnDisplay
     */
    public Lambda1<Object, Entry<T, Integer>> getFnDisplay() {
        return this.fnDisplay;
    }
}
