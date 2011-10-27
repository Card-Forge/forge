package forge.gui.deckeditor;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;

/**
 * <p>
 * TableModel class.
 * </p>
 * 
 * @param <T>
 *            the generic type
 * @author Forge
 * @version $Id$
 */
public final class TableModel<T extends InventoryItem> extends AbstractTableModel {
    /**
     * 
     */
    private static final long serialVersionUID = -6896726613116254828L;

    private final class SortOrders {
        private class Order {
            public final int sortColumn;
            public boolean isSortAsc = true;

            public Order(final int col) {
                sortColumn = col;
            }
        };

        private final int MAX_DEPTH = 3;
        private List<Order> orders = new ArrayList<Order>(3);
        private TableSorterCascade<T> sorter = null;
        private boolean isSorterReady = false;

        private int indexOfColumn(final int column) {
            int posColumn = orders.size() - 1;
            for (; posColumn >= 0; posColumn--) {
                if (orders.get(posColumn) != null && orders.get(posColumn).sortColumn == column) {
                    break;
                }
            }
            return posColumn;
        }

        // index of column to sort by, desired direction
        public void add(final int column, final boolean wantAsc) {
            add(column);
            orders.get(0).isSortAsc = wantAsc;
            isSorterReady = false;
        }

        // puts desired direction on top, set "asc"; if already was on top,
        // inverts direction;
        public void add(final int column) {
            int posColumn = indexOfColumn(column);
            switch (posColumn) {
            case -1: // no such column here - let's add then
                orders.add(0, new Order(column));
                break;
            case 0: // found at top-level, should invert
                orders.get(0).isSortAsc ^= true; // invert
                break;
            default: // found somewhere, move down others, set this one onto
                     // top;
                orders.remove(posColumn);
                orders.add(0, new Order(column));
                break;
            }
            if (orders.size() > MAX_DEPTH) {
                orders.remove(MAX_DEPTH);
            }
            isSorterReady = false;
        }

        public TableSorterCascade<T> getSorter() {
            if (!isSorterReady) {
                List<TableSorter<T>> oneColSorters = new ArrayList<TableSorter<T>>(MAX_DEPTH);
                for (Order order : orders) {
                    oneColSorters.add(new TableSorter<T>(columns.get(order.sortColumn).fnSort, order.isSortAsc));
                }
                sorter = new TableSorterCascade<T>(oneColSorters);
            }
            return sorter;
        }
    }

    private ItemPool<T> data;
    private final CardPanelBase cardDisplay;
    private final List<TableColumnInfo<T>> columns;
    private final SortOrders sortOrders = new SortOrders();

    /**
     * Instantiates a new table model.
     * 
     * @param cd
     *            the cd
     * @param columnsToShow
     *            the columns to show
     * @param cls
     *            the cls
     */
    public TableModel(final CardPanelBase cd, final List<TableColumnInfo<T>> columnsToShow, final Class<T> cls) {
        data = new ItemPool<T>(cls);
        cardDisplay = cd;
        columns = columnsToShow;
        columns.get(4).isMinMaxApplied = false;
    }

    /**
     * Resize cols.
     * 
     * @param table
     *            the table
     */
    public void resizeCols(final JTable table) {
        TableColumn tableColumn = null;
        for (int i = 0; i < table.getColumnCount(); i++) {
            tableColumn = table.getColumnModel().getColumn(i);
            TableColumnInfo<T> colInfo = columns.get(i);

            tableColumn.setPreferredWidth(colInfo.nominalWidth);
            if (colInfo.isMinMaxApplied) {
                tableColumn.setMinWidth(colInfo.minWidth);
                tableColumn.setMaxWidth(colInfo.maxWidth);
            }
        }
    }

    /**
     * Clear.
     */
    public void clear() {
        data.clear();
    }

    /**
     * Gets the cards.
     * 
     * @return the cards
     */
    public ItemPoolView<T> getCards() {
        return data.getView();
    }

    /**
     * <p>
     * removeCard.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public void removeCard(final T c) {
        boolean wasThere = data.count(c) > 0;
        if (wasThere) {
            data.remove(c);
            fireTableDataChanged();
        }
    }

    /**
     * Adds the card.
     * 
     * @param c
     *            the c
     */
    public void addCard(final T c) {
        data.add(c);
        fireTableDataChanged();
    }

