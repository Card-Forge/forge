package forge.gui.deckeditor;


import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.slightlymagic.braids.util.lambda.Lambda1;

import forge.card.CardPool;
import forge.card.CardPoolView;
import forge.card.CardPrinted;

import java.awt.event.*;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;


/**
 * <p>TableModel class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class TableModel extends AbstractTableModel {
    /**
     * 
     */
    private static final long serialVersionUID = -6896726613116254828L;

    @SuppressWarnings("rawtypes") // We use raw comparables to provide fields for sorting 

    private final class SortOrders {
        private class Order {
            public final int sortColumn;
            public boolean isSortAsc = true;
            public Order(int col) { sortColumn = col; }
        };

        private final int MAX_DEPTH = 3;
        Order[] orders = new Order[] {null, null, null};
        private TableSorterCascade sorter = null;
        private boolean isSorterReady = false;
        private int indexOfColumn(final int column) {
            int posColumn = orders.length - 1;
            for (; posColumn >= 0; posColumn--) {
                if (orders[posColumn] != null && orders[posColumn].sortColumn == column) {
                    break;
                }
            }
            return posColumn;
        }

        // index of column to sort by, desired direction
        public void add(final int column, boolean wantAsc) {
            add(column);
            orders[0].isSortAsc = wantAsc;
            isSorterReady = false;
        }

        // puts desired direction on top, set "asc"; if already was on top, inverts direction; 
        public void add(final int column) {
            int posColumn = indexOfColumn(column);
            switch (posColumn) {
                case -1: // no such column here - let's add then
                    System.arraycopy(orders, 0, orders, 1, MAX_DEPTH - 1);
                    orders[0] = new Order(column);
                    break;
                case 0: // found at top-level, should invert
                    orders[0].isSortAsc ^= true; // invert
                    break;
                default: // found somewhere, move down others, set this one onto top;
                    System.arraycopy(orders, 0, orders, 1, posColumn);
                    orders[0] = new Order(column);
                    break;
            }
            isSorterReady = false;
        }

        public TableSorterCascade getSorter() {
            if (!isSorterReady) {
                TableSorter[] oneColSorters = new TableSorter[MAX_DEPTH];
                for (int i = 0; i < orders.length; i++) {
                    Order order = orders[i];
                    if (order == null) {
                        oneColSorters[i] = null;
                    } else {
                        oneColSorters[i] = new TableSorter(columns.get(order.sortColumn).fnSort, order.isSortAsc);
                    }
                }
                sorter = new TableSorterCascade(oneColSorters);
            }
            return sorter;
        }
    }

    private CardPool data = new CardPool();
    private final CardDisplay cardDisplay;
    private final List<TableColumnInfo<CardPrinted>> columns;
    private final SortOrders sortOrders = new SortOrders();

    public TableModel(final CardDisplay cd, List<TableColumnInfo<CardPrinted>> columnsToShow ) { 
        cardDisplay = cd;
        columns = columnsToShow;
        columns.get(4).isMinMaxApplied = false;
    }

    

    @SuppressWarnings("rawtypes")
    private final TableColumnInfo<CardPrinted> columnAI = new TableColumnInfo<CardPrinted>("AI", 30,
        new Lambda1<Comparable, Entry<CardPrinted, Integer>>() {
            @Override public Comparable apply(final Entry<CardPrinted, Integer> from) {
                return "n/a"; } },
        new Lambda1<Object, Entry<CardPrinted, Integer>>() {
            @Override public Object apply(final Entry<CardPrinted, Integer> from) {
                return "n/a"; } });

    /*
        columnQty, columnName, columnCost,
        columnColor, columnType, columnStats,
        columnRarity, columnSet, columnAI
    */


    public void resizeCols(final JTable table) {
        TableColumn tableColumn = null;
        for (int i = 0; i < table.getColumnCount(); i++) {
            tableColumn = table.getColumnModel().getColumn(i);
            TableColumnInfo<CardPrinted> colInfo = columns.get(i);
            
            tableColumn.setPreferredWidth(colInfo.nominalWidth);
            if (colInfo.isMinMaxApplied) {
                tableColumn.setMinWidth(colInfo.minWidth);
                tableColumn.setMaxWidth(colInfo.maxWidth);
            }
        }//for
    }

    public void clear() { data.clear(); }
    public CardPoolView getCards() { return data.getView(); }

    /**
     * <p>removeCard.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void removeCard(final CardPrinted c) {
        boolean wasThere = data.count(c) > 0;
        if (wasThere) {
            data.remove(c);
            fireTableDataChanged();
        }
    }


    public void addCard(final CardPrinted c) {
        data.add(c);
        fireTableDataChanged();
    }
    public void addCard(final CardPrinted c, final int count) {
        data.add(c, count);
        fireTableDataChanged();
    }
    public void addCard(final Entry<CardPrinted, Integer> e) {
        data.add(e.getKey(), e.getValue());
        fireTableDataChanged();
    }
    public void addCards(final Iterable<Entry<CardPrinted, Integer>> c) {
        data.addAll(c);
        fireTableDataChanged();
    }
    public void addAllCards(final Iterable<CardPrinted> c) {
        data.addAllCards(c);
        fireTableDataChanged();
    }

    public Entry<CardPrinted, Integer> rowToCard(final int row) {
        return data.getOrderedList().get(row);
    }
    public int getRowCount() {
        return data.countDistinct();
    }

    public int getColumnCount() {
        return columns.size();
    }

    /** {@inheritDoc} */
    @Override
    public String getColumnName(int n) {
        return columns.get(n).getName();
    }

    /** {@inheritDoc} */
    public Object getValueAt(int row, int column) {
        return columns.get(column).fnDisplay.apply(rowToCard(row));
    }


    class ColumnListener extends MouseAdapter {
        protected JTable table;

        public ColumnListener(JTable t) { table = t; }

        public void mouseClicked(MouseEvent e) {
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
    
    
    public void showSelectedCard(JTable table)
    {
        int row = table.getSelectedRow();
        if (row != -1) {
            CardPrinted cp = rowToCard(row).getKey();
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
            public void valueChanged(ListSelectionEvent arg0) {
                showSelectedCard(table);
            }
        });
        table.addFocusListener(new FocusListener() {
            
            @Override public void focusLost(FocusEvent e) {}
            @Override public void focusGained(FocusEvent e) {
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
