/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011
import forge.gui.deckeditor.views.VDeckEditorUI;
Forge Team
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

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
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
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.commons.lang3.ArrayUtils;

import forge.Card;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.SEditorIO;
import forge.gui.deckeditor.tables.SColumnUtil.ColumnName;
import forge.gui.deckeditor.tables.SColumnUtil.SortState;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;

/**
 * <p>
 * EditorTableModel class.
 * </p>
 * 
 * @param <T>
 *            the generic type
 * @author Forge
 * @version $Id$
 */
@SuppressWarnings("serial")
public final class EditorTableModel<T extends InventoryItem> extends AbstractTableModel {
    private final ItemPool<T> data;
    private final JTable table;
    private final CascadeManager cascadeManager = new CascadeManager();
    private final int maxSortDepth = 3;

    /**
     * Instantiates a new table model, using a JTable,
     * a column set, and a data set of generic type <T>.
     * 
     * @param table0 &emsp; {@link javax.swing.JTable}
     * @param class0 &emsp; Generic type <T>
     */
    public EditorTableModel(final JTable table0, final Class<T> class0) {
        this.table = table0;
        this.data = new ItemPool<T>(class0);
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
     * Clears all data in the model.
     */
    public void clear() {
        this.data.clear();
    }

    /**
     * Gets all cards in the model.
     * 
     * @return the cards
     */
    public ItemPoolView<T> getCards() {
        return this.data.getView();
    }

    /**
     * Removes a card from the model.
     * 
     * @param card0 &emsp; {@link forge.Card} object
     */
    public void removeCard(final T card0) {
        final boolean wasThere = this.data.count(card0) > 0;
        if (wasThere) {
            this.data.remove(card0);
            this.fireTableDataChanged();
        }
    }

    /**
     * Adds a card to the model.
     * 
     * @param card0 &emsp; {@link forge.Card} object.
     */
    public void addCard(final T card0) {
        this.data.add(card0);
        this.fireTableDataChanged();
    }

    /**
     * Adds multiple copies of multiple cards to the model.
     * 
     * @param cards0 &emsp; {@link java.lang.Iterable}<Entry<T, Integer>>
     */
    public void addCards(final Iterable<Entry<T, Integer>> cards0) {
        this.data.addAll(cards0);
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
            if (cp instanceof CardPrinted) {
                CDeckEditorUI.SINGLETON_INSTANCE.setCard(((CardPrinted) cp).getMatchingForgeCard());
            }
            else if (cp != null) {
                CDeckEditorUI.SINGLETON_INSTANCE.setCard(cp);
            }
            else {
                CDeckEditorUI.SINGLETON_INSTANCE.setCard((Card)null);
            }
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
                EditorTableModel.this.showSelectedCard(table);
            }
        });
        table.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(final FocusEvent e) {
            }

            @Override
            public void focusGained(final FocusEvent e) {
                EditorTableModel.this.showSelectedCard(table);
            }
        });

        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                headerClicked(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                SEditorIO.savePreferences();
            }
        });
    } // addCardListener()

    /**
     * Resort.
     */
    public void refreshSort() {
        if (this.data.getOrderedList().size() == 0) { return; }

        Collections.sort(this.data.getOrderedList(), new MyComparator());
    }

    @SuppressWarnings("unchecked")
    private void headerClicked(final MouseEvent e) {
        final TableColumnModel colModel = EditorTableModel.this.table.getColumnModel();
        final int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
        final int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();

        if (modelIndex < 0) {
            return;
        }

        // This will invert if needed
        // 2012/07/21 - Changed from modelIndex to ColumnModelIndex due to a crash
        // Crash was: Hide 2 columns, then search by last column.
        EditorTableModel.this.cascadeManager.add((TableColumnInfo<T>) this.table.getColumnModel().getColumn(columnModelIndex));
        EditorTableModel.this.refreshSort();
        EditorTableModel.this.table.tableChanged(new TableModelEvent(EditorTableModel.this));
        EditorTableModel.this.table.repaint();
        if (EditorTableModel.this.table.getRowCount() > 0) {
            EditorTableModel.this.table.setRowSelectionInterval(0, 0);
        }
        SEditorIO.savePreferences();
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
        return this.data.countDistinct();
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object getValueAt(int iRow, int iCol) {
        return ((TableColumnInfo<InventoryItem>) table.getColumnModel().getColumn(table.convertColumnIndexToView(iCol))).getFnDisplay().apply((Entry<InventoryItem, Integer>) this.rowToCard(iRow));
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

            final List<TableSorter<InventoryItem>> oneColSorters
                = new ArrayList<TableSorter<InventoryItem>>(maxSortDepth);

            for (final TableColumnInfo<InventoryItem> col : this.colsToSort) {
                oneColSorters.add(new TableSorter<InventoryItem>(
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
            return EditorTableModel.this.cascadeManager.getSorter().compare(
                    (Entry<InventoryItem, Integer>) o1, (Entry<InventoryItem, Integer>) o2);
        }
    }
} // CardTableModel
