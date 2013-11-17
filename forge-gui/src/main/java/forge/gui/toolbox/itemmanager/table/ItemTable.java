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
package forge.gui.toolbox.itemmanager.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.ItemManagerModel;
import forge.gui.toolbox.itemmanager.SItemManagerUtil;
import forge.item.InventoryItem;


/**
 * ItemTable.
 * 
 * @param <T>
 *            the generic type
 */
@SuppressWarnings("serial")
public final class ItemTable<T extends InventoryItem> extends JTable {
    private final FSkin.JTableSkin<ItemTable<T>> skin;
    private final ItemManager<T> itemManager;
    private final ItemTableModel<T> tableModel;

    public ItemManager<T> getItemManager() {
    	return this.itemManager;
    }
    
    public ItemTableModel<T> getTableModel() {
        return this.tableModel;
    }

    /**
     * ItemTable Constructor.
     * 
     * @param itemManager0
     * @param model0
     */
    public ItemTable(ItemManager<T> itemManager0, ItemManagerModel<T> model0) {
        this.itemManager = itemManager0;
        this.tableModel = new ItemTableModel<T>(this, model0);

        // use different selection highlight colors for focused vs. unfocused tables
        skin = FSkin.get(this);
        skin.setSelectionBackground(FSkin.getColor(FSkin.Colors.CLR_INACTIVE));
        skin.setSelectionForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                if (!e.isTemporary()) {
                    skin.setSelectionBackground(FSkin.getColor(FSkin.Colors.CLR_INACTIVE));
                }
            }
            
