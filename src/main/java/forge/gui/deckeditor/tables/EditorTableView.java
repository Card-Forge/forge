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
package forge.gui.deckeditor.tables;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.gui.deckeditor.SEditorUtil;
import forge.gui.deckeditor.views.ITableContainer;
import forge.gui.toolbox.FSkin;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;
import forge.util.Aggregates;


/**
 * TableWithCards.
 * 
 * @param <T>
 *            the generic type
 */
public final class EditorTableView<T extends InventoryItem> {
    private ItemPool<T> pool;
    private EditorTableModel<T> model;
    private final JTable table;
    private Predicate<T> filter = null;
    private boolean wantUnique = false;
    private boolean alwaysNonUnique = false;

    private final Class<T> genericType;

    /**
     * 
     * getTable.
     * 
     * @return JTable
     */
    public JTable getTable() {
        return this.table;
    }

    /**
     * TableWithCards.
     * 
     * @param cls
     *            the cls
     */
    public EditorTableView(final Class<T> cls) {
        this(false, cls);
    }

    /**
     * TableWithCards Constructor.
     * 
     * @param forceUnique
     *            a boolean
     * @param type0 the cls
     */
    @SuppressWarnings("serial")
    public EditorTableView(final boolean forceUnique, final Class<T> type0) {
        this.genericType = type0;
        this.wantUnique = forceUnique;

        // subclass JTable to show tooltips when hovering over column headers
        // and cell data that are truncated due to too-small column widths
        table = new JTable() {
            private String _getCellTooltip(
                    TableCellRenderer renderer, int row, int col, Object val) {
                Component cell = renderer.getTableCellRendererComponent(
                                        table, val, false, false, row, col);
                
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
                Object val = table.getValueAt(row, col);

                return _getCellTooltip(getCellRenderer(row, col), row, col, val);
            }
        };
        
        table.setFont(FSkin.getFont(12));
        table.setBorder(null);
        table.getTableHeader().setBorder(null);
        table.setRowHeight(18);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
    }

