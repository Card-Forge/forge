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
package forge.gui.toolbox.itemmanager.views;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.commons.lang3.ArrayUtils;

import forge.gui.toolbox.FMouseAdapter;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.ItemManagerModel;
import forge.gui.toolbox.itemmanager.SItemManagerIO;
import forge.gui.toolbox.itemmanager.views.SColumnUtil.ColumnName;
import forge.gui.toolbox.itemmanager.views.SColumnUtil.SortState;
import forge.item.InventoryItem;
import forge.util.ItemPoolSorter;


/**
 * ItemTable.
 * 
 * @param <T>
 *            the generic type
 */
@SuppressWarnings("serial")
public final class ItemListView<T extends InventoryItem> extends ItemView<T> {
    private final ItemTable table = new ItemTable();
    private final FSkin.JTableSkin<ItemTable> skin;
    private final ItemTableModel tableModel;

    public ItemTableModel getTableModel() {
        return this.tableModel;
    }

    /**
     * ItemTable Constructor.
     * 
     * @param itemManager0
     * @param model0
     */
    public ItemListView(ItemManager<T> itemManager0, ItemManagerModel<T> model0) {
        super(itemManager0);
        this.tableModel = new ItemTableModel(model0);

        // use different selection highlight colors for focused vs. unfocused tables
        this.skin = FSkin.get(this.table);
        this.skin.setSelectionBackground(FSkin.getColor(FSkin.Colors.CLR_INACTIVE));
        this.skin.setSelectionForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.table.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                if (!e.isTemporary() && !skin.isDisposed()) {
                    skin.setSelectionBackground(FSkin.getColor(FSkin.Colors.CLR_INACTIVE));
                }
            }

