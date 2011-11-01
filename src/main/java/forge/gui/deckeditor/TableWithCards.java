package forge.gui.deckeditor;

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
import forge.Singletons;
import forge.card.CardRules;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;

/**
 * TableWithCards.
 * 
 */
public final class TableWithCards {

    /** The pool. */
    private ItemPool<InventoryItem> pool;

    /** The model. */
    private TableModel<InventoryItem> model;

    /** The table. */
    private JTable table = new JTable();

    /** The j scroll pane. */
    private JScrollPane jScrollPane = new JScrollPane();

    /** The stats label. */
    private JLabel statsLabel = new JLabel();

    /** The filter. */
    private Predicate<InventoryItem> filter = null;

    /** The is tracking stats. */
    private boolean isTrackingStats = false;

    /** The want unique. */
    private boolean wantUnique = false;

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
     * 
     * TableWithCards.
     * 
     * @param title
     *            a String
     * @param showStats
     *            a boolean
     */
    public TableWithCards(final String title, final boolean showStats) {
        this(title, showStats, false);
    }

    /**
     * 
     * TableWithCards Constructor.
     * 
     * @param title
     *            a String
     * @param showStats
     *            a boolean
     * @param forceUnique
     *            a boolean
     */
    public TableWithCards(final String title, final boolean showStats, final boolean forceUnique) {
        // components
        final Color gray = new Color(148, 145, 140);
        final TitledBorder titledBorder = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, gray), title);

        final String tableToolTip = "Click on the column name (like name or color) to sort the cards";
        this.jScrollPane.setBorder(titledBorder);
        this.jScrollPane.setToolTipText(tableToolTip);
        this.jScrollPane.getViewport().add(this.table, null);

        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            this.statsLabel.setFont(new java.awt.Font("Dialog", 0, 13));
        }
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
        this.model = new TableModel<InventoryItem>(cardView, columns, InventoryItem.class);
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
                    final ItemPoolView<InventoryItem> deck = TableWithCards.this.model.getCards();
                    TableWithCards.this.statsLabel.setText(TableWithCards.getStats(deck));
                }
            });
        }
    }

    // This should not be here, but still found no better place
    /**
     * 
     * getStats.
     * 
     * @param deck
     *            an ItemPoolView<InventoryITem>
     * @return String
     */
    public static String getStats(final ItemPoolView<InventoryItem> deck) {
        final int total = deck.countAll();
        final int creature = CardRules.Predicates.Presets.IS_CREATURE.aggregate(deck, deck.getFnToCard(), deck.getFnToCount());
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
    public TableWithCards sort(final int iCol) {
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
    public TableWithCards sort(final int iCol, final boolean isAsc) {
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
        this.setDeckImpl(ItemPool.createFrom(cards, InventoryItem.class));
    }

    /**
     * setDeck.
     * 
     * @param <T>
     *            an Object
     * @param poolView
     *            an ItemPoolView
     */
    public <T extends InventoryItem> void setDeck(final ItemPoolView<T> poolView) {
        this.setDeckImpl(ItemPool.createFrom(poolView, InventoryItem.class));
    }

    /**
     * 
     * setDeckImpl.
     * 
     * @param thePool
     *            an ItemPool
     */
    protected void setDeckImpl(final ItemPool<InventoryItem> thePool) {
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
    public void setFilter(final Predicate<InventoryItem> filterToSet) {
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
    public void addCard(final InventoryItem card) {
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
    public void removeCard(final InventoryItem card) {
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
            this.model.addCards(this.filter.uniqueByLast(this.pool, this.pool.getFnToCardName(), this.pool.getFnToPrinted()));
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
    public ItemPoolView<InventoryItem> getCards() {
        return this.pool;
    }

}