    /**
     * Applies a EditorTableModel and a model listener to this instance's JTable.
     * 
     * @param view0 &emsp; the {@link javax.gui.deckeditor.views.ITableCOntainer}
     * @param cols0 &emsp; List<TableColumnInfo<InventoryItem>> of additional columns for this
     */
    @SuppressWarnings("unchecked")
    public void setup(final ITableContainer view0, final List<TableColumnInfo<InventoryItem>> cols0) {
        final DefaultTableColumnModel colmodel = new DefaultTableColumnModel();

        for (TableColumnInfo<InventoryItem> item : cols0) {
            item.setModelIndex(colmodel.getColumnCount());
            if (item.isShowing()) { colmodel.addColumn(item); }
        }

        this.model = new EditorTableModel<T>(this.table, this.genericType);
        this.model.addListeners();
        this.table.setModel(this.model);
        this.table.setColumnModel(colmodel);

        this.model.setup();
        this.model.refreshSort();

        this.table.getTableHeader().setBackground(new Color(200, 200, 200));

        // Update stats each time table changes
        this.model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent ev) {
                final List<T> deck = EditorTableView.this.model.getCards().toFlatList();
                final ItemPool<T> filteredDeck = new ItemPool<T>((Class<T>) CardPrinted.class);

                // Filter out non-card items (booster packs, etc.)
                for (T item : deck) {
                    if (item instanceof CardPrinted) {
                        filteredDeck.add(item);
                    }
                }

                SEditorUtil.setStats(filteredDeck, view0);
            }
        });
    }

    public void setAvailableColumns(final List<TableColumnInfo<InventoryItem>> cols0) {
        final DefaultTableColumnModel colmodel = new DefaultTableColumnModel();

        for (TableColumnInfo<InventoryItem> item : cols0) {
            item.setModelIndex(colmodel.getColumnCount());
            if (item.isShowing()) { colmodel.addColumn(item); }
        }

        this.table.setColumnModel(colmodel);
    }

    /**
     * 
     * fixSelection. Call this after deleting an item from table.
     * 
     * @param rowLastSelected
     *            an int
     */
    public void fixSelection(final int rowLastSelected) {
        // 3 cases: 0 cards left, select the same row, select prev row
        int newRow = rowLastSelected;
        final int cntRowsAbove = this.model.getRowCount();
        if (cntRowsAbove != 0) {
            if (cntRowsAbove == newRow) {
                newRow--;
            } // move selection away from the last, already missing, option
            this.table.setRowSelectionInterval(newRow, newRow);
        }
    }

    /**
     * 
     * setDeck.
     * 
     * @param cards
     *            an Iterable<InventoryITem>
     */
    public void setDeck(final Iterable<InventoryItem> cards) {
        this.setDeckImpl(ItemPool.createFrom(cards, this.genericType));
    }

    /**
     * setDeck.
     * 
     * @param poolView
     *            an ItemPoolView
     */
    public void setDeck(final ItemPoolView<T> poolView) {
        this.setDeckImpl(ItemPool.createFrom(poolView, this.genericType));
    }

    /**
     * Sets the deck.
     * 
     * @param pool
     *            the new deck
     */
    public void setDeck(final ItemPool<T> pool) {
        this.setDeckImpl(pool);
    }

    /**
     * 
     * setDeckImpl.
     * 
     * @param thePool
     *            an ItemPool
     */
    protected void setDeckImpl(final ItemPool<T> thePool) {
        this.model.clear();
        this.pool = thePool;
        this.model.addCards(this.pool);
        this.updateView(true);
    }

    /**
     * 
     * getSelectedCard.
     * 
     * @return InventoryItem
     */
    public InventoryItem getSelectedCard() {
        final int iRow = this.table.getSelectedRow();
        return iRow >= 0 ? this.model.rowToCard(iRow).getKey() : null;
    }

    private boolean isUnfiltered() {
        return this.filter == null;
    }

    /**
     * 
     * setFilter.
     * 
     * @param filterToSet
     *            a Predicate
     */
    public void setFilter(final Predicate<T> filterToSet) {
        this.filter = filterToSet;
        this.updateView(true);
    }

    /**
     * 
     * addCard.
     * 
     * @param card
     *            an InventoryItem
     */
    public void addCard(final T card) {
        // int n = table.getSelectedRow();
        this.pool.add(card);
        if (this.isUnfiltered()) {
            this.model.addCard(card);
        }
        this.updateView(false);
    }

    /**
     * 
     * removeCard.
     * 
     * @param card
     *            an InventoryItem
     */
    public void removeCard(final T card) {
        final int n = this.table.getSelectedRow();
        this.pool.remove(card);
        if (this.isUnfiltered()) {
            this.model.removeCard(card);
        }
        this.updateView(false);
        this.fixSelection(n);
    }

    /**
     * 
     * updateView.
     * 
     * @param bForceFilter
     *            a boolean
     */
    public void updateView(final boolean bForceFilter) {
        final boolean useFilter = (bForceFilter && (this.filter != null)) || !this.isUnfiltered();

        if (useFilter || this.wantUnique || bForceFilter) {
            this.model.clear();
        }

        if (useFilter && this.wantUnique) {
            Predicate<Entry<T, Integer>> filterForPool = Predicates.compose(this.filter, this.pool.getFnToPrinted());
            Iterable<Entry<T, Integer>> cards = Aggregates.uniqueByLast(Iterables.filter(this.pool, filterForPool), this.pool.getFnToCardName());
            this.model.addCards(cards);
        } else if (useFilter) {
            Predicate<Entry<T, Integer>> pred = Predicates.compose(this.filter, this.pool.getFnToPrinted());
            this.model.addCards(Iterables.filter(this.pool, pred));
        } else if (this.wantUnique) {
            Iterable<Entry<T, Integer>> cards = Aggregates.uniqueByLast(this.pool, this.pool.getFnToCardName());
            this.model.addCards(cards);
        } else if (!useFilter && bForceFilter) {
            this.model.addCards(this.pool);
        }

        this.model.refreshSort();
    }

    /**
     * 
     * getCards.
     * 
     * @return ItemPoolView
     */
    public ItemPoolView<T> getCards() {
        return this.pool;
    }

    /**
     * 
     * getWantUnique.
     * 
     * @return true if the editor is in "unique card names only" mode.
     */
    public boolean getWantUnique() {
        return this.wantUnique;
    }

    /**
     * 
     * setWantUnique.
     * 
     * @param unique if true, the editor will be set to the "unique card names only" mode.
     */
    public void setWantUnique(boolean unique) {
        this.wantUnique = this.alwaysNonUnique ? false : unique;
    }

    /**
     * 
     * getAlwaysNonUnique.
     * 
     * @return if ture, this editor must always show non-unique cards (e.g. quest editor).
     */
    public boolean getAlwaysNonUnique() {
        return this.alwaysNonUnique;
    }

    /**
     * 
     * setAlwaysNonUnique.
     * 
     * @param nonUniqueOnly if true, this editor must always show non-unique cards (e.g. quest editor).
     */
    public void setAlwaysNonUnique(boolean nonUniqueOnly) {
        this.alwaysNonUnique = nonUniqueOnly;
    }

    public void setWantElasticColumns(boolean value) {
        table.setAutoResizeMode(value ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS : JTable.AUTO_RESIZE_NEXT_COLUMN);
    }
}
