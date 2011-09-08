package forge.gui.deckeditor;

import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import net.miginfocom.swing.MigLayout;
import net.slightlymagic.maxmtg.Predicate;
import forge.Command;
import forge.Constant;
import forge.card.CardDb;
import forge.card.CardPrinted;
import forge.deck.Deck;
import forge.error.ErrorViewer;
import forge.properties.NewConstants;
import forge.view.swing.OldGuiNewGame;

/**
 * <p>
 * Gui_DeckEditor class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class DeckEditor extends DeckEditorBase implements NewConstants {
    /** Constant <code>serialVersionUID=130339644136746796L</code> */
    private static final long serialVersionUID = 130339644136746796L;

    public DeckEditorMenu customMenu;

    private JButton removeButton = new JButton();
    private JButton addButton = new JButton();
    private JButton analysisButton = new JButton();
    private JButton clearFilterButton = new JButton();
    
    private JLabel jLabelAnalysisGap = new JLabel();  
    private FilterNameTypeSetPanel filterNameTypeSet;

    private boolean isConstructed = false;

    /** {@inheritDoc} */
    @Override
    public void setTitle(final String message) {
        super.setTitle(message);
    }

    public void show(final Command exitCommand) {
        final Command exit = new Command() {
            private static final long serialVersionUID = 5210924838133689758L;

            public void execute() {
                DeckEditor.this.dispose();
                exitCommand.execute();
            }
        };

        customMenu = new DeckEditorMenu(this, exit);
        this.setJMenuBar(customMenu);

        // do not change this!!!!
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent ev) {
                customMenu.close();
            }
        });

        setup();

        isConstructed = Constant.Runtime.GameType[0].equals(Constant.GameType.Constructed);

        // show cards, makes this user friendly
        if (isConstructed) {
            customMenu.newConstructed();
        }

        top.sort(1, true);
        bottom.sort(1, true);

    } // show(Command)


    private void setup() {
        List<TableColumnInfo<CardPrinted>> columns = new ArrayList<TableColumnInfo<CardPrinted>>();
        columns.add(new TableColumnInfo<CardPrinted>("Qty", 30, PresetColumns.fnQtyCompare, PresetColumns.fnQtyGet));
        columns.add(new TableColumnInfo<CardPrinted>("Name", 175, PresetColumns.fnNameCompare, PresetColumns.fnNameGet));
        columns.add(new TableColumnInfo<CardPrinted>("Cost", 75, PresetColumns.fnCostCompare, PresetColumns.fnCostGet));
        columns.add(new TableColumnInfo<CardPrinted>("Color", 50, PresetColumns.fnColorCompare, PresetColumns.fnColorGet));
        columns.add(new TableColumnInfo<CardPrinted>("Type", 100, PresetColumns.fnTypeCompare, PresetColumns.fnTypeGet));
        columns.add(new TableColumnInfo<CardPrinted>("Stats", 40, PresetColumns.fnStatsCompare, PresetColumns.fnStatsGet));
        columns.add(new TableColumnInfo<CardPrinted>("R", 35, PresetColumns.fnRarityCompare, PresetColumns.fnRarityGet));
        columns.add(new TableColumnInfo<CardPrinted>("Set", 40, PresetColumns.fnSetCompare, PresetColumns.fnSetGet));
        columns.add(new TableColumnInfo<CardPrinted>("AI", 30, PresetColumns.fnAiStatusCompare, PresetColumns.fnAiStatusGet));
        columns.get(2).setCellRenderer(new ManaCostRenderer());
        
        top.setup(columns, cardView);
        bottom.setup(columns, cardView);
        
        filterNameTypeSet.setListeners(new OnChangeTextUpdateDisplay(), itemListenerUpdatesDisplay);

        setSize(1024, 740);
        setExtendedState(Frame.MAXIMIZED_BOTH);

    }


    /**
     * <p>
     * Constructor for Gui_DeckEditor.
     * </p>
     */
    public DeckEditor() {
        try {
            filterBoxes = new FilterCheckBoxes(true);
            top = new TableWithCards("Avaliable Cards", true, true);
            bottom = new TableWithCards("Deck", true);
            cardView = new CardPanelHeavy();
            filterNameTypeSet = new FilterNameTypeSetPanel();
            
            jbInit();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }
    }


    private void jbInit() {
        // removeButton.setIcon(upIcon);
        if (!OldGuiNewGame.useLAFFonts.isSelected()) {
            removeButton.setFont(new java.awt.Font("Dialog", 0, 13));
        }
        removeButton.setText("Remove from Deck");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                removeButtonClicked(e);
            }
        });
        addButton.setText("Add to Deck");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                addButton_actionPerformed(e);
            }
        });
        // addButton.setIcon(downIcon);
        if (!OldGuiNewGame.useLAFFonts.isSelected()) {
            addButton.setFont(new java.awt.Font("Dialog", 0, 13));
        }
        clearFilterButton.setText("Clear Filter");
        clearFilterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                clearFilterButton_actionPerformed(e);
            }
        });
        if (!OldGuiNewGame.useLAFFonts.isSelected()) {
            clearFilterButton.setFont(new java.awt.Font("Dialog", 0, 13));
        }
        analysisButton.setText("Deck Analysis");
        analysisButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                analysisButton_actionPerformed(e);
            }
        });
        if (!OldGuiNewGame.useLAFFonts.isSelected()) {
            analysisButton.setFont(new java.awt.Font("Dialog", 0, 13));
        }

        // Type filtering
        Font f = new Font("Tahoma", Font.PLAIN, 10);
        for (JCheckBox box : filterBoxes.allTypes) {
            if (!OldGuiNewGame.useLAFFonts.isSelected()) { box.setFont(f); }
            box.setOpaque(false);
        }

        // Color filtering
        for (JCheckBox box : filterBoxes.allColors) {
            box.setOpaque(false);
        }

        // Do not lower statsLabel any lower, we want this to be visible at 1024
        // x 768 screen size
        this.setTitle("Deck Editor");

        Container pane = this.getContentPane();
        MigLayout layout = new MigLayout("fill");
        pane.setLayout(layout);

        // this.getContentPane().add(landCheckBox,
        // "cell 0 0, egx checkbox, split 16");
        boolean isFirst = true;

        for (JCheckBox box : filterBoxes.allTypes) {
            String growParameter = "grow";
            if (isFirst) {
                growParameter = "cell 0 0, egx checkbox, grow, split 14";
                isFirst = false;
            }
            this.getContentPane().add(box, growParameter);
            box.addItemListener(itemListenerUpdatesDisplay);
        }

        for (JCheckBox box : filterBoxes.allColors) {
            this.getContentPane().add(box, "grow");
            box.addItemListener(itemListenerUpdatesDisplay);
        }

        //this.getContentPane().add(filterButton, "wmin 100, hmin 25, wmax 140, hmax 25, grow");
        this.getContentPane().add(clearFilterButton, "wmin 100, hmin 25, wmax 140, hmax 25, grow");

        this.getContentPane().add(filterNameTypeSet, "cell 0 1, grow");
        this.getContentPane().add(top.getTableDecorated(), "cell 0 2 1 2, pushy, grow");

        this.getContentPane().add(cardView, "cell 1 0 1 8, flowy, grow");
        
        
       
        this.getContentPane().add(top.getLabel(), "cell 0 4");

        this.getContentPane().add(addButton, "w 100, h 49, sg button, cell 0 5, split 4");
        this.getContentPane().add(removeButton, "w 100, h 49, sg button");

        // jLabel4 is used to push the analysis button to the right
        // This will separate this button from the add and remove card buttons
        jLabelAnalysisGap.setText("");
        this.getContentPane().add(jLabelAnalysisGap, "wmin 100, grow");

        this.getContentPane().add(analysisButton, "w 100, h 49, wrap");

        this.getContentPane().add(bottom.getTableDecorated(), "cell 0 6, grow");
        this.getContentPane().add(bottom.getLabel(), "cell 0 7");

        top.getTable().addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) { addCardToDeck(); }
            }
        });

        //javax.swing.JRootPane rootPane = this.getRootPane();
        //rootPane.setDefaultButton(filterButton);
    }

    @Override
    protected Predicate<CardPrinted> buildFilter() {
        return Predicate.and(filterBoxes.buildFilter(), filterNameTypeSet.buildFilter());
    }

    void clearFilterButton_actionPerformed(ActionEvent e) {
        // disable automatic update triggered by listeners
        isFiltersChangeFiringUpdate = false;

        for (JCheckBox box : filterBoxes.allTypes) { if (!box.isSelected()) { box.doClick(); } }
        for (JCheckBox box : filterBoxes.allColors) { if (!box.isSelected()) { box.doClick(); } }

        filterNameTypeSet.clearFilters();

        isFiltersChangeFiringUpdate = true;

        top.setFilter(null);
    }

    void addButton_actionPerformed(ActionEvent e) {
        addCardToDeck();
    }

    void addCardToDeck() {
        CardPrinted card = top.getSelectedCard();
        if (card == null) { return; }

        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");

        bottom.addCard(card);
        if (!isConstructed) {
            top.removeCard(card);
        }
    }

    void removeButtonClicked(ActionEvent e) {
        CardPrinted card = bottom.getSelectedCard();
        if (card == null) { return; }

        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");

        bottom.removeCard(card);
        if (!isConstructed) {
            top.addCard(card);
        }
    }

    // refresh Gui from deck, Gui shows the cards in the deck
    /**
     * <p>
     * refreshGui.
     * </p>
     */
    @SuppressWarnings("unused")
    // refreshGui
    private void refreshGui() {
        Deck deck = Constant.Runtime.HumanDeck[0];
        if (deck == null) // this is just a patch, i know
            deck = new Deck(Constant.Runtime.GameType[0]);

        bottom.setDeck(deck.getMain());

        if (deck.isSealed() || deck.isDraft()) {
            top.setDeck(deck.getSideboard()); // add sideboard to GUI
        } else {
            top.setDeck(CardDb.instance().getAllUniqueCards());
        }
    }

}
