package forge.gui.deckeditor;

import forge.AllZone;
import forge.Constant;
import forge.FileUtil;
import forge.HttpUtil;
import forge.card.CardDb;
import forge.card.CardPoolView;
import forge.card.CardPrinted;
import forge.deck.Deck;
import forge.deck.DeckManager;
import forge.error.ErrorViewer;
import forge.game.GameType;
import forge.game.limited.BoosterDraft;
import forge.gui.GuiUtils;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.view.swing.OldGuiNewGame;

import javax.swing.*;

import net.slightlymagic.maxmtg.Predicate;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Gui_BoosterDraft class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class DeckEditorDraft extends DeckEditorBase implements NewConstants, NewConstants.LANG.Gui_BoosterDraft {
    /**
     * Constant <code>serialVersionUID=-6055633915602448260L</code>
     */
    private static final long serialVersionUID = -6055633915602448260L;

    private BoosterDraft boosterDraft;

    private JButton jButton1 = new JButton(); 

    private CardPanelLite cardView = new CardPanelLite();

    /**
     * <p>showGui.</p>
     *
     * @param in_boosterDraft a {@link forge.game.limited.BoosterDraft} object.
     */
    public void showGui(BoosterDraft in_boosterDraft) {
        boosterDraft = in_boosterDraft;

        setup();
        showChoices(boosterDraft.nextChoice());
        bottom.setDeck((CardPoolView)null);
        
        top.sort(1, true);
        bottom.sort(1, true);

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
        columns.add(new TableColumnInfo<CardPrinted>("Qty", 30, PresetColumns.fnQtyCompare, PresetColumns.fnQtyGet));
        columns.add(new TableColumnInfo<CardPrinted>("Name", 180, PresetColumns.fnNameCompare, PresetColumns.fnNameGet));
        columns.add(new TableColumnInfo<CardPrinted>("Cost", 70, PresetColumns.fnCostCompare, PresetColumns.fnCostGet));
        columns.add(new TableColumnInfo<CardPrinted>("Color", 50, PresetColumns.fnColorCompare, PresetColumns.fnColorGet));
        columns.add(new TableColumnInfo<CardPrinted>("Type", 100, PresetColumns.fnTypeCompare, PresetColumns.fnTypeGet));
        columns.add(new TableColumnInfo<CardPrinted>("Stats", 40, PresetColumns.fnStatsCompare, PresetColumns.fnStatsGet));
        columns.add(new TableColumnInfo<CardPrinted>("R", 35, PresetColumns.fnRarityCompare, PresetColumns.fnRarityGet));
        columns.add(new TableColumnInfo<CardPrinted>("Set", 40, PresetColumns.fnSetCompare, PresetColumns.fnSetGet));
        columns.add(new TableColumnInfo<CardPrinted>("AI", 30, PresetColumns.fnAiStatusCompare, PresetColumns.fnAiStatusGet));
        columns.get(2).setCellRenderer(new ManaCostRenderer());

        top.setup(columns, cardView);
        bottom.setup(columns, cardView);
        
        this.setSize(980, 740);
        GuiUtils.centerFrame(this);
        this.setResizable(false);
        
        top.getTable().addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(final KeyEvent e) {
                if (e.getKeyChar() == ' ') { jButton1_actionPerformed(null); }
            }
        });

    }

    public DeckEditorDraft() {
        super(GameType.Draft);
        try {
            top = new TableWithCards("Choose one card", false);
            bottom = new TableWithCards("Previously picked cards", true);
            filterBoxes = null;
            cardView = new CardPanelLite();
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
        this.getContentPane().setLayout(null);

        top.getTableDecorated().setBounds(new Rectangle(19, 28, 680, 344));
        bottom.getTableDecorated().setBounds(new Rectangle(19, 478, 680, 184));
        bottom.getLabel().setBounds(new Rectangle(19, 680, 665, 31));
        
        cardView.setBounds(new Rectangle(715, 23, 240, 666));
        
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.setTitle("Booster Draft");
        
        jButton1.setBounds(new Rectangle(238, 418, 147, 44));
        jButton1.setFont(new java.awt.Font("Dialog", 0, 16));
        jButton1.setText("Choose Card");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton1_actionPerformed(e);
            }
        });
        
        this.getContentPane().add(cardView, null);
        this.getContentPane().add(top.getTableDecorated(), null);
        this.getContentPane().add(bottom.getLabel(), null);
        this.getContentPane().add(bottom.getTableDecorated(), null);
        this.getContentPane().add(jButton1, null);
    }

    /**
     * <p>jButton1_actionPerformed.</p>
     *
     * @param e a {@link java.awt.event.ActionEvent} object.
     */
    void jButton1_actionPerformed(ActionEvent e) {
        CardPrinted card = top.getSelectedCard();
        if ( null == card ) { return; } 

        bottom.addCard(card);

        //get next booster pack
        boosterDraft.setChoice(card);
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
        top.setDeck(list);
        cardView.showCard(null);
        top.fixSelection(0);
    }//showChoices()

    /**
     * <p>getPlayersDeck.</p>
     *
     * @return a {@link forge.deck.Deck} object.
     */
    private Deck getPlayersDeck() {
        Deck deck = new Deck(GameType.Draft);
        Constant.Runtime.HumanDeck[0] = deck;

        //add sideboard to deck
        CardPoolView list = bottom.getCards();
        deck.addSideboard(list);
        
        String landSet = BoosterDraft.LandSetCode[0];
        final int LANDS_COUNT = 20;
        deck.addSideboard(CardDb.instance().getCard("Forest", landSet), LANDS_COUNT);
        deck.addSideboard(CardDb.instance().getCard("Mountain", landSet), LANDS_COUNT);
        deck.addSideboard(CardDb.instance().getCard("Swamp", landSet), LANDS_COUNT);
        deck.addSideboard(CardDb.instance().getCard("Island", landSet), LANDS_COUNT);
        deck.addSideboard(CardDb.instance().getCard("Plains", landSet), LANDS_COUNT);

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

        for(int i = 1; i < all.length; i++) {
            all[i].setName(String.format("Draft %s - Computer %d", s, i));
        }
        
        //DeckManager deckManager = new DeckManager(ForgeProps.getFile(NEW_DECKS));
        DeckManager deckManager = AllZone.getDeckManager();
        deckManager.addDraftDeck(all);

        //write file
        DeckManager.writeDraftDecks(all);

        //close and open next screen
        dispose();
        new OldGuiNewGame();
    }/*saveDraft()*/


    @Override
    protected Predicate<CardPrinted> buildFilter() {
        return CardPrinted.Predicates.Presets.isTrue;
    }
}
