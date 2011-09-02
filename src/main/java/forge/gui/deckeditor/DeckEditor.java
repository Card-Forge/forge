package forge.gui.deckeditor;

import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.commons.lang3.StringUtils;

import net.miginfocom.swing.MigLayout;
import net.slightlymagic.maxmtg.Predicate;
import net.slightlymagic.maxmtg.Predicate.StringOp;
import forge.Command;
import forge.Constant;
import forge.SetInfoUtil;
import forge.card.CardRules;
import forge.card.CardDb;
import forge.card.CardPool;
import forge.card.CardPoolView;
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
public class DeckEditor extends DeckEditorBase implements NewConstants {
    /** Constant <code>serialVersionUID=130339644136746796L</code> */
    private static final long serialVersionUID = 130339644136746796L;

    public DeckEditorMenu customMenu;

    private JButton removeButton = new JButton();
    private JButton addButton = new JButton();
    private JButton analysisButton = new JButton();
    private JScrollPane jScrollPane3 = new JScrollPane();
    private JPanel jPanel3 = new JPanel();
    private GridLayout gridLayout1 = new GridLayout();

    private JLabel labelFilterName = new JLabel();
    private JLabel labelFilterType = new JLabel();
    private JLabel labelFilterRules = new JLabel();
    private JLabel jLabel4 = new JLabel();

    //public JButton filterButton = new JButton();
    private JTextField txtCardName = new JTextField();

    private JTextField txtCardType = new JTextField();
    private JTextField txtCardRules = new JTextField();
    private JComboBox searchSetCombo = new JComboBox();
    private JButton clearFilterButton = new JButton();
    
    private boolean isConstructed = false;

    /** {@inheritDoc} */
    @Override
    public void setTitle(String message) {
        super.setTitle(message);
    }