            @Override
            public void focusGained(FocusEvent e) {
                skin.setSelectionBackground(FSkin.getColor(FSkin.Colors.CLR_ACTIVE));
                // if nothing selected when we gain focus, select the first row (if exists)
                if (-1 == getSelectedRow() && 0 < getRowCount()) {
                    setRowSelectionInterval(0, 0);
                }
            }
        });
        
        skin.setFont(FSkin.getFont(12));
        setBorder(null);
        getTableHeader().setBorder(null);
        setRowHeight(18);
        setWantElasticColumns(false);
        
        // prevent tables from intercepting tab focus traversals
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
    }
    
    private final TableModelListener tableModelListener = new TableModelListener() {
        @Override
        public void tableChanged(final TableModelEvent ev) {
            SItemManagerUtil.setStats(ItemTable.this.itemManager);
        }
    };

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
        setModel(this.tableModel);
        setColumnModel(colmodel);

        this.tableModel.setup();
        this.tableModel.refreshSort();

        getTableHeader().setBackground(new Color(200, 200, 200));

        // Update stats each time table changes
        this.tableModel.removeTableModelListener(tableModelListener); //ensure listener not added multiple times
        this.tableModel.addTableModelListener(tableModelListener);
    }
    
    private String _getCellTooltip(TableCellRenderer renderer, int row, int col, Object val) {
        Component cell = renderer.getTableCellRendererComponent(
                                this, val, false, false, row, col);
        
        // if we're conditionally showing the tooltip, check to see
        // if we shouldn't show it
        if (!(cell instanceof AlwaysShowToolTip))
        {
            // if there's enough room (or there's no value), no tooltip
            // we use '>' here instead of '>=' since that seems to be the
            // threshold for where the ellipses appear for the default
            // JTable renderer
            int requiredWidth = cell.getPreferredSize().width;
            TableColumn tableColumn = columnModel.getColumn(col);
            if (null == val || tableColumn.getWidth() > requiredWidth) {
                return null;
            }
        }

        // use a pre-set tooltip if it exists
        if (cell instanceof JComponent)
        {
            JComponent jcell = (JComponent)cell;
            String tip = jcell.getToolTipText();
            if (null != tip)
            {
                return tip;
            }
        }

        // otherwise, show the full text in the tooltip
        return String.valueOf(val);
    }
    
    // column headers
    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
                int col = columnModel.getColumnIndexAtX(e.getPoint().x);
                if (col < 0) { return null; }
                TableColumn tableColumn = columnModel.getColumn(col);
                TableCellRenderer headerRenderer = tableColumn.getHeaderRenderer();
                if (null == headerRenderer) {
                    headerRenderer = getDefaultRenderer();
                }
                
                return _getCellTooltip(
                        headerRenderer, -1, col, tableColumn.getHeaderValue());
            }
        };
    }
    
    // cell data
    @Override
    public String getToolTipText(MouseEvent e) {
        Point p = e.getPoint();
        int row = rowAtPoint(p);
        int col = columnAtPoint(p);
        
        if (col >= getColumnCount() || row >= getRowCount()) {
            return null;
        }
        
        Object val = getValueAt(row, col);
        if (null == val) {
            return null;
        }
        
        return _getCellTooltip(getCellRenderer(row, col), row, col, val);
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
        } else {
            lastTooltipRow = row;
            lastTooltipCol = col;
            lastTooltipPt  = p;
        }
        return new Point(p.x + 10, p.y + 20);
    }

    public void setAvailableColumns(final List<TableColumnInfo<InventoryItem>> cols) {
        final DefaultTableColumnModel colModel = new DefaultTableColumnModel();

        for (TableColumnInfo<InventoryItem> item : cols) {
            item.setModelIndex(colModel.getColumnCount());
            if (item.isShowing()) { colModel.addColumn(item); }
        }

        setColumnModel(colModel);
    }

    /**
     * 
     * fixSelection. Call this after deleting an item from table.
     * 
     * @param rowLastSelected
     *            an int
     */
    public void fixSelection(final int rowLastSelected) {
        if (0 > rowLastSelected) {
            return;
        }
        
        // 3 cases: 0 items left, select the same row, select prev row
        int numRows = getRowCount();
        if (numRows == 0) {
            return;
        }
        
        int newRow = rowLastSelected;
        if (numRows <= newRow) {
            // move selection away from the last, already missing, option
            newRow = numRows - 1;
        }
        
        selectAndScrollTo(newRow);
    }

    /**
     * 
     * getSelectedItem.
     * 
     * @return InventoryItem
     */
    public T getSelectedItem() {
        final int iRow = getSelectedRow();
        return iRow >= 0 ? this.tableModel.rowToItem(iRow).getKey() : null;
    }
    
    /**
     * 
     * getSelectedItems.
     * 
     * @return List<InventoryItem>
     */
    public List<T> getSelectedItems() {
        List<T> items = new ArrayList<T>();
        for (int row : getSelectedRows()) {
            items.add(tableModel.rowToItem(row).getKey());
        }
        return items;
    }

    /**
     * 
     * setSelectedItem.
     * 
     * @param item - Item to select
     */
    public void setSelectedItem(T item) {
    	int row = this.tableModel.itemToRow(item);
    	if (row != -1) {
    		selectAndScrollTo(row);
    	}
    }

    public void setWantElasticColumns(boolean value) {
        setAutoResizeMode(value ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS : JTable.AUTO_RESIZE_OFF);
    }
    
    public void selectAndScrollTo(int rowIdx) {
        if (!(getParent() instanceof JViewport)) {
            return;
        }
        JViewport viewport = (JViewport)getParent();

        // compute where we're going and where we are
        Rectangle targetRect  = getCellRect(rowIdx, 0, true);
        Rectangle curViewRect = viewport.getViewRect();

        // if the target cell is not visible, attempt to jump to a location where it is
        // visible but not on the edge of the viewport
        if (targetRect.y + targetRect.height > curViewRect.y + curViewRect.height) {
            // target is below us, move to position 3 rows below target
            targetRect.setLocation(targetRect.x, targetRect.y + (targetRect.height * 3));
        } else if  (targetRect.y < curViewRect.y) {
            // target is above is, move to position 3 rows above target
            targetRect.setLocation(targetRect.x, targetRect.y - (targetRect.height * 3));
        }
        
        scrollRectToVisible(targetRect);
        setRowSelectionInterval(rowIdx, rowIdx);
    }
}
