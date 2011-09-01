package forge.gui.deckeditor;

import forge.AllZone;
import forge.BoosterDraft;
import forge.CardList;
import forge.Constant;
import forge.FileUtil;
import forge.HttpUtil;
import forge.Constant.GameType;
import forge.Constant.Runtime;
import forge.card.CardPoolView;
import forge.card.CardPrinted;
import forge.deck.Deck;
import forge.deck.DeckManager;
import forge.error.ErrorViewer;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPicturePanel;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.view.swing.OldGuiNewGame;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * <p>Gui_BoosterDraft class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Gui_BoosterDraft extends JFrame implements NewConstants, NewConstants.LANG.Gui_BoosterDraft {
    /**
     * Constant <code>serialVersionUID=-6055633915602448260L</code>
     */
    private static final long serialVersionUID = -6055633915602448260L;

    private BoosterDraft boosterDraft;

    /**
     * Constant <code>limitedDeckEditor=true</code>
     */
    private static final boolean limitedDeckEditor = true;

    private TableModel allCardModel;
    private TableModel deckModel;

    private JScrollPane jScrollPane1 = new JScrollPane();
    private JScrollPane jScrollPane2 = new JScrollPane();
    private TitledBorder titledBorder1;
    private TitledBorder titledBorder2;

    private JLabel statsLabel = new JLabel();
    private JTable allCardTable = new JTable();
    private JTable deckTable = new JTable();
    private JScrollPane jScrollPane3 = new JScrollPane();
    private JPanel jPanel3 = new JPanel();
    private GridLayout gridLayout1 = new GridLayout();
    private JLabel statsLabel2 = new JLabel();
    private JButton jButton1 = new JButton();

    private CardViewPanelLite cardView = new CardViewPanelLite();

    /**
     * <p>showGui.</p>
     *
     * @param in_boosterDraft a {@link forge.BoosterDraft} object.
     */
    public void showGui(BoosterDraft in_boosterDraft) {
        boosterDraft = in_boosterDraft;

        setup();
        showChoices(boosterDraft.nextChoice());

        allCardModel.sort(1, true);
        deckModel.sort(1, true);

        setVisible(true);
    }

    /**
     * <p>addListeners.</p>
     */
    private void addListeners() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                int n = JOptionPane.showConfirmDialog(null, ForgeProps.getLocalized(CLOSE_MESSAGE), "",
                        JOptionPane.YES_NO_OPTION);
                if (n == JOptionPane.YES_OPTION) {
                    dispose();
                    new OldGuiNewGame();
                }
            }//windowClosing()
        });
    }//addListeners()

    /**
     * <p>setup.</p>
     */
    private void setup() {
        addListeners();
//    setupMenu();

        
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
        
        //construct allCardTable, get all cards
        allCardModel = new TableModel(cardView, columns);
        allCardModel.addListeners(allCardTable);
        allCardTable.setModel(allCardModel);
        allCardModel.resizeCols(allCardTable);

        //construct deckModel
        deckModel = new TableModel(cardView, columns);
        deckModel.addListeners(deckTable);
        deckTable.setModel(deckModel);
        deckModel.resizeCols(deckTable);

        //add cards to GUI from deck
//    refreshGui();

        allCardTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) jButton1_actionPerformed(null);
            }
        });//MouseListener


        //get stats from deck
        deckModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent ev) {
                statsLabel.setText(getStats(deckModel.getCards()));
            }
        });


        //get stats from all cards
        allCardModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent ev) {

                statsLabel2.setText(getStats(allCardModel.getCards()));
            }
        });

        //Use both so that when "un"maximizing, the frame isn't tiny
        setSize(1024, 740);
        //setExtendedState(Frame.MAXIMIZED_BOTH);
    }//setupAndDisplay()


    private String getStats(final CardPoolView deck) {
        return DeckEditorBase.getStats(deck);
    }

    public Gui_BoosterDraft() {
        try {
            jbInit();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }
    }


    /**
     * <p>jbInit.</p>
     *
     * @throws java.lang.Exception if any.
     */
    private void jbInit() throws Exception {
        titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
                "Previously Picked Cards");
        titledBorder2 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)),
                "Choose one card");

        this.getContentPane().setLayout(null);
        jScrollPane1.setBorder(titledBorder2);
        jScrollPane1.setBounds(new Rectangle(19, 28, 661, 344));
        jScrollPane2.setBorder(titledBorder1);
        jScrollPane2.setBounds(new Rectangle(19, 478, 661, 184));

        cardView.jbInit();
        cardView.setBounds(new Rectangle(693, 23, 239, 665));

        statsLabel.setFont(new java.awt.Font("Dialog", 0, 16));
        statsLabel.setText("Total - 0, Creatures - 0 Land - 0");
        statsLabel.setBounds(new Rectangle(19, 665, 665, 31));
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.setTitle("Booster Draft");
        jScrollPane3.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane3.setBounds(new Rectangle(6, 168, 225, 143));
        jPanel3.setBounds(new Rectangle(7, 21, 224, 141));
        jPanel3.setLayout(gridLayout1);
        gridLayout1.setColumns(1);
        gridLayout1.setRows(0);
        statsLabel2.setBounds(new Rectangle(19, 378, 665, 31));
        statsLabel2.setText("Total - 0, Creatures - 0 Land - 0");
        statsLabel2.setFont(new java.awt.Font("Dialog", 0, 16));
        jButton1.setBounds(new Rectangle(238, 418, 147, 44));
        jButton1.setFont(new java.awt.Font("Dialog", 0, 16));
        jButton1.setText("Choose Card");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton1_actionPerformed(e);
            }
        });
        this.getContentPane().add(cardView, null);
        this.getContentPane().add(jScrollPane1, null);
        this.getContentPane().add(statsLabel2, null);
        this.getContentPane().add(statsLabel, null);
        this.getContentPane().add(jScrollPane2, null);
        this.getContentPane().add(jButton1, null);
        jScrollPane2.getViewport().add(deckTable, null);
        jScrollPane1.getViewport().add(allCardTable, null);
    }

    /**
     * <p>addButton_actionPerformed.</p>
     *
     * @param e a {@link java.awt.event.ActionEvent} object.
     */
    void addButton_actionPerformed(ActionEvent e) {
        int n = allCardTable.getSelectedRow();
        if (n != -1) {
            setTitle("Deck Editor - " + Constant.Runtime.HumanDeck[0].getName() + " - changed");

            CardPrinted c = allCardModel.rowToCard(n).getKey();
            deckModel.addCard(c);
            deckModel.resort();

            allCardModel.removeCard(c);

            //3 conditions" 0 cards left, select the same row, select next row
            int size = allCardModel.getRowCount();
            if (size != 0) {
                if (size == n) n--;
                allCardTable.addRowSelectionInterval(n, n);
            }
        }//if(valid row)
    }//addButton_actionPerformed

    /**
     * <p>removeButton_actionPerformed.</p>
     *
     * @param e a {@link java.awt.event.ActionEvent} object.
     */
    void removeButton_actionPerformed(ActionEvent e) {
        int n = deckTable.getSelectedRow();
        if (n != -1) {
            setTitle("Deck Editor - " + Constant.Runtime.HumanDeck[0].getName() + " - changed");

            CardPrinted c = deckModel.rowToCard(n).getKey();
            deckModel.removeCard(c);

            if (limitedDeckEditor) {
                allCardModel.addCard(c);
                allCardModel.resort();
            }

            //3 conditions" 0 cards left, select the same row, select next row
            int size = deckModel.getRowCount();
            if (size != 0) {
                if (size == n) n--;
                deckTable.addRowSelectionInterval(n, n);
            }
        }//if(valid row)
    }//removeButton_actionPerformed

    //if true, don't do anything else


    /**
     * <p>refreshDeck.</p>
     */
    @SuppressWarnings("unused")
    // refreshDeck
    private void refreshDeck() {
        //make new Deck
        Deck deck = new Deck(Constant.Runtime.GameType[0]);
        deck.setName(Constant.Runtime.HumanDeck[0].getName());
        Constant.Runtime.HumanDeck[0] = deck;

        //update Deck with cards shown in GUI
        
        deck.addMain(deckModel.getCards());
        if (deck.isSealed()) {
            deck.addSideboard(allCardModel.getCards());
        }
    }/* refreshDeck() */

    /**
     * <p>jButton1_actionPerformed.</p>
     *
     * @param e a {@link java.awt.event.ActionEvent} object.
     */
    void jButton1_actionPerformed(ActionEvent e) {
        //pick card
        int n = allCardTable.getSelectedRow();
        if (n == -1) //is valid selection?
            return;

        CardPrinted c = allCardModel.rowToCard(n).getKey();

        deckModel.addCard(c);
        deckModel.resort();

        //get next booster pack
        boosterDraft.setChoice(c);
        if (boosterDraft.hasNextChoice()) {
            showChoices(boosterDraft.nextChoice());
        } else {
            if (Constant.Runtime.UpldDrft[0]) {
                if (BoosterDraft.draftPicks.size() > 1) {
                    ArrayList<String> outDraftData = new ArrayList<String>();

                    String keys[] = {""};
                    keys = BoosterDraft.draftPicks.keySet().toArray(keys);

                    for (int i = 0; i < keys.length; i++) {
                        outDraftData.add(keys[i] + "|" + BoosterDraft.draftPicks.get(keys[i]));
                    }

                    FileUtil.writeFile("res/draft/tmpDraftData.txt", outDraftData);

                    HttpUtil poster = new HttpUtil();
                    poster.upload("http://cardforge.org/draftAI/submitDraftData.php?fmt=" + BoosterDraft.draftFormat[0], "res/draft/tmpDraftData.txt");
                }
            }

            //quit
            saveDraft();
            dispose();
        }
    }/*OK Button*/

    /**
     * <p>showChoices.</p>
     *
     * @param list a {@link forge.CardList} object.
     */
    private void showChoices(CardPoolView list) {
        allCardModel.clear();
        allCardModel.addCards(list);
        allCardModel.resort();
        allCardTable.setRowSelectionInterval(0, 0);

    }//showChoices()

    /**
     * <p>getPlayersDeck.</p>
     *
     * @return a {@link forge.deck.Deck} object.
     */
    private Deck getPlayersDeck() {
        Deck deck = new Deck(Constant.GameType.Draft);
        Constant.Runtime.HumanDeck[0] = deck;

        //add sideboard to deck
        CardPoolView list = deckModel.getCards();
        deck.addSideboard(list);

        for (int i = 0; i < 20; i++) {
            deck.addSideboard("Forest|" + BoosterDraft.LandSetCode[0]);
            deck.addSideboard("Mountain|" + BoosterDraft.LandSetCode[0]);
            deck.addSideboard("Swamp|" + BoosterDraft.LandSetCode[0]);
            deck.addSideboard("Island|" + BoosterDraft.LandSetCode[0]);
            deck.addSideboard("Plains|" + BoosterDraft.LandSetCode[0]);
        }

        return deck;
    }//getPlayersDeck()

    /**
     * <p>saveDraft.</p>
     */
    private void saveDraft() {
        String s = "";
        while (s == null || s.length() == 0) {
            s = JOptionPane.showInputDialog(null, ForgeProps.getLocalized(SAVE_DRAFT_MESSAGE),
                    ForgeProps.getLocalized(SAVE_DRAFT_TITLE), JOptionPane.QUESTION_MESSAGE);
        }
        //TODO: check if overwriting the same name, and let the user delete old drafts

        //construct computer's decks
        //save draft
        Deck[] computer = boosterDraft.getDecks();

        Deck human = getPlayersDeck();
        human.setName(s);

        Deck[] all = {
                human, computer[0], computer[1], computer[2], computer[3], computer[4], computer[5], computer[6]};

        //DeckManager deckManager = new DeckManager(ForgeProps.getFile(NEW_DECKS));
        DeckManager deckManager = AllZone.getDeckManager();
        deckManager.addDraftDeck(all);

        //write file
        deckManager.close();

        //close and open next screen
        dispose();
        new OldGuiNewGame();
    }/*saveDraft()*/
}