    /**
     * Adds the card.
     * 
     * @param c
     *            the c
     * @param count
     *            the count
     */
    public void addCard(final T c, final int count) {
        data.add(c, count);
        fireTableDataChanged();
    }

    /**
     * Adds the card.
     * 
     * @param e
     *            the e
     */
    public void addCard(final Entry<T, Integer> e) {
        data.add(e.getKey(), e.getValue());
        fireTableDataChanged();
    }

    /**
     * Adds the cards.
     * 
     * @param c
     *            the c
     */
    public void addCards(final Iterable<Entry<T, Integer>> c) {
        data.addAll(c);
        fireTableDataChanged();
    }

    /**
     * Adds the all cards.
     * 
     * @param c
     *            the c
     */
    public void addAllCards(final Iterable<T> c) {
        data.addAllCards(c);
        fireTableDataChanged();
    }

    /**
     * Row to card.
     * 
     * @param row
     *            the row
     * @return the entry
     */
    public Entry<T, Integer> rowToCard(final int row) {
        List<Entry<T, Integer>> model = data.getOrderedList();
        return row >= 0 && row < model.size() ? model.get(row) : null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.table.TableModel#getRowCount()
     */
    /**
     * @return int
     */
    public int getRowCount() {
        return data.countDistinct();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    /**
     * @return int
     */
    public int getColumnCount() {
        return columns.size();
    }

    /** {@inheritDoc} */
    @Override
    public String getColumnName(final int n) {
        return columns.get(n).getName();
    }

    /** {@inheritDoc} */
    public Object getValueAt(final int row, final int column) {
        return columns.get(column).fnDisplay.apply(rowToCard(row));
    }

    /**
     * The listener interface for receiving column events. The class that is
     * interested in processing a column event implements this interface, and
     * the object created with that class is registered with a component using
     * the component's addColumnListener method. When
     * the column event occurs, that object's appropriate
     * method is invoked.
     * 
     */
    class ColumnListener extends MouseAdapter {

        /** The table. */
        protected JTable table;

        /**
         * Instantiates a new column listener.
         * 
         * @param t
         *            the t
         */
        public ColumnListener(final JTable t) {
            table = t;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
         */
        /**
         * @param e MouseEvent
         */
        public void mouseClicked(final MouseEvent e) {
            TableColumnModel colModel = table.getColumnModel();
            int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
            int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();

            if (modelIndex < 0) {
                return;
            }

            // This will invert if needed
            sortOrders.add(modelIndex);

            for (int i = 0; i < columns.size(); i++) {
                TableColumn column = colModel.getColumn(i);
                column.setHeaderValue(getColumnName(column.getModelIndex()));
            }
            table.getTableHeader().repaint();

            resort();
            table.tableChanged(new TableModelEvent(TableModel.this));
            table.repaint();
        }
    }

    /**
     * Show selected card.
     * 
     * @param table
     *            the table
     */
    public void showSelectedCard(final JTable table) {
        int row = table.getSelectedRow();
        if (row != -1) {
            T cp = rowToCard(row).getKey();
            cardDisplay.showCard(cp);
        }
    }

    /**
     * <p>
     * addListeners.
     * </p>
     * 
     * @param table
     *            a {@link javax.swing.JTable} object.
     */
    public void addListeners(final JTable table) {
        // updates card detail, listens to any key strokes
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(final ListSelectionEvent arg0) {
                showSelectedCard(table);
            }
        });
        table.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(final FocusEvent e) {
            }

            @Override
            public void focusGained(final FocusEvent e) {
                showSelectedCard(table);
            }
        });

        table.getTableHeader().addMouseListener(new ColumnListener(table));

    } // addCardListener()

    /**
     * Resort.
     */
    public void resort() {
        Collections.sort(data.getOrderedList(), sortOrders.getSorter());
    }

    /**
     * Sort.
     * 
     * @param iCol
     *            the i col
     * @param isAsc
     *            the is asc
     */
    public void sort(final int iCol, final boolean isAsc) {
        sortOrders.add(iCol, isAsc);
        resort();
    }

} // CardTableModel
