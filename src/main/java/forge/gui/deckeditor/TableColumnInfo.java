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
     * @return the minWidth
     */
    public int getMinWidth() {
        return minWidth;
    }

    /**
     * @param minWidth the minWidth to set
     */
    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the maxWidth
     */
    public int getMaxWidth() {
        return maxWidth;
    }

    /**
     * @param maxWidth the maxWidth to set
     */
    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the nominalWidth
     */
    public int getNominalWidth() {
        return nominalWidth;
    }

    /**
     * @param nominalWidth the nominalWidth to set
     */
    public void setNominalWidth(int nominalWidth) {
        this.nominalWidth = nominalWidth; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the isMinMaxApplied
     */
    public boolean isMinMaxApplied() {
        return isMinMaxApplied;
    }

    /**
     * @param isMinMaxApplied the isMinMaxApplied to set
     */
    public void setMinMaxApplied(boolean isMinMaxApplied) {
        this.isMinMaxApplied = isMinMaxApplied; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the fnSort
     */
    public Lambda1<Comparable, Entry<T, Integer>> getFnSort() {
        return fnSort;
    }

    /**
     * @return the fnDisplay
     */
    public Lambda1<Object, Entry<T, Integer>> getFnDisplay() {
        return fnDisplay;
    }
}
