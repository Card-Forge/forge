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
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;

import com.google.common.base.Predicate;
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
public final class TableView<T extends InventoryItem> {
    private ItemPool<T> pool;
    private TableModel<T> model;
    private final JTable table = new JTable();
    private Predicate<T> filter = null;
    private boolean wantUnique = false;

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
    public TableView(final Class<T> cls) {
        this(false, cls);
    }

    /**
     * TableWithCards Constructor.
     * 
     * @param forceUnique
     *            a boolean
     * @param type0 the cls
     */
    public TableView(final boolean forceUnique, final Class<T> type0) {
        this.genericType = type0;
        this.wantUnique = forceUnique;

        table.setFont(FSkin.getFont(12));
        table.setBorder(null);
        table.getTableHeader().setBorder(null);
        table.setRowHeight(18);
    }

    /**
     * Applies a TableModel and a model listener to this instance's JTable.
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

        this.model = new TableModel<T>(this.table, this.genericType);
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
                final List<T> deck = TableView.this.model.getCards().toFlatList();
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
            Predicate<Entry<T, Integer>> filterForPool = forge.util.closures.Predicate.brigde(this.filter, this.pool.getFnToPrinted());
            Iterable<Entry<T, Integer>> cards = Aggregates.uniqueByLast(Iterables.filter(this.pool, filterForPool), this.pool.getFnToCardName());
            this.model.addCards(cards);
        } else if (useFilter) {
            Predicate<Entry<T, Integer>> pred = forge.util.closures.Predicate.brigde(this.filter, this.pool.getFnToPrinted());
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

}
