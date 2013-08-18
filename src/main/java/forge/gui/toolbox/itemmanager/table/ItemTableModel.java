/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011
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
package forge.gui.toolbox.itemmanager.table;

import java.awt.Cursor;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.commons.lang3.ArrayUtils;

import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.toolbox.itemmanager.ItemManagerModel;
import forge.gui.toolbox.itemmanager.SItemManagerIO;
import forge.gui.toolbox.itemmanager.table.SColumnUtil.ColumnName;
import forge.gui.toolbox.itemmanager.table.SColumnUtil.SortState;
import forge.item.ItemPoolSorter;
import forge.item.InventoryItem;

/**
 * <p>
 * ItemTableModel class.
 * </p>
 * 
 * @param <T>
 *            the generic type
 * @author Forge
 * @version $Id: ItemTableModel.java 19857 2013-02-24 08:49:52Z Max mtg $
 */
@SuppressWarnings("serial")
public final class ItemTableModel<T extends InventoryItem> extends AbstractTableModel {
    private final ItemTable<T> table;
    private final ItemManagerModel<T> model;
    private final CascadeManager cascadeManager = new CascadeManager();
    private final int maxSortDepth = 3;

    /**
     * Instantiates a new table model.
     * 
     * @param table0 &emsp; {@link forge.gui.ItemManager.ItemTable<T>}
     * @param model0 &emsp; {@link forge.gui.ItemManager.ItemManagerModel<T>}
     */
    public ItemTableModel(final ItemTable<T> table0, final ItemManagerModel<T> model0) {
        this.table = table0;
        this.model = model0;
    }

    /** */
    @SuppressWarnings("unchecked")
    public void setup() {
        final Enumeration<TableColumn> e = table.getColumnModel().getColumns();
        final TableColumn[] sortcols = new TableColumn[table.getColumnCount()];

        // Assemble priority sort.
        while (e.hasMoreElements()) {
            final TableColumnInfo<InventoryItem> col = (TableColumnInfo<InventoryItem>) e.nextElement();
            if (col.getSortPriority() > 0) {
                sortcols[col.getSortPriority()] = col;
            }
        }

        final boolean isDeckTable = ((TableColumnInfo<InventoryItem>) table.getColumnModel()
                .getColumn(0)).getEnumValue().substring(0, 4).equals("DECK")
                    ? true : false;

        if (sortcols[1] == null) {
            if (isDeckTable) {
                cascadeManager.add((TableColumnInfo<T>) SColumnUtil.getColumn(ColumnName.DECK_NAME));
            }
            else {
                cascadeManager.add((TableColumnInfo<T>) SColumnUtil.getColumn(ColumnName.CAT_NAME));
            }
        }
        else {
            ArrayUtils.reverse(sortcols);
            for (int i = 1; i < sortcols.length; i++) {
                if (sortcols[i] != null) {
                    cascadeManager.add((TableColumnInfo<T>) sortcols[i]);
                }
            }
        }
    }

    /**
     * Row to card.
     * 
     * @param row
     *            the row
     * @return the entry
     */
    public Entry<T, Integer> rowToItem(final int row) {
        final List<Entry<T, Integer>> model = this.model.getOrderedList();
        return (row >= 0) && (row < model.size()) ? model.get(row) : null;
    }

    /**
     * Show selected card.
     * 
     * @param table
     *            the table
     */
    public void showSelectedItem(final JTable table) {
        final int row = table.getSelectedRow();
        if (row != -1) {
            Entry<T, Integer> card = this.rowToItem(row);
            CDeckEditorUI.SINGLETON_INSTANCE.setCard(null != card ? card.getKey() : null);
        }
    }

