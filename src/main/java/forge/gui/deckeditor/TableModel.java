package forge.gui.deckeditor;


import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;


/**
 * <p>TableModel class.</p>
 *
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
            public Order(final int col) { sortColumn = col; }
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

        // puts desired direction on top, set "asc"; if already was on top, inverts direction;
        public void add(final int column) {
            int posColumn = indexOfColumn(column);
            switch (posColumn) {
                case -1: // no such column here - let's add then
                    orders.add(0, new Order(column));
                    break;
                case 0: // found at top-level, should invert
                    orders.get(0).isSortAsc ^= true; // invert
                    break;
                default: // found somewhere, move down others, set this one onto top;
                    orders.remove(posColumn);
                    orders.add(0, new Order(column));
                    break;
            }
            if(orders.size() > MAX_DEPTH) { orders.remove(MAX_DEPTH); } 
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

    public TableModel(final CardPanelBase cd, final List<TableColumnInfo<T>> columnsToShow, Class<T> cls) {
        data = new ItemPool<T>(cls);
        cardDisplay = cd;
        columns = columnsToShow;
        columns.get(4).isMinMaxApplied = false;
    }


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

    public void clear() { data.clear(); }
    public ItemPoolView<T> getCards() { return data.getView(); }

    /**
     * <p>removeCard.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void removeCard(final T c) {
        boolean wasThere = data.count(c) > 0;
        if (wasThere) {
            data.remove(c);
            fireTableDataChanged();
        }
    }


    public void addCard(final T c) {
        data.add(c);
        fireTableDataChanged();
    }
    public void addCard(final T c, final int count) {
        data.add(c, count);
        fireTableDataChanged();
    }
    public void addCard(final Entry<T, Integer> e) {
        data.add(e.getKey(), e.getValue());
        fireTableDataChanged();
    }
    public void addCards(final Iterable<Entry<T, Integer>> c) {
        data.addAll(c);
        fireTableDataChanged();
    }
    public void addAllCards(final Iterable<T> c) {
        data.addAllCards(c);
        fireTableDataChanged();
    }

    public Entry<T, Integer> rowToCard(final int row) {
        List<Entry<T, Integer>> model = data.getOrderedList();
        return row >= 0 && row < model.size() ? model.get(row) : null;
    }
    public int getRowCount() {
        return data.countDistinct();
    }

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


    class ColumnListener extends MouseAdapter {
        protected JTable table;

        public ColumnListener(final JTable t) { table = t; }

        public void mouseClicked(final MouseEvent e) {
          TableColumnModel colModel = table.getColumnModel();
          int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
          int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();

          if (modelIndex < 0) { return; }

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

    public void showSelectedCard(final JTable table) {
        int row = table.getSelectedRow();
        if (row != -1) {
            T cp = rowToCard(row).getKey();
            cardDisplay.showCard(cp);
        }
    }

    /**
     * <p>addListeners.</p>
     *
     * @param table a {@link javax.swing.JTable} object.
     */
    public void addListeners(final JTable table) {
        //updates card detail, listens to any key strokes
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(final ListSelectionEvent arg0) {
                showSelectedCard(table);
            }
        });
        table.addFocusListener(new FocusListener() {

            @Override public void focusLost(final FocusEvent e) {}
            @Override public void focusGained(final FocusEvent e) {
                showSelectedCard(table);
            }
        });

        table.getTableHeader().addMouseListener(new ColumnListener(table));

    }//addCardListener()


    public void resort() {
        Collections.sort(data.getOrderedList(), sortOrders.getSorter());
    }

    public void sort( int iCol, boolean isAsc ) {
        sortOrders.add(iCol, isAsc);
        resort();
    }

}//CardTableModel
