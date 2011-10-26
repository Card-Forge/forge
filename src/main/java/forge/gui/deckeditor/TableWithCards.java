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
    protected ItemPool<InventoryItem> pool;

    /** The model. */
    protected TableModel<InventoryItem> model;

    /** The table. */
    protected JTable table = new JTable();

    /** The j scroll pane. */
    protected JScrollPane jScrollPane = new JScrollPane();

    /** The stats label. */
    protected JLabel statsLabel = new JLabel();

    /** The filter. */
    protected Predicate<InventoryItem> filter = null;

    /** The is tracking stats. */
    protected boolean isTrackingStats = false;

    /** The want unique. */
    protected boolean wantUnique = false;

    // need this to allow users place its contents
    /**
     * 
     * getTableDecorated.
     * 
     * @return JComponent
     */
    public JComponent getTableDecorated() {
        return jScrollPane;
    }

    /**
     * 
     * getTable.
     * 
     * @return JTable
     */
    public JTable getTable() {
        return table;
    }

    /**
     * 
     * getLabel.
     * 
     * @return JComponent
     */
    public JComponent getLabel() {
        return statsLabel;
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
        Color gray = new Color(148, 145, 140);
        TitledBorder titledBorder = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, gray), title);

        String tableToolTip = "Click on the column name (like name or color) to sort the cards";
        jScrollPane.setBorder(titledBorder);
        jScrollPane.setToolTipText(tableToolTip);
        jScrollPane.getViewport().add(table, null);

        if (!Singletons.getModel().getPreferences().lafFonts) {
            statsLabel.setFont(new java.awt.Font("Dialog", 0, 13));
        }
        statsLabel.setText("Total: 0, Creatures: 0, Land: 0");

        // class data
        isTrackingStats = showStats;
        wantUnique = forceUnique;
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
        model = new TableModel<InventoryItem>(cardView, columns, InventoryItem.class);
        model.addListeners(table);
        table.setModel(model);
        model.resizeCols(table);

        for (int idx = columns.size() - 1; idx >= 0; idx--) {
            TableCellRenderer renderer = columns.get(idx).getCellRenderer();
            if (null != renderer) {
                table.getColumnModel().getColumn(idx).setCellRenderer(renderer);
            }
        }

        if (isTrackingStats) {
            // get stats from deck
            model.addTableModelListener(new TableModelListener() {
                public void tableChanged(final TableModelEvent ev) {
                    ItemPoolView<InventoryItem> deck = model.getCards();
                    statsLabel.setText(getStats(deck));
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
        int total = deck.countAll();
        int creature = CardRules.Predicates.Presets.isCreature.aggregate(deck, deck.fnToCard, deck.fnToCount);
        int land = CardRules.Predicates.Presets.isLand.aggregate(deck, deck.fnToCard, deck.fnToCount);

        StringBuffer show = new StringBuffer();
        show.append("Total - ").append(total).append(", Creatures - ").append(creature).append(", Land - ")
                .append(land);
        String[] color = Constant.Color.onlyColors;
        List<Predicate<CardRules>> predicates = CardRules.Predicates.Presets.colors;
        for (int i = 0; i < color.length; ++i) {
            show.append(String.format(", %s - %d", color[i], predicates.get(i).count(deck, deck.fnToCard)));
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
        return sort(iCol, true);
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
        model.sort(iCol, isAsc);
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
        int cntRowsAbove = model.getRowCount();
        if (cntRowsAbove != 0) {
            if (cntRowsAbove == newRow) {
                newRow--;
            } // move selection away from the last, already missing, option
            table.setRowSelectionInterval(newRow, newRow);
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
        setDeckImpl(ItemPool.createFrom(cards, InventoryItem.class));
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
        setDeckImpl(ItemPool.createFrom(poolView, InventoryItem.class));
    }

    /**
     * 
     * setDeckImpl.
     * 
     * @param thePool
     *            an ItemPool
     */
    protected void setDeckImpl(final ItemPool<InventoryItem> thePool) {
        model.clear();
        pool = thePool;
        model.addCards(pool);
        updateView(true);
    }

    /**
     * 
     * getSelectedCard.
     * 
     * @return InventoryItem
     */
    public InventoryItem getSelectedCard() {
        int iRow = table.getSelectedRow();
        return iRow >= 0 ? model.rowToCard(iRow).getKey() : null;
    }

    private boolean isUnfiltered() {
        return filter == null || filter.is1();
    }

    /**
     * 
     * setFilter.
     * 
     * @param filterToSet
     *            a Predicate
     */
    public void setFilter(final Predicate<InventoryItem> filterToSet) {
        filter = filterToSet;
        updateView(true);
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
        pool.add(card);
        if (isUnfiltered()) {
            model.addCard(card);
        }
        updateView(false);
    }

    /**
     * 
     * removeCard.
     * 
     * @param card
     *            an InventoryItem
     */
    public void removeCard(final InventoryItem card) {
        int n = table.getSelectedRow();
        pool.remove(card);
        if (isUnfiltered()) {
            model.removeCard(card);
        }
        updateView(false);
        fixSelection(n);
    }

    /**
     * 
     * updateView.
     * 
     * @param bForceFilter
     *            a boolean
     */
    public void updateView(final boolean bForceFilter) {
        boolean useFilter = (bForceFilter && filter != null) || !isUnfiltered();

        if (useFilter || wantUnique) {
            model.clear();
        }

        if (useFilter && wantUnique) {
            model.addCards(filter.uniqueByLast(pool, pool.fnToCardName, pool.fnToPrinted));
        } else if (useFilter) {
            model.addCards(filter.select(pool, pool.fnToPrinted));
        } else if (wantUnique) {
            model.addCards(CardRules.Predicates.Presets.constantTrue.uniqueByLast(pool, pool.fnToCardName,
                    pool.fnToCard));
        }

        model.resort();
    }

    /**
     * 
     * getCards.
     * 
     * @return ItemPoolView
     */
    public ItemPoolView<InventoryItem> getCards() {
        return pool;
    }

}
