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
import forge.card.CardPool;
import forge.card.CardPoolView;
import forge.card.CardPrinted;
import forge.card.CardRules;
//import forge.view.swing.OldGuiNewGame;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public final class TableWithCards {

    protected CardPool pool;
    protected TableModel model;
    protected JTable table = new JTable();
    protected JScrollPane jScrollPane = new JScrollPane();
    protected JLabel statsLabel = new JLabel();
    protected Predicate<CardPrinted> filter = null;
    protected boolean isTrackingStats = false;
    protected boolean wantUnique = false;

    // need this to allow users place its contents
    public JComponent getTableDecorated() { return  jScrollPane; }
    public JTable getTable() { return table; }
    public JComponent getLabel() { return statsLabel; }

    public TableWithCards(final String title, final boolean showStats) {
        this(title, showStats, false);
    }
    public TableWithCards(final String title, final boolean showStats, final boolean forceUnique) {
        // components
        Color gray = new Color(148, 145, 140);
        TitledBorder titledBorder = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, gray), title);

        String tableToolTip = "Click on the column name (like name or color) to sort the cards";
        jScrollPane.setBorder(titledBorder);
        jScrollPane.setToolTipText(tableToolTip);
        jScrollPane.getViewport().add(table, null);

        if (!Singletons.getModel().getPreferences().lafFonts) { statsLabel.setFont(new java.awt.Font("Dialog", 0, 13)); }
        statsLabel.setText("Total: 0, Creatures: 0, Land: 0");

        // class data
        isTrackingStats = showStats;
        wantUnique = forceUnique;
    }

    public void setup(final List<TableColumnInfo<CardPrinted>> columns, final CardPanelBase cardView)
    {
        model = new TableModel(cardView, columns);
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
                    CardPoolView deck = model.getCards();
                    statsLabel.setText(getStats(deck));
                }
            });
        }
    }

    // This should not be here, but still found no better place
    public static String getStats(final CardPoolView deck) {
        int total = deck.countAll();
        int creature = CardRules.Predicates.Presets.isCreature.aggregate(deck, CardPoolView.fnToCard, CardPoolView.fnToCount);
        int land = CardRules.Predicates.Presets.isLand.aggregate(deck, CardPoolView.fnToCard, CardPoolView.fnToCount);

        StringBuffer show = new StringBuffer();
        show.append("Total - ").append(total).append(", Creatures - ").append(creature).append(", Land - ").append(land);
        String[] color = Constant.Color.onlyColors;
        List<Predicate<CardRules>> predicates = CardRules.Predicates.Presets.colors;
        for (int i = 0; i < color.length; ++i) {
            show.append(String.format(", %s - %d", color[i], predicates.get(i).count(deck, CardPoolView.fnToCard)));
        }

        return show.toString();
    } // getStats()

    public TableWithCards sort(final int iCol) { return sort(iCol, true); }
    public TableWithCards sort(final int iCol, final boolean isAsc) {
        model.sort(iCol, isAsc);
        return this;
    }

    // Call this after deleting an item from table
    public void fixSelection(final int rowLastSelected) {
        // 3 cases: 0 cards left, select the same row, select prev row
        int newRow = rowLastSelected;
        int cntRowsAbove = model.getRowCount();
        if (cntRowsAbove != 0) {
            if (cntRowsAbove == newRow) { newRow--; } // move selection away from the last, already missing, option
            table.setRowSelectionInterval(newRow, newRow);
        }
    }

    public void setDeck(final Iterable<CardPrinted> cards) {
        setDeckImpl(new CardPool(cards));
    }

    public void setDeck(final CardPoolView poolView) {
        setDeckImpl(new CardPool(poolView));
    }
    
    protected void setDeckImpl(CardPool thePool)
    {
        model.clear();
        pool = thePool;
        model.addCards(pool);
        updateView(true);
    }

    public CardPrinted getSelectedCard() {
        int iRow = table.getSelectedRow();
        return iRow >= 0 ? model.rowToCard(iRow).getKey() : null;
    }

    private boolean isUnfiltered() { return filter == null || filter.is1(); }

    public void setFilter(final Predicate<CardPrinted> filterToSet) {
        filter = filterToSet;
        updateView(true);
    }

    public void addCard(final CardPrinted card) {
        //int n = table.getSelectedRow();
        pool.add(card);
        if (isUnfiltered()) { model.addCard(card); }
        updateView(false);
    }

    public void removeCard(final CardPrinted card) {
        int n = table.getSelectedRow();
        pool.remove(card);
        if (isUnfiltered()) { model.removeCard(card); }
        updateView(false);
        fixSelection(n);
    }

    public void updateView(boolean bForceFilter) {
        boolean useFilter = ( bForceFilter && filter != null ) || !isUnfiltered();
        
        if (useFilter || wantUnique) {
            model.clear();
        }

        if (useFilter && wantUnique) {
            model.addCards(filter.uniqueByLast(pool, CardPoolView.fnToCardName, CardPoolView.fnToPrinted));
        } else if (useFilter) {
            model.addCards(filter.select(pool, CardPoolView.fnToPrinted));
        } else if (wantUnique) {
            model.addCards(CardRules.Predicates.Presets.constantTrue.uniqueByLast(pool, CardPoolView.fnToCardName, CardPoolView.fnToCard));
        }

        model.resort();
    }

    public CardPoolView getCards() {
        return pool;
    }

}
