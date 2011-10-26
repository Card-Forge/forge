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
    public int minWidth;

    /** The max width. */
    public int maxWidth;

    /** The nominal width. */
    public int nominalWidth;

    /** The is min max applied. */
    public boolean isMinMaxApplied = true;

    /** The fn sort. */
    public final Lambda1<Comparable, Entry<T, Integer>> fnSort; // this will be
                                                                // used for
                                                                // sorting

    /** The fn display. */
    public final Lambda1<Object, Entry<T, Integer>> fnDisplay; // this is used
                                                               // to display

    private TableCellRenderer cellRenderer = null;

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public final String getName() {
        return name;
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
        fnSort = fieldSort;
        fnDisplay = fieldDisplay;
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
        this.maxWidth = width;
        this.minWidth = width;
        this.nominalWidth = width;
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
        this.maxWidth = wMax;
        this.minWidth = wMin;
        this.nominalWidth = width;
    }

    /**
     * Sets the cell renderer.
     * 
     * @param renderer
     *            the new cell renderer
     */
    public final void setCellRenderer(final TableCellRenderer renderer) {
        cellRenderer = renderer;
    }

    /**
     * Gets the cell renderer.
     * 
     * @return the cell renderer
     */
    public final TableCellRenderer getCellRenderer() {
        return cellRenderer;
    }
}