    /** {@inheritDoc} */
    public void setDecks(CardPoolView topPool, CardPoolView bottomPool) {
        top = new CardPool(topPool);
        topModel.clear();
        topModel.addCards(buildFilter().select(top, CardPoolView.fnToCard));
        topModel.resort();
        topTable.repaint();

        bottom = bottomPool;
        bottomModel.clear();
        bottomModel.addCards(bottom);
        bottomModel.resort();
        bottomTable.repaint();
    }// updateDisplay


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
            public void windowClosing(WindowEvent ev) {
                customMenu.close();
            }
        });

        setup();

        isConstructed = Constant.Runtime.GameType[0].equals(Constant.GameType.Constructed); 

        // show cards, makes this user friendly
        if (isConstructed) {
            customMenu.newConstructed();
        }

        topModel.sort(1, true);
        bottomModel.sort(1, true);

    }// show(Command)


    private void setup() {
        List<TableColumnInfo<CardPrinted>> columns = new ArrayList<TableColumnInfo<CardPrinted>>();
        columns.add(new TableColumnInfo<CardPrinted>("Qty", 30, CardColumnPresets.fnQtyCompare, CardColumnPresets.fnQtyGet));
        columns.add(new TableColumnInfo<CardPrinted>("Name", 180, CardColumnPresets.fnNameCompare, CardColumnPresets.fnNameGet));
        columns.add(new TableColumnInfo<CardPrinted>("Cost", 70, CardColumnPresets.fnCostCompare, CardColumnPresets.fnCostGet));
        columns.add(new TableColumnInfo<CardPrinted>("Color", 50, CardColumnPresets.fnColorCompare, CardColumnPresets.fnColorGet));
        columns.add(new TableColumnInfo<CardPrinted>("Type", 100, CardColumnPresets.fnTypeCompare, CardColumnPresets.fnTypeGet));
        columns.add(new TableColumnInfo<CardPrinted>("Stats", 40, CardColumnPresets.fnStatsCompare, CardColumnPresets.fnStatsGet));
        columns.add(new TableColumnInfo<CardPrinted>("R", 35, CardColumnPresets.fnRarityCompare, CardColumnPresets.fnRarityGet));
        columns.add(new TableColumnInfo<CardPrinted>("Set", 40, CardColumnPresets.fnSetCompare, CardColumnPresets.fnSetGet));
        columns.add(new TableColumnInfo<CardPrinted>("AI", 30, CardColumnPresets.fnAiStatusCompare, CardColumnPresets.fnAiStatusGet));
        setupTables(columns, true);

        // TODO use this as soon the deck editor has resizable GUI
        // Use both so that when "un"maximizing, the frame isn't tiny
        setSize(1024, 740);
        setExtendedState(Frame.MAXIMIZED_BOTH);

        // This was an attempt to limit the width of the deck editor to 1400
        // pixels.
        /*
         * setSize(1024, 740); Rectangle bounds = getBounds(); Dimension screen
         * = getToolkit().getScreenSize(); int maxWidth;
         * 
         * if (screen.width >= 1400) { maxWidth = 1400; } else { maxWidth =
         * screen.width; } bounds.width = maxWidth; bounds.height =
         * screen.height;
         * 
         * setMaximizedBounds(bounds);
         */
    }// setupAndDisplay()


    /**
     * <p>
     * Constructor for Gui_DeckEditor.
     * </p>
     */
    public DeckEditor() {
        super(true, true);
        try {
            jbInit();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }
    }

    /**
     * <p>
     * jbInit.
     * </p>
     * 
     * @throws java.lang.Exception
     *             if any.
     */
    private void jbInit() throws Exception {
        jbInitTables("All Cards", "Deck");

        // removeButton.setIcon(upIcon);
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            removeButton.setFont(new java.awt.Font("Dialog", 0, 13));
        removeButton.setText("Remove from Deck");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeButton_actionPerformed(e);
            }
        });
        addButton.setText("Add to Deck");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addButton_actionPerformed(e);
            }
        });
        // addButton.setIcon(downIcon);
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            addButton.setFont(new java.awt.Font("Dialog", 0, 13));

        clearFilterButton.setText("Clear Filter");
        clearFilterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearFilterButton_actionPerformed(e);
            }
        });
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            clearFilterButton.setFont(new java.awt.Font("Dialog", 0, 13));

        analysisButton.setText("Deck Analysis");
        analysisButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                analysisButton_actionPerformed(e);
            }
        });
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            analysisButton.setFont(new java.awt.Font("Dialog", 0, 13));

        /**
         * Type filtering
         */
        Font f = new Font("Tahoma", Font.PLAIN, 10);
        for (JCheckBox box : filterBoxes.allTypes) {
            if (!OldGuiNewGame.useLAFFonts.isSelected()) { box.setFont(f); }
            box.setOpaque(false);
        }

        /**
         * Color filtering
         */
        for (JCheckBox box : filterBoxes.allColors) {
            box.setOpaque(false);
        }


        // picture.addMouseListener(new CustomListener());
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            statsLabel.setFont(new java.awt.Font("Dialog", 0, 13));
        statsLabel.setText("Total: 0, Creatures: 0, Land: 0");
        // Do not lower statsLabel any lower, we want this to be visible at 1024
        // x 768 screen size
        this.setTitle("Deck Editor");
        jScrollPane3.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jPanel3.setLayout(gridLayout1);
        gridLayout1.setColumns(1);
        gridLayout1.setRows(0);
        statsLabel2.setText("Total: 0, Creatures: 0, Land: 0");
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            statsLabel2.setFont(new java.awt.Font("Dialog", 0, 13));
        /*
         * jLabel1.setText(
         * "Click on the column name (like name or color) to sort the cards");
         */

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

        this.getContentPane().add(jScrollPane1, "cell 0 2 1 2, pushy, grow");

        cardView.jbInit();
        this.getContentPane().add(cardView, "cell 1 0 1 8, flowy, grow");


        labelFilterName.setText("Name:");
        labelFilterName.setToolTipText("Card names must include the text in this field");
        this.getContentPane().add(labelFilterName, "cell 0 1, split 7");
        this.getContentPane().add(txtCardName, "wmin 100, grow");
        txtCardName.getDocument().addDocumentListener(new OnChangeTextUpdateDisplay());
        