    /**
     * <p>
     * addListeners.
     * </p>
     */
    public void addListeners() {
        // updates card detail, listens to any key strokes
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent arg0) {
                if (table.isFocusOwner()) {
                    ItemTableModel.this.showSelectedItem(table);
                }
            }
        });
        
        table.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                ItemTableModel.this.showSelectedItem(table);
            }
        });

        final JTableHeader header = table.getTableHeader();
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR) != header.getCursor()) {
                    headerClicked(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                SItemManagerIO.savePreferences(ItemTableModel.this.table);
            }
        });
    } // addItemListener()

    /**
     * Resort.
     */
    public void refreshSort() {
        if (this.model.getOrderedList().size() == 0) { return; }

        Collections.sort(this.model.getOrderedList(), new MyComparator());
    }

    @SuppressWarnings("unchecked")
    private void headerClicked(final MouseEvent e) {
        final TableColumnModel colModel = ItemTableModel.this.table.getColumnModel();
        final int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
        final int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();

        if (modelIndex < 0) {
            return;
        }

        // This will invert if needed
        // 2012/07/21 - Changed from modelIndex to ColumnModelIndex due to a crash
        // Crash was: Hide 2 columns, then search by last column.
        ItemTableModel.this.cascadeManager.add((TableColumnInfo<T>) this.table.getColumnModel().getColumn(columnModelIndex));
        ItemTableModel.this.refreshSort();
        ItemTableModel.this.table.tableChanged(new TableModelEvent(ItemTableModel.this));
        ItemTableModel.this.table.repaint();
        if (ItemTableModel.this.table.getRowCount() > 0) {
            ItemTableModel.this.table.setRowSelectionInterval(0, 0);
        }
        SItemManagerIO.savePreferences(ItemTableModel.this.table);
    }

    //========== Overridden from AbstractTableModel
    /** {@inheritDoc} */
    @Override
    public int findColumn(final String name0) {
        return table.getColumnModel().getColumnIndex(name0);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    @Override
    public int getColumnCount() {
        return table.getColumnCount();
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getRowCount()
     */
    @Override
    public int getRowCount() {
        return this.model.countDistinct();
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object getValueAt(int iRow, int iCol) {
        Entry<T, Integer> card = this.rowToItem(iRow);
        if (null == card) {
            return null;
        }
        return ((TableColumnInfo<T>) table.getColumnModel().getColumn(table.convertColumnIndexToView(iCol))).getFnDisplay().apply(card);
    }

    //========= Custom class handling

    /**
     * Manages sorting orders for multiple depths of sorting.
     */
    private final class CascadeManager {
        private final List<TableColumnInfo<InventoryItem>> colsToSort = new ArrayList<TableColumnInfo<InventoryItem>>(3);
        private TableSorterCascade<InventoryItem> sorter = null;

        // Adds a column to sort cascade list.
        // If column is first in the cascade, inverts direction of sort.
        // Otherwise, sorts in ascending direction.
        @SuppressWarnings("unchecked")
        public void add(final TableColumnInfo<T> col0) {
            this.sorter = null;

            // Found at top level, should invert
            if (colsToSort.size() > 0 && colsToSort.get(0).equals(col0)) {
                this.colsToSort.get(0).setSortState(
                        this.colsToSort.get(0).getSortState() == SortState.ASC
                            ? SortState.DESC : SortState.ASC);
                this.colsToSort.get(0).setSortPriority(1);
            }
            // Found somewhere: move down others, this one to top.
            else if (colsToSort.contains(col0)) {
                col0.setSortState(SortState.ASC);
                this.colsToSort.remove(col0);
                this.colsToSort.add(0, (TableColumnInfo<InventoryItem>) col0);
            }
            // No column in list; add directly.
            else {
                col0.setSortState(SortState.ASC);
                this.colsToSort.add(0, (TableColumnInfo<InventoryItem>) col0);
                this.colsToSort.get(0).setSortPriority(1);
            }

            // Decrement sort priority on remaining columns
            for (int i = 1; i < maxSortDepth; i++) {
                if (colsToSort.size() == i) { break; }

                if (colsToSort.get(i).getSortPriority() != 0) {
                    colsToSort.get(i).setSortPriority(i + 1);
                }
            }

            // Unset and remove boundary columns.
            if (this.colsToSort.size() > maxSortDepth) {
                this.colsToSort.get(maxSortDepth).setSortState(SortState.NONE);
                this.colsToSort.get(maxSortDepth).setSortPriority(0);
                this.colsToSort.remove(maxSortDepth);
            }
        }

        public TableSorterCascade<InventoryItem> getSorter() {
            if (this.sorter == null) {
                this.sorter = createSorter();
            }
            return this.sorter;
        }

        private TableSorterCascade<InventoryItem> createSorter() {

            final List<ItemPoolSorter<InventoryItem>> oneColSorters
                = new ArrayList<ItemPoolSorter<InventoryItem>>(maxSortDepth);

            for (final TableColumnInfo<InventoryItem> col : this.colsToSort) {
                oneColSorters.add(new ItemPoolSorter<InventoryItem>(
                        col.getFnSort(),
                        col.getSortState().equals(SortState.ASC) ? true : false));
            }

            return new TableSorterCascade<InventoryItem>(oneColSorters);
        }
    }

    private class MyComparator implements Comparator<Entry<T, Integer>> {
        /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @SuppressWarnings("unchecked")
        @Override
        public int compare(Entry<T, Integer> o1, Entry<T, Integer> o2) {
            return ItemTableModel.this.cascadeManager.getSorter().compare(
                    (Entry<InventoryItem, Integer>) o1, (Entry<InventoryItem, Integer>) o2);
        }
    }
} // ItemTableModel
