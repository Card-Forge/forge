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
package forge.itemmanager.views;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.commons.lang3.StringUtils;

import forge.assets.FSkinProp;
import forge.gui.MouseUtil;
import forge.item.InventoryItem;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemColumnConfig;
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerModel;
import forge.toolbox.FCheckBox;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinBorder;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinFont;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.FSkin.SkinnedTable;
import forge.toolbox.FSkin.SkinnedTableHeader;


/**
 * ItemTable.
 *
 * @param <T>
 *            the generic type
 */
@SuppressWarnings("serial")
public final class ItemListView<T extends InventoryItem> extends ItemView<T> {
    static final SkinColor BACK_COLOR = FSkin.getColor(FSkin.Colors.CLR_ZEBRA);
    private static final SkinColor FORE_COLOR = FSkin.getColor(FSkin.Colors.CLR_TEXT);
    private static final SkinColor SEL_ACTIVE_COLOR = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);
    private static final SkinColor SEL_INACTIVE_COLOR = FSkin.getColor(FSkin.Colors.CLR_INACTIVE);
    private static final SkinColor HEADER_BACK_COLOR = BACK_COLOR.getContrastColor(-10);
    static final SkinColor ALT_ROW_COLOR = BACK_COLOR.getContrastColor(-20);
    private static final SkinColor GRID_COLOR = BACK_COLOR.getContrastColor(20);
    private static final SkinBorder HEADER_BORDER = new FSkin.CompoundSkinBorder(new FSkin.MatteSkinBorder(0, 0, 1, 1, GRID_COLOR), new EmptyBorder(0, 1, 0, 0));
    private static final SkinFont ROW_FONT = FSkin.getFont(12);
    private static final int ROW_HEIGHT = 19;

    private final ItemTable table = new ItemTable();
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
    public ItemListView(final ItemManager<T> itemManager0, final ItemManagerModel<T> model0) {
        super(itemManager0, model0);
        this.tableModel = new ItemTableModel(model0);
        this.setAllowMultipleSelections(false);
        this.getPnlOptions().setVisible(false); //hide options panel by default

        // use different selection highlight colors for focused vs. unfocused tables
        this.table.addMouseListener(new FMouseAdapter() {
            @Override public void onLeftDoubleClick(final MouseEvent e) {
                if (e.isConsumed()) { return; } //don't activate if inline button double clicked

                final int clickedIndex = table.rowAtPoint(e.getPoint());

                itemManager.activateSelectedItems();

                if (clickedIndex >= table.getRowCount()) {
                    FMouseAdapter.forceMouseUp(); //prevent mouse getting stuck if final row removed from double click handling
                }
            }

            @Override public void onRightClick(final MouseEvent e) {
                itemManager.showContextMenu(e);
            }
        });

        // prevent tables from intercepting tab focus traversals
        this.table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        this.table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
    }

    @Override
    public void setup(final ItemManagerConfig config, final Map<ColumnDef, ItemTableColumn> colOverrides) {
        final Iterable<T> selectedItemsBefore = getSelectedItems();
        final DefaultTableColumnModel colmodel = new DefaultTableColumnModel();

        //ensure columns ordered properly
        final List<ItemTableColumn> columns = new LinkedList<ItemTableColumn>();
        for (final ItemColumnConfig colConfig : config.getCols().values()) {
            if (colOverrides == null || !colOverrides.containsKey(colConfig.getDef())) {
                columns.add(new ItemTableColumn(new ItemColumn(colConfig)));
            }
            else {
                columns.add(colOverrides.get(colConfig.getDef()));
            }
        }
        Collections.sort(columns, new Comparator<ItemTableColumn>() {
            @Override public int compare(final ItemTableColumn arg0, final ItemTableColumn arg1) {
                return Integer.compare(arg0.getIndex(), arg1.getIndex());
            }
        });

        //hide table header if only showing single string column
        final boolean hideHeader = (config.getCols().size() == 1 && config.getCols().containsKey(ColumnDef.STRING));

        getPnlOptions().removeAll();

        if (config.getShowUniqueCardsOption()) {
            final FCheckBox chkBox = new FCheckBox("Unique Cards Only", this.itemManager.getWantUnique());
            chkBox.setFont(ROW_FONT);
            chkBox.setToolTipText("Toggle whether to show unique cards only");
            chkBox.addChangeListener(new ChangeListener() {
                @Override public void stateChanged(final ChangeEvent arg0) {
                    final boolean wantUnique = chkBox.isSelected();
                    if (itemManager.getWantUnique() == wantUnique) { return; }
                    itemManager.setWantUnique(wantUnique);
                    itemManager.refresh();

                    if (itemManager.getConfig() != null) {
                        itemManager.getConfig().setUniqueCardsOnly(wantUnique);
                    }
                }
            });
            getPnlOptions().add(chkBox);
        }

        int modelIndex = 0;
        for (final ItemTableColumn col : columns) {
            col.setModelIndex(modelIndex++);
            if (col.isVisible()) { colmodel.addColumn(col); }

            if (!hideHeader) {
                final FCheckBox chkBox = new FCheckBox(StringUtils.isEmpty(col.getShortName()) ?
                        col.getLongName() : col.getShortName(), col.isVisible());
                chkBox.setFont(ROW_FONT);
                chkBox.setToolTipText(col.getLongName());
                chkBox.addChangeListener(new ChangeListener() {
                    @Override public void stateChanged(final ChangeEvent arg0) {
                        final boolean visible = chkBox.isSelected();
                        if (col.isVisible() == visible) { return; }
                        col.setVisible(visible);

                        if (col.isVisible()) {
                            colmodel.addColumn(col);

                            //move column into proper position
                            final int oldIndex = colmodel.getColumnCount() - 1;
                            int newIndex = col.getIndex();
                            for (int i = 0; i < col.getIndex(); i++) {
                                if (!columns.get(i).isVisible()) {
                                    newIndex--;
                                }
                            }
                            if (newIndex < oldIndex) {
                                colmodel.moveColumn(oldIndex, newIndex);
                            }
                        }
                        else {
                            colmodel.removeColumn(col);
                        }
                        ItemManagerConfig.save();
                    }
                });
                getPnlOptions().add(chkBox);
            }
        }

        if (hideHeader) {
            this.table.getTableHeader().setPreferredSize(new Dimension(0, 0));
        }
        else {
            this.table.getTableHeader().setPreferredSize(new Dimension(0, ROW_HEIGHT));
        }

        this.tableModel.addListeners();
        this.table.setModel(this.tableModel);
        this.table.setColumnModel(colmodel);

        this.tableModel.setup();
        this.refresh(selectedItemsBefore, 0, 0);
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
    protected SkinImage getIcon() {
        return FSkin.getIcon(FSkinProp.ICO_LIST);
    }

    @Override
    protected String getCaption() {
        return "List View";
    }

    @Override
    public void setAllowMultipleSelections(final boolean allowMultipleSelections) {
        this.table.setSelectionMode(allowMultipleSelections ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
    }

    @Override
    public int getSelectedIndex() {
        return this.table.getSelectedRow();
    }

    @Override
    public Iterable<Integer> getSelectedIndices() {
        final List<Integer> indices = new ArrayList<Integer>();
        final int[] selectedRows = this.table.getSelectedRows();
        for (int i = 0; i < selectedRows.length; i++) {
            indices.add(selectedRows[i]);
        }
        return indices;
    }

    @Override
    protected void onSetSelectedIndex(final int index) {
        this.table.setRowSelectionInterval(index, index);
    }

    @Override
    protected void onSetSelectedIndices(final Iterable<Integer> indices) {
        this.table.clearSelection();
        for (final Integer index : indices) {
            this.table.addRowSelectionInterval(index, index);
        }
    }

    @Override
    protected void onScrollSelectionIntoView(final JViewport viewport) {
        // compute where we're going and where we are
        final Rectangle targetRect  = this.table.getCellRect(this.getSelectedIndex(), 0, true);
        final Rectangle curViewRect = viewport.getViewRect();

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
    public int getIndexOfItem(final T item) {
        return this.tableModel.itemToRow(item);
    }

    @Override
    public T getItemAtIndex(final int index) {
        final Entry<T, Integer> itemEntry = this.tableModel.rowToItem(index);
        return itemEntry != null ? itemEntry.getKey() : null;
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
    public int getIndexAtPoint(final Point p) {
        return this.table.rowAtPoint(p);
    }

    @Override
    protected void onResize() {
    }

    @Override
    protected void onRefresh() {
        this.tableModel.fireTableDataChanged();
    }

    public final class ItemTable extends SkinnedTable {
        private ItemTable() {
            this.setBackground(BACK_COLOR);
            this.setForeground(FORE_COLOR);
            this.setSelectionForeground(FORE_COLOR);
            this.setSelectionBackground(SEL_INACTIVE_COLOR);
            this.setGridColor(GRID_COLOR);
            this.setFont(ROW_FONT);

            this.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(final FocusEvent e) {
                    setSelectionBackground(SEL_ACTIVE_COLOR);
                    // if nothing selected when we gain focus, select the first row (if exists)
                    if (getSelectedIndex() == -1 && getCount() > 0) {
                        setRowSelectionInterval(0, 0);
                    }
                }

                @Override
                public void focusLost(final FocusEvent e) {
                    if (!e.isTemporary()) {
                        setSelectionBackground(SEL_INACTIVE_COLOR);
                    }
                }
            });

            this.setBorder((Border)null);
            this.setRowHeight(ROW_HEIGHT);
            this.setRowMargin(0);
            this.setShowHorizontalLines(false);
            this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

            final ActionListener listener = new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final StringBuilder sb = new StringBuilder();
                    for (final int row : getSelectedRows()) {
                        final Entry<T, Integer> item = tableModel.rowToItem(row);
                        sb.append(item.getValue().toString());
                        sb.append(' ');
                        sb.append(item.getKey().toString());
                        sb.append('\n');
                    }
                    final StringSelection selection = new StringSelection(sb.toString());
                    final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(selection, selection);
                }
            };

            final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false);
            this.registerKeyboardAction(listener, "Copy", stroke, JComponent.WHEN_FOCUSED);
        }
        @Override
        protected JTableHeader createDefaultTableHeader() {
            final SkinnedTableHeader header = new SkinnedTableHeader(columnModel) {
                @Override
                public String getToolTipText(final MouseEvent e) {
                    final int col = columnModel.getColumnIndexAtX(e.getPoint().x);
                    if (col < 0) { return null; }
                    final ItemTableColumn tableColumn = (ItemTableColumn) columnModel.getColumn(col);
                    if (tableColumn.getLongName().isEmpty()) {
                        return null;
                    }
                    return tableColumn.getLongName();
                }
            };
            header.setBorder((Border)null);
            header.setBackground(HEADER_BACK_COLOR);
            header.setForeground(FORE_COLOR);

            final DefaultTableCellRenderer renderer = ((DefaultTableCellRenderer)header.getDefaultRenderer());
            header.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(final JTable table,
                        final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                    final JLabel lbl = (JLabel) renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    FSkin.setTempBorder(lbl, HEADER_BORDER);
                    return lbl;
                }
            });
            return header;
        }

        @Override
        public void processMouseEvent(final MouseEvent e) {
            final Point p = e.getPoint();
            final int row = rowAtPoint(p);
            final int col = columnAtPoint(p);

            if (col < 0 || col >= getColumnCount() || row < 0 || row >= getRowCount()) {
                return;
            }

            final Object val = getValueAt(row, col);
            if (val == null) {
                return;
            }

            final ItemCellRenderer renderer = (ItemCellRenderer)getCellRenderer(row, col);
            if (renderer != null) {
                renderer.processMouseEvent(e, ItemListView.this, val, row, col); //give renderer a chance to process the mouse event
            }
            try {
                super.processMouseEvent(e);
            }
            catch (final Exception ex) { //trap error thrown by weird tooltip issue
                ex.printStackTrace();
            }
        }

        private String getCellTooltip(final TableCellRenderer renderer, final int row, final int col, final Object val) {
            final Component cell = renderer.getTableCellRendererComponent(this, val, false, false, row, col);

            // use a pre-set tooltip if it exists
            if (cell instanceof JComponent) {
                final JComponent jcell = (JComponent)cell;
                final String tip = jcell.getToolTipText();
                if (tip != null && !tip.isEmpty()) {
                    return tip;
                }
            }

            // if we're conditionally showing the tooltip, check to see
            // if we shouldn't show it
            if (val == null) { return ""; }
            final String text = val.toString();
            if (text.isEmpty()) { return ""; }

            if (!(renderer instanceof ItemCellRenderer) || !((ItemCellRenderer)renderer).alwaysShowTooltip()) {
                // if there's enough room (or there's no value), no tooltip
                // we use '>' here instead of '>=' since that seems to be the
                // threshold for where the ellipses appear for the default
                // JTable renderer
                final int requiredWidth = cell.getPreferredSize().width;
                final TableColumn tableColumn = this.getColumnModel().getColumn(col);
                if (tableColumn.getWidth() > requiredWidth) {
                    return "";
                }
            }

            // if above checks passed, show the full text in the tooltip
            return text;
        }

        @Override
        public String getToolTipText(final MouseEvent e) {
            final Point p = e.getPoint();
            final int row = rowAtPoint(p);
            final int col = columnAtPoint(p);

            if (col >= getColumnCount() || row >= getRowCount()) {
                return "";
            }

            final Object val = getValueAt(row, col);
            if (val == null) {
                return "";
            }

            return getCellTooltip(getCellRenderer(row, col), row, col, val);
        }

        private int   lastTooltipRow = -1;
        private int   lastTooltipCol = -1;
        private Point lastTooltipPt;

        @Override
        public Point getToolTipLocation(final MouseEvent e) {
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

        /**
         * Instantiates a new table model.
         *
         * @param table0 &emsp; {@link forge.gui.ItemManager.ItemTable<T>}
         * @param model0 &emsp; {@link forge.gui.ItemManager.ItemManagerModel<T>}
         */
        public ItemTableModel(final ItemManagerModel<T> model0) {
            this.model = model0;
        }

        public void setup() {
            final Enumeration<TableColumn> e = table.getColumnModel().getColumns();
            final ItemTableColumn[] sortcols = new ItemTableColumn[table.getColumnCount()];

            // Assemble priority sort.
            while (e.hasMoreElements()) {
                final ItemTableColumn col = (ItemTableColumn) e.nextElement();
                if (col.getSortPriority() > 0 && col.getSortPriority() <= sortcols.length) {
                    sortcols[col.getSortPriority() - 1] = col;
                }
            }

            model.getCascadeManager().reset();

            for (int i = sortcols.length - 1; i >= 0; i--) {
                final ItemTableColumn col = sortcols[i];
                if (col != null) {
                    model.getCascadeManager().add(col.getItemColumn(), true);
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

        private final ListSelectionListener listSelectionListener = new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent arg0) {
                ItemListView.this.onSelectionChange();
            }
        };

        private final FocusAdapter focusAdapter = new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                ItemListView.this.onSelectionChange();
            }
        };

        private final FMouseAdapter headerMouseAdapter = new FMouseAdapter(true) {
            private ItemTableColumn resizeColumn;

            @Override
            public void onLeftMouseDown(final MouseEvent e) {
                focus();
                if (Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR) == table.getTableHeader().getCursor()) {
                    final TableColumnModel colModel = table.getColumnModel();
                    final int index = colModel.getColumnIndexAtX(e.getX() - 3); //-3 to ensure we get column left of resizer
                    if (index >= 0) {
                        resizeColumn = (ItemTableColumn) colModel.getColumn(index);
                        resizeColumn.startResize();
                        MouseUtil.lockCursor(); //lock resize cursor during resize
                    }
                }
            }

            @Override
            public void onLeftMouseUp(final MouseEvent e) {
                if (resizeColumn != null) {
                    MouseUtil.unlockCursor();
                    resizeColumn.endResize();
                    resizeColumn = null;
                }
            }

            @Override
            public void onLeftClick(final MouseEvent e) {
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
                model.getCascadeManager().add(((ItemTableColumn) table.getColumnModel().getColumn(columnModelIndex)).getItemColumn(), false);
                model.refreshSort();
                table.tableChanged(new TableModelEvent(ItemTableModel.this));
                table.repaint();
                ItemListView.this.setSelectedIndex(0);
                ItemManagerConfig.save();
            }

            @Override
            public void onLeftMouseDragDrop(final MouseEvent e) { //save preferences after column moved/resized
                for (int i = 0; i < table.getColumnCount(); i++) {
                    final ItemTableColumn column = (ItemTableColumn) table.getColumnModel().getColumn(i);
                    column.updatePreferredWidth();
                    column.setIndex(i);
                }
                ItemManagerConfig.save();
            }

            @Override
            public void onMouseExit(final MouseEvent e) {
                MouseUtil.resetCursor();
            }
        };

        private final MouseMotionAdapter headerMouseMotionAdapter = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent arg0) {
                MouseUtil.setCursor(table.getTableHeader().getCursor());
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

            table.getTableHeader().removeMouseMotionListener(headerMouseMotionAdapter); //ensure listener not added multiple times
            table.getTableHeader().addMouseMotionListener(headerMouseMotionAdapter);
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
        public Object getValueAt(final int iRow, final int iCol) {
            final Entry<T, Integer> card = this.rowToItem(iRow);
            if (null == card) {
                return null;
            }
            return ((ItemTableColumn) table.getColumnModel().getColumn(table.convertColumnIndexToView(iCol))).getFnDisplay().apply(card);
        }

        //========= Custom class handling

    }
}