/*        txtCardName.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void removeUpdate(final DocumentEvent e) { }
            @Override public void insertUpdate(final DocumentEvent e) { }
            @Override public void changedUpdate(final DocumentEvent e) { updateDisplay(); }
        });
        */

        labelFilterType.setText("Type:");
        labelFilterType.setToolTipText("Card types must include the text in this field");
        this.getContentPane().add(labelFilterType, "");
        this.getContentPane().add(txtCardType, "wmin 100, grow");
        txtCardType.getDocument().addDocumentListener(new OnChangeTextUpdateDisplay());
        labelFilterRules.setText("Text:");
        labelFilterRules.setToolTipText("Card descriptions must include the text in this field");
        this.getContentPane().add(labelFilterRules, "");
        this.getContentPane().add(txtCardRules, "wmin 200, grow");
        txtCardRules.getDocument().addDocumentListener(new OnChangeTextUpdateDisplay());

        searchSetCombo.removeAllItems();
        searchSetCombo.addItem("");
        for (int i = 0; i < SetInfoUtil.getNameList().size(); i++)
            searchSetCombo.addItem(SetInfoUtil.getNameList().get(i));
        searchSetCombo.addItemListener(itemListenerUpdatesDisplay);

        this.getContentPane().add(searchSetCombo, "wmin 150, grow");

        this.getContentPane().add(statsLabel2, "cell 0 4");

        this.getContentPane().add(addButton, "w 100, h 49, sg button, cell 0 5, split 4");
        this.getContentPane().add(removeButton, "w 100, h 49, sg button");

        // jLabel4 is used to push the analysis button to the right
        // This will separate this button from the add and remove card buttons
        jLabel4.setText("");
        this.getContentPane().add(jLabel4, "wmin 100, grow");

        this.getContentPane().add(analysisButton, "w 100, h 49, wrap");

        this.getContentPane().add(jScrollPane2, "cell 0 6, grow");
        this.getContentPane().add(statsLabel, "cell 0 7");

        topTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) { addCardFromTopTableToBottom(); }
            }
        });

        //javax.swing.JRootPane rootPane = this.getRootPane();
        //rootPane.setDefaultButton(filterButton);
    }

    @Override
    protected Predicate<CardRules> buildFilter() {
        List<Predicate<CardRules>> rules = new ArrayList<Predicate<CardRules>>(5);
        rules.add(super.buildFilter());
        if (StringUtils.isNotBlank(txtCardName.getText())) {
            rules.add(CardRules.Predicates.name(StringOp.CONTAINS, txtCardName.getText()));
        }

        if (StringUtils.isNotBlank(txtCardType.getText())) {
            rules.add(CardRules.Predicates.joinedType(StringOp.CONTAINS, txtCardType.getText()));
        }
        
        if (StringUtils.isNotBlank(txtCardRules.getText())) {
            rules.add(CardRules.Predicates.rules(StringOp.CONTAINS, txtCardRules.getText()));
        }
        
        if (searchSetCombo.getSelectedIndex() != 0) {
            String setCode = SetInfoUtil.getCode3ByName(searchSetCombo.getSelectedItem().toString());
            rules.add(CardRules.Predicates.wasPrintedInSet(setCode));
        }

        return rules.size() == 1 ? rules.get(0) : Predicate.and(rules);
    }

    void clearFilterButton_actionPerformed(ActionEvent e) {
        // disable automatic update triggered by listeners
        isFiltersChangeFiringUpdate = false;

        for (JCheckBox box : filterBoxes.allTypes) { if (!box.isSelected()) { box.doClick(); } }
        for (JCheckBox box : filterBoxes.allColors) { if (!box.isSelected()) { box.doClick(); } }

        txtCardName.setText("");
        txtCardType.setText("");
        txtCardRules.setText("");
        searchSetCombo.setSelectedIndex(0);

        // restore automatics ... 
        isFiltersChangeFiringUpdate = true;
        // ... and force update
        updateDisplay();
    } // clearFilterButton_actionPerformed

    /**
     * <p>
     * addButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    void addButton_actionPerformed(ActionEvent e) {
        addCardFromTopTableToBottom();
    }// addButton_actionPerformed


    void addCardFromTopTableToBottom() {
        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");

        int n = topTable.getSelectedRow();
        if (n != -1) {
            CardPrinted c = topModel.rowToCard(n).getKey();
            bottomModel.addCard(c);
            bottomModel.resort();

            if (!customMenu.getGameType().equals(Constant.GameType.Constructed)) {
                top.remove(c);
                topModel.removeCard(c);
                topModel.resort();
            }
            fixSelection(topModel, topTable, n);
        }// if(valid row)        
    }
    
    
    /**
     * <p>
     * removeButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    void removeButton_actionPerformed(ActionEvent e) {
        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");

        int n = bottomTable.getSelectedRow();
        if (n != -1) {
            CardPrinted c = bottomModel.rowToCard(n).getKey();
            bottomModel.removeCard(c);

            if (!Constant.GameType.Constructed.equals(customMenu.getGameType())) {
                topModel.addCard(c);
                topModel.resort();
            }

            fixSelection(bottomModel, bottomTable, n);
        }// if(valid row)
    }//

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

        topModel.clear();
        bottomModel.clear();

        bottomModel.addCards(deck.getMain());

        if (deck.isSealed() || deck.isDraft()) {
            topModel.addCards(deck.getSideboard()); // add sideboard to GUI
        } else {
            topModel.addAllCards(CardDb.instance().getAllUniqueCards());
        }

        topModel.resort();
        bottomModel.resort();
    } // //refreshGui()

    protected class OnChangeTextUpdateDisplay implements DocumentListener {
        //private String lastText = "";
        private void onChange() {
            //String newValue = getTextFromDocument(e.getDocument();
            //System.out.println(String.format("%s --> %s", lastText, nowText));
            if (isFiltersChangeFiringUpdate) { updateDisplay(); }
        }

        private String getTextFromDocument(final Document doc) {
            try {
                return doc.getText(0, doc.getLength());
            } catch (BadLocationException ex) {
                return null;
            }
        }

        @Override public void insertUpdate(DocumentEvent e) { onChange(); }
        @Override public void removeUpdate(DocumentEvent e) { onChange(); }

        // Happend only on ENTER pressed
        @Override public void changedUpdate(DocumentEvent e) { }
    }
    
    
}
