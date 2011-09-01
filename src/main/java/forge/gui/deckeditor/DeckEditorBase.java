package forge.gui.deckeditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.MouseInputListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;

import arcane.ui.CardPanel;

import net.slightlymagic.maxmtg.Predicate;

import forge.Card;
import forge.Constant;
import forge.GUI_DeckAnalysis;
import forge.GuiDisplayUtil;
import forge.ImageCache;
import forge.ImagePreviewPanel;
import forge.card.CardPool;
import forge.card.CardPrinted;
import forge.card.CardRules;
import forge.card.CardPoolView;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPicturePanel;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

public abstract class DeckEditorBase extends JFrame implements DeckDisplay  {
    private static final long serialVersionUID = -401223933343539977L;

    //public JCheckBox whiteCheckBox = new GuiFilterCheckBox("white", "White");

    public final FilterCheckBoxes filterBoxes;
    // set this to false when resetting filter from code (like "clearFiltersPressed"), reset when done.
    protected boolean isFiltersChangeFiringUpdate = true;
    public final CardViewPanel cardView = new CardViewPanel(); 

    // CardPools and Table data for top and bottom lists
    protected CardPool top;
    protected TableModel topModel;
    protected JTable topTable = new JTable();
    protected JScrollPane jScrollPane1 = new JScrollPane();
    protected JLabel statsLabel = new JLabel();


    protected CardPoolView bottom;
    protected TableModel bottomModel;
    protected JTable bottomTable = new JTable();
    protected JScrollPane jScrollPane2 = new JScrollPane();
    protected JLabel statsLabel2 = new JLabel();

    // top shows available card pool
    // if constructed, top shows all cards
    // if sealed, top shows N booster packs
    // if draft, top shows cards that were chosen
    public final TableModel getTopTableModel() { return topModel; }
    public final CardPool getTop() { return top; }

    // bottom shows cards that the user has chosen for his library
    public final CardPoolView getBottom() { return bottomModel.getCards(); }

    protected DeckEditorBase(final boolean useFilters, final boolean areGraphicalFilters) {
        filterBoxes = useFilters ? new FilterCheckBoxes(areGraphicalFilters) : null;
    }
    
    protected final void setupTables(List<TableColumnInfo<CardPrinted>> columns, boolean trackStats ) {
        // construct topTable, get all cards

        topModel = new TableModel(cardView, columns);
        topModel.addListeners(topTable);
        topTable.setModel(topModel);
        topModel.resizeCols(topTable);

        // construct bottomModel
        bottomModel = new TableModel(cardView, columns);
        bottomModel.addListeners(bottomTable);
        bottomTable.setModel(bottomModel);
        bottomModel.resizeCols(bottomTable);

        if (trackStats)
        {
            // get stats from deck
            bottomModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(final TableModelEvent ev) {
                    CardPoolView deck = bottomModel.getCards();
                    statsLabel.setText(getStats(deck));
                }
            });
    
            // get stats from all cards
            topModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(final TableModelEvent ev) {
                    CardPoolView deck = topModel.getCards();
                    statsLabel2.setText(getStats(deck));
                }
            });
        }
    }
    
    protected final void jbInitTables(String topTitle, String bottomTitle)
    {
        Color gray = new Color(148, 145, 140);
        TitledBorder titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, gray), topTitle);
        TitledBorder titledBorder2 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, gray), bottomTitle);
        this.getContentPane().setLayout(null);
        String tableToolTip = "Click on the column name (like name or color) to sort the cards";
        jScrollPane1.setBorder(titledBorder1);
        jScrollPane1.setToolTipText(tableToolTip);
        jScrollPane2.setBorder(titledBorder2);
        jScrollPane2.setToolTipText(tableToolTip);
        
        jScrollPane2.getViewport().add(bottomTable, null);
        jScrollPane1.getViewport().add(topTable, null);
    }
    
    
    // This should not be here, but still found no better place
    public static String getStats(CardPoolView deck) {
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
    }// getStats()

    // THIS IS HERE FOR OVERLOADING!!!1
    // or may be return abstract getFilter from derived class + this filter ... virtual protected member, but later
    protected Predicate<CardRules> buildFilter() {
        if (null == filterBoxes) {
            return Predicate.getTrue(CardRules.class);
        }
        return filterBoxes.buildFilter();
    }

    
    void analysisButton_actionPerformed(ActionEvent e) {
        if (bottomModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "Cards in deck not found.", "Analysis Deck",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            DeckEditorBase g = DeckEditorBase.this;
            GUI_DeckAnalysis dAnalysis = new GUI_DeckAnalysis(g, bottomModel);
            dAnalysis.setVisible(true);
            g.setEnabled(false);
        }
    }

    protected ItemListener itemListenerUpdatesDisplay = new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
            if (isFiltersChangeFiringUpdate) { updateDisplay(); }
        }
    };

    public void updateDisplay() {
        topModel.clear();
        Predicate<CardRules> currentFilter = buildFilter();
        topModel.addCards(currentFilter.select(top, CardPoolView.fnToCard));
        topModel.resort();
    }


    
    

    // Call this after deleting an item from table
    protected void fixSelection(TableModel model, JTable table, int rowLastSelected) {
        // 3 cases: 0 cards left, select the same row, select prev row
        int cntRowsAbove = model.getRowCount();
        if (cntRowsAbove != 0) {
            if (cntRowsAbove == rowLastSelected) { rowLastSelected--; } // move selection away from the last, already missing, option
            table.setRowSelectionInterval(rowLastSelected, rowLastSelected);
        }
    }

}
