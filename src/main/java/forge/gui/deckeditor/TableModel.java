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
            private final int sortColumn;
            private boolean isSortAsc = true;

            public Order(final int col) {
                this.sortColumn = col;
            }
        };

        private final int maxDepth = 3;
        private final List<Order> orders = new ArrayList<Order>(3);
        private TableSorterCascade<T> sorter = null;
        private boolean isSorterReady = false;

        private int indexOfColumn(final int column) {
            int posColumn = this.orders.size() - 1;
            for (; posColumn >= 0; posColumn--) {
                if ((this.orders.get(posColumn) != null) && (this.orders.get(posColumn).sortColumn == column)) {
                    break;
                }
            }
            return posColumn;
        }

        // index of column to sort by, desired direction
        public void add(final int column, final boolean wantAsc) {
            this.add(column);
            this.orders.get(0).isSortAsc = wantAsc;
            this.isSorterReady = false;
        }

        // puts desired direction on top, set "asc"; if already was on top,
        // inverts direction;
        public void add(final int column) {
            final int posColumn = this.indexOfColumn(column);
            switch (posColumn) {
            case -1: // no such column here - let's add then
                this.orders.add(0, new Order(column));
                break;
            case 0: // found at top-level, should invert
                this.orders.get(0).isSortAsc ^= true; // invert
                break;
            default: // found somewhere, move down others, set this one onto
                     // top;
                this.orders.remove(posColumn);
                this.orders.add(0, new Order(column));
                break;
            }
            if (this.orders.size() > this.maxDepth) {
                this.orders.remove(this.maxDepth);
            }
            this.isSorterReady = false;
        }

        public TableSorterCascade<T> getSorter() {
            if (!this.isSorterReady) {
                final List<TableSorter<T>> oneColSorters = new ArrayList<TableSorter<T>>(this.maxDepth);
                for (final Order order : this.orders) {
                    oneColSorters.add(new TableSorter<T>(TableModel.this.columns.get(order.sortColumn).getFnSort(),
                            order.isSortAsc));
                }
                this.sorter = new TableSorterCascade<T>(oneColSorters);
            }
            return this.sorter;
        }
    }

    private final ItemPool<T> data;
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
        this.data = new ItemPool<T>(cls);
        this.cardDisplay = cd;
        this.columns = columnsToShow;
        this.columns.get(4).setMinMaxApplied(false);
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
            final TableColumnInfo<T> colInfo = this.columns.get(i);

            tableColumn.setPreferredWidth(colInfo.getNominalWidth());
            if (colInfo.isMinMaxApplied()) {
                tableColumn.setMinWidth(colInfo.getMinWidth());
                tableColumn.setMaxWidth(colInfo.getMaxWidth());
            }
        }
    }

    /**
     * Clear.
     */
    public void clear() {
        this.data.clear();
    }

    /**
     * Gets the cards.
     * 
     * @return the cards
     */
    public ItemPoolView<T> getCards() {
        return this.data.getView();
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
        final boolean wasThere = this.data.count(c) > 0;
        if (wasThere) {
            this.data.remove(c);
            this.fireTableDataChanged();
        }
    }

    /**
     * Adds the card.
     * 
     * @param c
     *            the c
     */
    public void addCard(final T c) {
        this.data.add(c);
        this.fireTableDataChanged();
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
        this.data.add(c, count);
        this.fireTableDataChanged();
    }

    /**
     * Adds the card.
     * 
     * @param e
     *            the e
     */
    public void addCard(final Entry<T, Integer> e) {
        this.data.add(e.getKey(), e.getValue());
        this.fireTableDataChanged();
    }

    /**
     * Adds the cards.
     * 
     * @param c
     *            the c
     */
    public void addCards(final Iterable<Entry<T, Integer>> c) {
        this.data.addAll(c);
        this.fireTableDataChanged();
    }

    /**
     * Adds the all cards.
     * 
     * @param c
     *            the c
     */
    public void addAllCards(final Iterable<T> c) {
        this.data.addAllCards(c);
        this.fireTableDataChanged();
    }

    /**
     * Row to card.
     * 
     * @param row
     *            the row
     * @return the entry
     */
    public Entry<T, Integer> rowToCard(final int row) {
        final List<Entry<T, Integer>> model = this.data.getOrderedList();
        return (row >= 0) && (row < model.size()) ? model.get(row) : null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.table.TableModel#getRowCount()
     */
    /**
     * Gets the row count.
     * 
     * @return int
     */
    @Override
    public int getRowCount() {
        return this.data.countDistinct();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    /**
     * Gets the column count.
     * 
     * @return int
     */
    @Override
    public int getColumnCount() {
        return this.columns.size();
    }

    /** {@inheritDoc} */
    @Override
    public String getColumnName(final int n) {
        return this.columns.get(n).getName();
    }

    /** {@inheritDoc} */
    @Override
    public Object getValueAt(final int row, final int column) {
        return this.columns.get(column).getFnDisplay().apply(this.rowToCard(row));
    }

    /**
     * The listener interface for receiving column events. The class that is
     * interested in processing a column event implements this interface, and
     * the object created with that class is registered with a component using
     * the component's addColumnListener method. When the column event occurs,
     * that object's appropriate method is invoked.
     * 
     */
    class ColumnListener extends MouseAdapter {

        /** The table. */
        private final JTable table;

        /**
         * Instantiates a new column listener.
         * 
         * @param t
         *            the t
         */
        public ColumnListener(final JTable t) {
            this.table = t;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
         */
        /**
         * Mouse clicked.
         * 
         * @param e
         *            MouseEvent
         */
        @Override
        public void mouseClicked(final MouseEvent e) {
            final TableColumnModel colModel = this.table.getColumnModel();
            final int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
            final int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();

            if (modelIndex < 0) {
                return;
            }

            // This will invert if needed
            TableModel.this.sortOrders.add(modelIndex);

            for (int i = 0; i < TableModel.this.columns.size(); i++) {
                final TableColumn column = colModel.getColumn(i);
                column.setHeaderValue(TableModel.this.getColumnName(column.getModelIndex()));
            }
            this.table.getTableHeader().repaint();

            TableModel.this.resort();
            this.table.tableChanged(new TableModelEvent(TableModel.this));
            this.table.repaint();
        }
    }

    /**
     * Show selected card.
     * 
     * @param table
     *            the table
     */
    public void showSelectedCard(final JTable table) {
        final int row = table.getSelectedRow();
        if (row != -1) {
            final T cp = this.rowToCard(row).getKey();
            this.cardDisplay.showCard(cp);
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
                TableModel.this.showSelectedCard(table);
            }
        });
        table.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(final FocusEvent e) {
            }

            @Override
            public void focusGained(final FocusEvent e) {
                TableModel.this.showSelectedCard(table);
            }
        });

        table.getTableHeader().addMouseListener(new ColumnListener(table));

    } // addCardListener()

    /**
     * Resort.
     */
    public void resort() {
        Collections.sort(this.data.getOrderedList(), this.sortOrders.getSorter());
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
        this.sortOrders.add(iCol, isAsc);
        this.resort();
    }

} // CardTableModel
