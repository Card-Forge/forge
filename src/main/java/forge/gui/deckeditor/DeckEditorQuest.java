package forge.gui.deckeditor;

import forge.Command;
import forge.Constant;
import forge.card.CardPool;
import forge.card.CardPoolView;
import forge.card.CardPrinted;
import forge.deck.Deck;
import forge.error.ErrorViewer;
import forge.gui.GuiUtils;
import forge.properties.NewConstants;
import forge.quest.data.QuestData;
import forge.view.swing.OldGuiNewGame;


import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

//import forge.quest.data.QuestBoosterPack;

/**
 * <p>
 * Gui_Quest_DeckEditor class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class DeckEditorQuest extends DeckEditorBase implements NewConstants {
    /** Constant <code>serialVersionUID=152061168634545L</code> */
    private static final long serialVersionUID = 152061168634545L;

    DeckEditorQuestMenu customMenu;

    // private ImageIcon upIcon = Constant.IO.upIcon;
    // private ImageIcon downIcon = Constant.IO.downIcon;

    private JButton addButton = new JButton();
    private JButton removeButton = new JButton();
    private JButton analysisButton = new JButton();
    private JLabel labelSortHint = new JLabel();

    private QuestData questData;


    public void show(final Command exitCommand) {
        final Command exit = new Command() {
            private static final long serialVersionUID = -7428793574300520612L;

            public void execute() {
                DeckEditorQuest.this.dispose();
                exitCommand.execute();
            }
        };

        // do not change this!!!!
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent ev) {
                customMenu.close();
            }
        });

        setup();

        customMenu = new DeckEditorQuestMenu(questData, this, exit);
        this.setJMenuBar(customMenu);

        Deck deck = null;

        // open deck that the player used if QuestData has it
        if (Constant.Runtime.HumanDeck[0] != null
                && questData.getDeckNames().contains(Constant.Runtime.HumanDeck[0].getName())) {
            deck = questData.getDeck(Constant.Runtime.HumanDeck[0].getName());
        } else {
            deck = new Deck(Constant.GameType.Sealed);
            deck.setName("");
        }

        // tell Gui_Quest_DeckEditor the name of the deck
        customMenu.setPlayerDeckName(deck.getName());

        CardPoolView bottomPool = deck.getMain();
        CardPool cardpool = new CardPool();
        cardpool.addAll(questData.getCards().getCardpool());

        // remove bottom cards that are in the deck from the card pool
        cardpool.removeAll(bottomPool);

        // show cards, makes this user friendly, lol, well may, ha
        setDecks(cardpool, bottomPool);

        // this affects the card pool
        top.sort(4, true);// sort by type
        top.sort(3, true);// then sort by color

        bottom.sort(1, true);
    } // show(Command)


    /**
     * <p>
     * setup.
     * </p>
     */
    public void setup() {
        List<TableColumnInfo<CardPrinted>> columns = new ArrayList<TableColumnInfo<CardPrinted>>();
        columns.add(new TableColumnInfo<CardPrinted>("Qty", 30, PresetColumns.fnQtyCompare, PresetColumns.fnQtyGet));
        columns.add(new TableColumnInfo<CardPrinted>("Name", 180, PresetColumns.fnNameCompare, PresetColumns.fnNameGet));
        columns.add(new TableColumnInfo<CardPrinted>("Cost", 70, PresetColumns.fnCostCompare, PresetColumns.fnCostGet));
        columns.add(new TableColumnInfo<CardPrinted>("Color", 50, PresetColumns.fnColorCompare, PresetColumns.fnColorGet));
        columns.add(new TableColumnInfo<CardPrinted>("Type", 100, PresetColumns.fnTypeCompare, PresetColumns.fnTypeGet));
        columns.add(new TableColumnInfo<CardPrinted>("Stats", 40, PresetColumns.fnStatsCompare, PresetColumns.fnStatsGet));
        columns.add(new TableColumnInfo<CardPrinted>("R", 35, PresetColumns.fnRarityCompare, PresetColumns.fnRarityGet));
        columns.add(new TableColumnInfo<CardPrinted>("Set", 40, PresetColumns.fnSetCompare, PresetColumns.fnSetGet));
        columns.add(new TableColumnInfo<CardPrinted>("New", 30, questData.getCards().fnNewCompare, questData.getCards().fnNewGet));

        columns.get(2).setCellRenderer(new ManaCostRenderer());

        top.setup(columns, cardView);
        bottom.setup(columns, cardView);

        this.setSize(1024, 768);
        GuiUtils.centerFrame(this);
        this.setResizable(false);

        // TODO use this as soon the deck editor has resizable GUI
        // //Use both so that when "un"maximizing, the frame isn't tiny
        // setSize(1024, 740);
        // setExtendedState(Frame.MAXIMIZED_BOTH);
    } // setupAndDisplay()

    public DeckEditorQuest(QuestData questData2) {
        questData = questData2;
        try {
            filterBoxes = new FilterCheckBoxes(false);
            top = new TableWithCards("All Cards", true);
            bottom = new TableWithCards("Your deck", true);
            cardView = new CardPanelHeavy();
            jbInit();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }
    }


    private void jbInit() throws Exception {
        this.setLayout(null);

        top.getTableDecorated().setBounds(new Rectangle(19, 20, 726, 346));
        bottom.getTableDecorated().setBounds(new Rectangle(19, 458, 726, 218));

        removeButton.setBounds(new Rectangle(180, 403, 146, 49));
        // removeButton.setIcon(upIcon);
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            removeButton.setFont(new java.awt.Font("Dialog", 0, 13));
        removeButton.setText("Remove Card");
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                removeButtonActionPerformed(e);
            }
        });
        addButton.setText("Add Card");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                addButtonActionPerformed(e);
            }
        });
        // addButton.setIcon(downIcon);
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            addButton.setFont(new java.awt.Font("Dialog", 0, 13));
        addButton.setBounds(new Rectangle(23, 403, 146, 49));

        analysisButton.setText("Deck Analysis");
        analysisButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                analysisButton_actionPerformed(e);
            }
        });
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            analysisButton.setFont(new java.awt.Font("Dialog", 0, 13));
        analysisButton.setBounds(new Rectangle(578, 426, 166, 25));

        /**
         * Type filtering
         */
        filterBoxes.land.setBounds(340, 400, 48, 20);
        filterBoxes.creature.setBounds(385, 400, 65, 20);
        filterBoxes.sorcery.setBounds(447, 400, 62, 20);
        filterBoxes.instant.setBounds(505, 400, 60, 20);
        filterBoxes.planeswalker.setBounds(558, 400, 85, 20);
        filterBoxes.artifact.setBounds(638, 400, 58, 20);
        filterBoxes.enchantment.setBounds(692, 400, 80, 20);
        
        Font f = new Font("Tahoma", Font.PLAIN, 10);
        for (JCheckBox box : filterBoxes.allTypes) {
            if (!OldGuiNewGame.useLAFFonts.isSelected()) { box.setFont(f); }
            box.setOpaque(false);
            box.addItemListener(itemListenerUpdatesDisplay);
        }

        /**
         * Color filtering
         */
        filterBoxes.white.setBounds(340, 430, 40, 20);
        filterBoxes.blue.setBounds(380, 430, 40, 20);
        filterBoxes.black.setBounds(420, 430, 40, 20);
        filterBoxes.red.setBounds(460, 430, 40, 20);
        filterBoxes.green.setBounds(500, 430, 40, 20);
        filterBoxes.colorless.setBounds(540, 430, 40, 20);

        for (JCheckBox box : filterBoxes.allColors) {
            box.setOpaque(false);
            box.addItemListener(itemListenerUpdatesDisplay);
        }
        /**
         * Other
         */
        cardView.setBounds(new Rectangle(765, 23, 239, 687));
        top.getLabel().setBounds(new Rectangle(19, 365, 720, 31));
        bottom.getLabel().setBounds(new Rectangle(19, 672, 720, 31));

        // Do not lower statsLabel any lower, we want this to be visible at 1024
        // x 768 screen size
        this.setTitle("Deck Editor");

        labelSortHint.setText("Click on the column name (like name or color) to sort the cards");
        labelSortHint.setBounds(new Rectangle(20, 1, 400, 19));

        this.getContentPane().add(top.getTableDecorated(), null);
        this.getContentPane().add(bottom.getTableDecorated(), null);
        this.getContentPane().add(addButton, null);
        this.getContentPane().add(removeButton, null);
        this.getContentPane().add(analysisButton, null);
        this.getContentPane().add(bottom.getLabel(), null);
        this.getContentPane().add(top.getLabel(), null);
        this.getContentPane().add(labelSortHint, null);
        this.getContentPane().add(cardView, null);

        for (JCheckBox box : filterBoxes.allTypes) {
            this.getContentPane().add(box, null);
        }

        for (JCheckBox box : filterBoxes.allColors) {
            this.getContentPane().add(box, null);
        }

        top.getTable().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyChar() == ' ') { addButtonActionPerformed(null); }
            }
        });
    }


    private  void addButtonActionPerformed(final ActionEvent e) {
        CardPrinted card = top.getSelectedCard();
        if (card == null) { return; }

        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");

        top.removeCard(card);
        bottom.addCard(card);
    }


    private void removeButtonActionPerformed(final ActionEvent e) {
        CardPrinted card = bottom.getSelectedCard();
        if (card == null) { return; }

        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");

        top.addCard(card);
        bottom.removeCard(card);
    }


    public void addCheatCard(final CardPrinted card) {
        top.addCard(card);
        questData.getCards().getCardpool().add(card);
    }

}
