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
package forge.gui.deckeditor.elements;

import java.awt.Color;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;

import net.slightlymagic.maxmtg.Predicate;

import forge.Constant;
import forge.card.CardRules;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;

/**
 * TableWithCards.
 * 
 * @param <T>
 *            the generic type
 */
public final class TableView<T extends InventoryItem> {

    /** The pool. */
    private ItemPool<T> pool;

    /** The model. */
    private TableModel<T> model;

    /** The table. */
    private final JTable table = new JTable();

    /** The j scroll pane. */
    private final JScrollPane jScrollPane = new JScrollPane();

    /** The stats label. */
    private final JLabel statsLabel = new JLabel();

    /** The filter. */
    private Predicate<T> filter = null;

    /** The is tracking stats. */
    private boolean isTrackingStats = false;

    /** The want unique. */
    private boolean wantUnique = false;

    private final Class<T> genericType;

    // need this to allow users place its contents
    /**
     * 
     * getTableDecorated.
     * 
     * @return JComponent
     */
    public JComponent getTableDecorated() {
        return this.jScrollPane;
    }

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
     * 
     * getLabel.
     * 
     * @return JComponent
     */
    public JComponent getLabel() {
        return this.statsLabel;
    }

    /**
     * TableWithCards.
     * 
     * @param title
     *            a String
     * @param showStats
     *            a boolean
     * @param cls
     *            the cls
     */
    public TableView(final String title, final boolean showStats, final Class<T> cls) {
        this(title, showStats, false, cls);
    }

    /**
     * TableWithCards Constructor.
     * 
     * @param title
     *            a String
     * @param showStats
     *            a boolean
     * @param forceUnique
     *            a boolean
     * @param cls
     *            the cls
     */
    public TableView(final String title, final boolean showStats, final boolean forceUnique, final Class<T> cls) {
        // components
        this.genericType = cls;

        final Color gray = new Color(148, 145, 140);
        final TitledBorder titledBorder = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, gray), title);

        final String tableToolTip = "Click on the column name (like name or color) to sort the cards";
        this.jScrollPane.setBorder(titledBorder);
        this.jScrollPane.setToolTipText(tableToolTip);
        this.jScrollPane.getViewport().add(this.table, null);

        this.statsLabel.setFont(new java.awt.Font("Dialog", 0, 13));
        this.statsLabel.setText("Total: 0, Creatures: 0, Land: 0");

        // class data
        this.isTrackingStats = showStats;
        this.wantUnique = forceUnique;
    }

    /**
     * 
     * setup.
     * 
     * @param columns
     *            a List<TableColumnInfo<InventoryItem>>
     * @param cardView
     *            a CardPanelBase
     */
    public void setup(final List<TableColumnInfo<InventoryItem>> columns, final CardPanelBase cardView) {
        this.model = new TableModel<T>(cardView, columns, this.genericType);
        this.model.addListeners(this.table);
        this.table.setModel(this.model);
        this.model.resizeCols(this.table);

        for (int idx = columns.size() - 1; idx >= 0; idx--) {
            final TableCellRenderer renderer = columns.get(idx).getCellRenderer();
            if (null != renderer) {
                this.table.getColumnModel().getColumn(idx).setCellRenderer(renderer);
            }
        }

        if (this.isTrackingStats) {
            // get stats from deck
            this.model.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(final TableModelEvent ev) {
                    final ItemPoolView<T> deck = TableView.this.model.getCards();
                    TableView.this.statsLabel.setText(TableView.getStats(deck));
                }
            });
        }
    }

    // This should not be here, but still found no better place
    /**
     * getStats.
     * 
     * @param <T>
     *            the generic type
     * @param deck
     *            an ItemPoolView<InventoryITem>
     * @return String
     */
    public static <T extends InventoryItem> String getStats(final ItemPoolView<T> deck) {
        final int total = deck.countAll();
        final int creature = CardRules.Predicates.Presets.IS_CREATURE.aggregate(deck, deck.getFnToCard(),
                deck.getFnToCount());
        final int land = CardRules.Predicates.Presets.IS_LAND.aggregate(deck, deck.getFnToCard(), deck.getFnToCount());

        final StringBuffer show = new StringBuffer();
        show.append("Total - ").append(total).append(", Creatures - ").append(creature).append(", Land - ")
                .append(land);
        final String[] color = Constant.Color.ONLY_COLORS;
        final List<Predicate<CardRules>> predicates = CardRules.Predicates.Presets.COLORS;
        for (int i = 0; i < color.length; ++i) {
            show.append(String.format(", %s - %d", color[i], predicates.get(i).count(deck, deck.getFnToCard())));
        }

        return show.toString();
    } // getStats()

    /**
     * 
     * sort.
     * 
     * @param iCol
     *            an int
     * @return TableWithCards
     */
    public TableView<T> sort(final int iCol) {
        return this.sort(iCol, true);
    }

    /**
     * 
     * sort.
     * 
     * @param iCol
     *            an int
     * @param isAsc
     *            a boolean
     * @return TableWithCards
     */
    public TableView<T> sort(final int iCol, final boolean isAsc) {
        this.model.sort(iCol, isAsc);
        return this;
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
        return (this.filter == null) || this.filter.is1();
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

        if (useFilter || this.wantUnique) {
            this.model.clear();
        }

        if (useFilter && this.wantUnique) {
            this.model.addCards(this.filter.uniqueByLast(this.pool, this.pool.getFnToCardName(),
                    this.pool.getFnToPrinted()));
        } else if (useFilter) {
            this.model.addCards(this.filter.select(this.pool, this.pool.getFnToPrinted()));
        } else if (this.wantUnique) {
            this.model.addCards(CardRules.Predicates.Presets.CONSTANT_TRUE.uniqueByLast(this.pool,
                    this.pool.getFnToCardName(), this.pool.getFnToCard()));
        }

        this.model.resort();
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