            @Override
            public void focusGained(FocusEvent e) {
                skin.setSelectionBackground(FSkin.getColor(FSkin.Colors.CLR_ACTIVE));
                // if nothing selected when we gain focus, select the first row (if exists)
                if (-1 == getSelectedIndex() && getCount() > 0) {
                    table.setRowSelectionInterval(0, 0);
                }
            }
        });

        this.skin.setFont(FSkin.getFont(12));
        this.table.setBorder(null);
        this.table.getTableHeader().setBorder(null);
        this.table.setRowHeight(18);
        setWantElasticColumns(false);

        // prevent tables from intercepting tab focus traversals
        this.table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        this.table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
    }

    /**
     * Applies a EditorTableModel and a model listener to this instance's JTable.
     * 
     * @param cols &emsp; List<TableColumnInfo<InventoryItem>> of additional columns for this
     */
    public void setup(final List<TableColumnInfo<InventoryItem>> cols) {
        final DefaultTableColumnModel colmodel = new DefaultTableColumnModel();

        //ensure columns ordered properly
        Collections.sort(cols, new Comparator<TableColumnInfo<InventoryItem>>() {
            @Override
            public int compare(TableColumnInfo<InventoryItem> arg0, TableColumnInfo<InventoryItem> arg1) {
                return Integer.compare(arg0.getIndex(), arg1.getIndex());
            }
        });

        for (TableColumnInfo<InventoryItem> item : cols) {
            item.setModelIndex(colmodel.getColumnCount());
            if (item.isShowing()) { colmodel.addColumn(item); }
        }

        this.tableModel.addListeners();
        this.table.setModel(this.tableModel);
        this.table.setColumnModel(colmodel);

        this.tableModel.setup();
        this.tableModel.refreshSort();

        this.table.getTableHeader().setBackground(new Color(200, 200, 200));
    }

    public void setAvailableColumns(final List<TableColumnInfo<InventoryItem>> cols) {
        final DefaultTableColumnModel colModel = new DefaultTableColumnModel();

        for (TableColumnInfo<InventoryItem> item : cols) {
            item.setModelIndex(colModel.getColumnCount());
            if (item.isShowing()) { colModel.addColumn(item); }
        }

        table.setColumnModel(colModel);
    }

    public JTable getTable() {
        return this.table;
    }

    @Override
    public JComponent getComponent() {
        return this.table;
    }

    @Override
    public Point getLocationOnScreen() {
        return this.table.getTableHeader().getLocationOnScreen(); //use table header's location since that stays in place
    }

    @Override
    protected String getCaption() {
        return "List View";
    }

    @Override
    public void setAllowMultipleSelections(boolean allowMultipleSelections) {
        this.table.setSelectionMode(allowMultipleSelections ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
    }

    @Override
    public int getSelectedIndex() {
        return this.table.getSelectedRow();
    }

    @Override
    public Iterable<Integer> getSelectedIndices() {
        List<Integer> indices = new ArrayList<Integer>();
        int[] selectedRows = this.table.getSelectedRows();
        for (int i = 0; i < selectedRows.length; i++) {
            indices.add(selectedRows[i]);
        }
        return indices;
    }

    @Override
    protected void onSetSelectedIndex(int index) {
        int count = getCount();
        if (count == 0) { return; }

        if (index >= count) {
            index = count - 1;
        }
        this.table.setRowSelectionInterval(index, index);
    }

    @Override
    protected void onSetSelectedIndices(Iterable<Integer> indices) {
        int count = getCount();
        if (count == 0) { return; }

        this.table.clearSelection();
        for (Integer index : indices) {
            if (index >= count) {
                index = count - 1;
            }
            this.table.addRowSelectionInterval(index, index);
        }
    }

    @Override
    protected void onScrollSelectionIntoView(JViewport viewport) {
        // compute where we're going and where we are
        Rectangle targetRect  = this.table.getCellRect(this.getSelectedIndex(), 0, true);
        Rectangle curViewRect = viewport.getViewRect();

        // if the target cell is not visible, attempt to jump to a location where it is
        // visible but not on the edge of the viewport
        if (targetRect.y + targetRect.height > curViewRect.y + curViewRect.height) {
            // target is below us, move to position 3 rows below target
            targetRect.setLocation(targetRect.x, targetRect.y + (targetRect.height * 3));
        }
        else if  (targetRect.y < curViewRect.y) {
            // target is above is, move to position 3 rows above target
            targetRect.setLocation(targetRect.x, targetRect.y - (targetRect.height * 3));
        }

        this.table.scrollRectToVisible(targetRect);
    }

    @Override
    public void selectAll() {
        this.table.selectAll();
    }

    @Override
    public int getIndexOfItem(T item) {
        return this.tableModel.itemToRow(item);
    }

    @Override
    public T getItemAtIndex(int index) {
        return this.tableModel.rowToItem(index).getKey();
    }

    @Override
    public int getCount() {
        return this.table.getRowCount();
    }

    @Override
    public int getSelectionCount() {
        return this.table.getSelectedRowCount();
    }

    @Override
    public int getIndexAtPoint(Point p) {
        return this.table.rowAtPoint(p);
    }

    public void setWantElasticColumns(boolean value) {
        this.table.setAutoResizeMode(value ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS : JTable.AUTO_RESIZE_OFF);
    }

    public final class ItemTable extends JTable {
        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new JTableHeader(columnModel) {
                public String getToolTipText(MouseEvent e) {
                    int col = columnModel.getColumnIndexAtX(e.getPoint().x);
                    if (col < 0) { return null; }
                    TableColumn tableColumn = columnModel.getColumn(col);
                    TableCellRenderer headerRenderer = tableColumn.getHeaderRenderer();
                    if (headerRenderer == null) {
                        headerRenderer = getDefaultRenderer();
                    }

                    return getCellTooltip(headerRenderer, -1, col, tableColumn.getHeaderValue());
                }
            };
        }

        public void processMouseEvent(MouseEvent e) {
            Point p = e.getPoint();
            int row = rowAtPoint(p);
            int col = columnAtPoint(p);

            if (col < 0 || col >= getColumnCount() || row < 0 || row >= getRowCount()) {
                return;
            }

            Object val = getValueAt(row, col);
            if (val == null) {
                return;
            }

            ItemCellRenderer renderer = (ItemCellRenderer)getCellRenderer(row, col);
            if (renderer != null) {
                renderer.processMouseEvent(e, ItemListView.this, val, row, col); //give renderer a chance to process the mouse event
            }
            super.processMouseEvent(e);
        }

        private String getCellTooltip(TableCellRenderer renderer, int row, int col, Object val) {
            Component cell = renderer.getTableCellRendererComponent(this, val, false, false, row, col);

            // if we're conditionally showing the tooltip, check to see
            // if we shouldn't show it
            if (!(renderer instanceof ItemCellRenderer) || !((ItemCellRenderer)renderer).alwaysShowTooltip()) {
                // if there's enough room (or there's no value), no tooltip
                // we use '>' here instead of '>=' since that seems to be the
                // threshold for where the ellipses appear for the default
                // JTable renderer
                int requiredWidth = cell.getPreferredSize().width;
                TableColumn tableColumn = this.getColumnModel().getColumn(col);
                if (null == val || tableColumn.getWidth() > requiredWidth) {
                    return null;
                }
            }

            // use a pre-set tooltip if it exists
            if (cell instanceof JComponent) {
                JComponent jcell = (JComponent)cell;
                String tip = jcell.getToolTipText();
                if (tip != null) {
                    return tip;
                }
            }

            // otherwise, show the full text in the tooltip
            return String.valueOf(val);
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            Point p = e.getPoint();
            int row = rowAtPoint(p);
            int col = columnAtPoint(p);

            if (col >= getColumnCount() || row >= getRowCount()) {
                return null;
            }

            Object val = getValueAt(row, col);
            if (val == null) {
                return null;
            }

            return getCellTooltip(getCellRenderer(row, col), row, col, val);
        }

        private int   lastTooltipRow = -1;
        private int   lastTooltipCol = -1;
        private Point lastTooltipPt;

        @Override
        public Point getToolTipLocation(MouseEvent e) {
            Point p = e.getPoint();
            final int row = rowAtPoint(p);
            final int col = columnAtPoint(p);
            if (row == lastTooltipRow && col == lastTooltipCol) {
                p = lastTooltipPt;
            }
            else {
                lastTooltipRow = row;
                lastTooltipCol = col;
                lastTooltipPt  = p;
            }
            return new Point(p.x + 10, p.y + 20);
        }
    }

    public final class ItemTableModel extends AbstractTableModel {
        private final ItemManagerModel<T> model;
        private final CascadeManager cascadeManager = new CascadeManager();
        private final int maxSortDepth = 3;

        /**
         * Instantiates a new table model.
         * 
         * @param table0 &emsp; {@link forge.gui.ItemManager.ItemTable<T>}
         * @param model0 &emsp; {@link forge.gui.ItemManager.ItemManagerModel<T>}
         */
        public ItemTableModel(final ItemManagerModel<T> model0) {
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

            cascadeManager.reset();

            if (sortcols[1] == null) {
                if (isDeckTable) {
                    cascadeManager.add((TableColumnInfo<T>) SColumnUtil.getColumn(ColumnName.DECK_NAME), true);
                }
                else {
                    cascadeManager.add((TableColumnInfo<T>) SColumnUtil.getColumn(ColumnName.CAT_NAME), true);
                }
            }
            else {
                ArrayUtils.reverse(sortcols);
                for (int i = 1; i < sortcols.length; i++) {
                    if (sortcols[i] != null) {
                        cascadeManager.add((TableColumnInfo<T>) sortcols[i], true);
                    }
                }
            }
        }

        /**
         * Row to item.
         * 
         * @param row - the row
         * @return the item
         */
        public Entry<T, Integer> rowToItem(final int row) {
            final List<Entry<T, Integer>> orderedList = this.model.getOrderedList();
            return (row >= 0) && (row < orderedList.size()) ? orderedList.get(row) : null;
        }

        /**
         * Item to row.
         * 
         * @param item - the item
         * @return the row
         */
        public int itemToRow(final T item) { //TODO: Consider optimizing this if used frequently
            final List<Entry<T, Integer>> orderedList = this.model.getOrderedList();
            for (int i = 0; i < orderedList.size(); i++) {
                if (orderedList.get(i).getKey() == item) {
                    return i;
                }
            }
            return -1;
        }

        public void onSelectionChange() {
            final int row = getSelectedIndex();
            if (row != -1) {
                ListSelectionEvent event = new ListSelectionEvent(getItemManager(), row, row, false);
                for (ListSelectionListener listener : getItemManager().getSelectionListeners()) {
                    listener.valueChanged(event);
                }
            }
        }

        private final ListSelectionListener listSelectionListener = new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent arg0) {
                ItemTableModel.this.onSelectionChange();
            }
        };

        private final FocusAdapter focusAdapter = new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                ItemTableModel.this.onSelectionChange();
            }
        };

        private final FMouseAdapter headerMouseAdapter = new FMouseAdapter(true) {
            @SuppressWarnings("unchecked")
            @Override
            public void onLeftClick(MouseEvent e) {
                if (Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR) == table.getTableHeader().getCursor()) {
                    return;
                }

                //toggle column sort
                final TableColumnModel colModel = table.getColumnModel();
                final int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
                final int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();

                if (modelIndex < 0) {
                    return;
                }

                // This will invert if needed
                // 2012/07/21 - Changed from modelIndex to ColumnModelIndex due to a crash
                // Crash was: Hide 2 columns, then search by last column.
                ItemTableModel.this.cascadeManager.add((TableColumnInfo<T>) table.getColumnModel().getColumn(columnModelIndex), false);
                ItemTableModel.this.refreshSort();
                table.tableChanged(new TableModelEvent(ItemTableModel.this));
                table.repaint();
                if (getCount() > 0) {
                    table.setRowSelectionInterval(0, 0);
                }
                SItemManagerIO.savePreferences(getItemManager());
            }

            @Override
            public void onLeftMouseDragDrop(MouseEvent e) { //save preferences after column moved/resized
                SItemManagerIO.savePreferences(getItemManager());
            }
        };

        /**
         * <p>
         * addListeners.
         * </p>
         */
        public void addListeners() {
            // updates card detail, listens to any key strokes
            table.getSelectionModel().removeListSelectionListener(listSelectionListener);  //ensure listener not added multiple times
            table.getSelectionModel().addListSelectionListener(listSelectionListener);

            table.removeFocusListener(focusAdapter); //ensure listener not added multiple times
            table.addFocusListener(focusAdapter);

            table.getTableHeader().removeMouseListener(headerMouseAdapter); //ensure listener not added multiple times
            table.getTableHeader().addMouseListener(headerMouseAdapter);
        }

        /**
         * Resort.
         */
        public void refreshSort() {
            if (this.model.getOrderedList().size() == 0) { return; }

            Collections.sort(this.model.getOrderedList(), new MyComparator());
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
            public void add(final TableColumnInfo<T> col0, boolean forSetup) {
                this.sorter = null;

                if (forSetup) { //just add column unmodified if setting up sort columns
                    this.colsToSort.add(0, (TableColumnInfo<InventoryItem>) col0);
                }
                else {
                    if (colsToSort.size() > 0 && colsToSort.get(0).equals(col0)) { //if column already at top level, just invert
                        col0.setSortPriority(1);
                        col0.setSortState(col0.getSortState() == SortState.ASC ? SortState.DESC : SortState.ASC);
                    }
                    else { //otherwise move column to top level and move others down
                        this.colsToSort.remove(col0);
                        col0.setSortPriority(1);
                        col0.setSortState(col0.getDefaultSortState());
                        this.colsToSort.add(0, (TableColumnInfo<InventoryItem>) col0);
                    }

                    //decrement sort priority on remaining columns
                    for (int i = 1; i < maxSortDepth; i++) {
                        if (colsToSort.size() == i) { break; }

                        if (colsToSort.get(i).getSortPriority() != 0) {
                            colsToSort.get(i).setSortPriority(i + 1);
                        }
                    }
                }

                //unset and remove boundary columns.
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

            public void reset() {
                this.colsToSort.clear();
                this.sorter = null;
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
    }
}
