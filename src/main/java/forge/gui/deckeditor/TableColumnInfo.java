package forge.gui.deckeditor;

import java.util.Map.Entry;

import javax.swing.table.TableCellRenderer;

import net.slightlymagic.braids.util.lambda.Lambda1;

/** 
 * Holds single column set up for TableModel.
 * Contains name, width + functions to retrieve column's value for compare and for display
 * (they are different, in case of sets for instance)
 */

    @SuppressWarnings("rawtypes")
    public class TableColumnInfo<T> {
        private final String name;

        public int minWidth;
        public int maxWidth;
        public int nominalWidth;

        public boolean isMinMaxApplied = true;

        public final Lambda1<Comparable, Entry<T, Integer>> fnSort; // this will be used for sorting
        public final Lambda1<Object, Entry<T, Integer>> fnDisplay;  // this is used to display

        private TableCellRenderer cellRenderer = null;

        public final String getName() { return name; }

        public TableColumnInfo(final String colName,
                final Lambda1<Comparable, Entry<T, Integer>> fieldSort,
                final Lambda1<Object, Entry<T, Integer>> fieldDisplay) {
            fnSort = fieldSort;
            fnDisplay = fieldDisplay;
            this.name = colName;
        }

        public TableColumnInfo(final String colName, final int width,
                final Lambda1<Comparable, Entry<T, Integer>> fieldSort,
                final Lambda1<Object, Entry<T, Integer>> fieldDisplay) {
            this(colName, fieldSort, fieldDisplay);
            this.maxWidth = width;
            this.minWidth = width;
            this.nominalWidth = width;
        }
        public TableColumnInfo(final String colName, final int wMin, final int width, final int wMax,
                final Lambda1<Comparable, Entry<T, Integer>> fieldSort,
                final Lambda1<Object, Entry<T, Integer>> fieldDisplay) {
            this(colName, fieldSort, fieldDisplay);
            this.maxWidth = wMax;
            this.minWidth = wMin;
            this.nominalWidth = width;
        }


        public void setCellRenderer(final TableCellRenderer renderer) {
            cellRenderer = renderer;
        }

        public final TableCellRenderer getCellRenderer() {
            return cellRenderer;
        }
    }

