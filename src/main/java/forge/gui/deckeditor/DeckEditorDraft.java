package forge.gui.deckeditor;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTable;

import net.slightlymagic.maxmtg.Predicate;
import forge.AllZone;
import forge.Constant;
import forge.deck.Deck;
import forge.deck.DeckManager;
import forge.error.ErrorViewer;
import forge.game.GameType;
import forge.game.limited.BoosterDraft;
import forge.gui.GuiUtils;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.view.swing.Gui_HomeScreen;
import forge.view.swing.OldGuiNewGame;

/**
 * <p>
 * Gui_BoosterDraft class.
 * </p>
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

    private JButton jButtonPick = new JButton();

    private CardPanelLite cardView = new CardPanelLite();

    private MouseListener pickWithMouse = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
            // Pick on left-button double click
            if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0 && e.getClickCount() == 2) {
                jButtonPickClicked(null);
            } else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) { // pick
                                                                            // on
                                                                            // right
                                                                            // click
                JTable table = top.getTable();
                int rowNumber = table.rowAtPoint(e.getPoint());
                // after hittest - if it was outside of rows - discard this
                // click
                if (rowNumber == -1) {
                    return;
                }

                // if row was not selected, select it. If it was, pick a card
                if (rowNumber != table.getSelectedRow()) {
                    table.getSelectionModel().setSelectionInterval(rowNumber, rowNumber);
                } else {
                    jButtonPickClicked(null);
                }
            }
        }
    };

    /**
     * Show gui.
     * 
     * @param in_boosterDraft
     *            the in_booster draft
     */
    public final void showGui(final BoosterDraft in_boosterDraft) {
        boosterDraft = in_boosterDraft;

        setup();
        showChoices(boosterDraft.nextChoice());
        bottom.setDeck((Iterable<InventoryItem>) null);

        top.sort(1, true);
        bottom.sort(1, true);

        setVisible(true);
    }

    /**
     * <p>
     * addListeners.
     * </p>
     */
    private void addListeners() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent ev) {
                int n = JOptionPane.showConfirmDialog(null, ForgeProps.getLocalized(CLOSE_MESSAGE), "",
                        JOptionPane.YES_NO_OPTION);
                if (n == JOptionPane.YES_OPTION) {
                    dispose();

                    if (System.getenv("NG2") != null) {
                        if (System.getenv("NG2").equalsIgnoreCase("true")) {
                            String[] argz = {};
                            Gui_HomeScreen.main(argz);
                        } else {
                            new OldGuiNewGame();
                        }
                    } else {
                        new OldGuiNewGame();
                    }

                }
            }// windowClosing()
        });
    }// addListeners()

    /**
     * <p>
     * setup.
     * </p>
     */
    private void setup() {
        addListeners();
        // setupMenu();

        List<TableColumnInfo<InventoryItem>> columns = new ArrayList<TableColumnInfo<InventoryItem>>();
        columns.add(new TableColumnInfo<InventoryItem>("Qty", 30, PresetColumns.fnQtyCompare, PresetColumns.fnQtyGet));
        columns.add(new TableColumnInfo<InventoryItem>("Name", 180, PresetColumns.fnNameCompare,
                PresetColumns.fnNameGet));
        columns.add(new TableColumnInfo<InventoryItem>("Cost", 70, PresetColumns.fnCostCompare, PresetColumns.fnCostGet));
        columns.add(new TableColumnInfo<InventoryItem>("Color", 50, PresetColumns.fnColorCompare,
                PresetColumns.fnColorGet));
        columns.add(new TableColumnInfo<InventoryItem>("Type", 100, PresetColumns.fnTypeCompare,
                PresetColumns.fnTypeGet));
        columns.add(new TableColumnInfo<InventoryItem>("Stats", 40, PresetColumns.fnStatsCompare,
                PresetColumns.fnStatsGet));
        columns.add(new TableColumnInfo<InventoryItem>("R", 35, PresetColumns.fnRarityCompare,
                PresetColumns.fnRarityGet));
        columns.add(new TableColumnInfo<InventoryItem>("Set", 40, PresetColumns.fnSetCompare, PresetColumns.fnSetGet));
        columns.add(new TableColumnInfo<InventoryItem>("AI", 30, PresetColumns.fnAiStatusCompare,
                PresetColumns.fnAiStatusGet));
        columns.get(2).setCellRenderer(new ManaCostRenderer());

        top.setup(columns, cardView);
        bottom.setup(columns, cardView);

        this.setSize(980, 740);
        GuiUtils.centerFrame(this);
        this.setResizable(false);

        top.getTable().addMouseListener(pickWithMouse);
        top.getTable().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    jButtonPickClicked(null);
                }
            }
        });

    }

    /**
     * Instantiates a new deck editor draft.
     */
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
     * <p>
     * jbInit.
     * </p>
     * 
     * @throws java.lang.Exception
     *             if any.
     */
    private void jbInit() throws Exception {
        this.getContentPane().setLayout(null);

        top.getTableDecorated().setBounds(new Rectangle(19, 28, 680, 344));
        bottom.getTableDecorated().setBounds(new Rectangle(19, 478, 680, 184));
        bottom.getLabel().setBounds(new Rectangle(19, 680, 665, 31));

        cardView.setBounds(new Rectangle(715, 23, 240, 666));

        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.setTitle("Booster Draft");

        jButtonPick.setBounds(new Rectangle(238, 418, 147, 44));
        jButtonPick.setFont(new java.awt.Font("Dialog", 0, 16));
        jButtonPick.setText("Choose Card");
        jButtonPick.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                jButtonPickClicked(e);
            }
        });

        this.getContentPane().add(cardView, null);
        this.getContentPane().add(top.getTableDecorated(), null);
        this.getContentPane().add(bottom.getLabel(), null);
        this.getContentPane().add(bottom.getTableDecorated(), null);
        this.getContentPane().add(jButtonPick, null);
    }

    /**
     * <p>
     * jButton1_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    final void jButtonPickClicked(final ActionEvent e) {
        InventoryItem item = top.getSelectedCard();
        if (item == null || !(item instanceof CardPrinted)) {
            return;
        }

        CardPrinted card = (CardPrinted) item;

        bottom.addCard(card);

        // get next booster pack
        boosterDraft.setChoice(card);
        if (boosterDraft.hasNextChoice()) {
            showChoices(boosterDraft.nextChoice());
        } else {
            boosterDraft.finishedDrafting();

            // quit
            saveDraft();
            dispose();
        }
    }/* OK Button */

    /**
     * <p>
     * showChoices.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    private void showChoices(final ItemPoolView<CardPrinted> list) {
        top.setDeck(list);
        cardView.showCard(null);
        top.fixSelection(0);
    }// showChoices()

    /**
     * <p>
     * getPlayersDeck.
     * </p>
     * 
     * @return a {@link forge.deck.Deck} object.
     */
    private Deck getPlayersDeck() {
        Deck deck = new Deck(GameType.Draft);
        Constant.Runtime.HumanDeck[0] = deck;

        // add sideboard to deck
        ItemPoolView<CardPrinted> list = ItemPool.createFrom(bottom.getCards(), CardPrinted.class);
        deck.addSideboard(list);

        String landSet = BoosterDraft.LandSetCode[0];
        final int LANDS_COUNT = 20;
        deck.addSideboard(CardDb.instance().getCard("Forest", landSet), LANDS_COUNT);
        deck.addSideboard(CardDb.instance().getCard("Mountain", landSet), LANDS_COUNT);
        deck.addSideboard(CardDb.instance().getCard("Swamp", landSet), LANDS_COUNT);
        deck.addSideboard(CardDb.instance().getCard("Island", landSet), LANDS_COUNT);
        deck.addSideboard(CardDb.instance().getCard("Plains", landSet), LANDS_COUNT);

        return deck;
    }// getPlayersDeck()

    /**
     * <p>
     * saveDraft.
     * </p>
     */
    private void saveDraft() {
        String s = "";
        while (s == null || s.length() == 0) {
            s = JOptionPane.showInputDialog(null, ForgeProps.getLocalized(SAVE_DRAFT_MESSAGE),
                    ForgeProps.getLocalized(SAVE_DRAFT_TITLE), JOptionPane.QUESTION_MESSAGE);
        }
        // TODO: check if overwriting the same name, and let the user delete old
        // drafts

        // construct computer's decks
        // save draft
        Deck[] computer = boosterDraft.getDecks();

        Deck human = getPlayersDeck();
        human.setName(s);

        Deck[] all = { human, computer[0], computer[1], computer[2], computer[3], computer[4], computer[5], computer[6] };

        for (int i = 1; i < all.length; i++) {
            all[i].setName(String.format("Draft %s - Computer %d", s, i));
        }

        // DeckManager deckManager = new
        // DeckManager(ForgeProps.getFile(NEW_DECKS));
        DeckManager deckManager = AllZone.getDeckManager();
        deckManager.addDraftDeck(all);

        // write file
        DeckManager.writeDraftDecks(all);

        // close and open next screen
        dispose();

        if (System.getenv("NG2") != null) {
            if (System.getenv("NG2").equalsIgnoreCase("true")) {
                String[] argz = {};
                Gui_HomeScreen.main(argz);
            } else {
                new OldGuiNewGame();
            }
        } else {
            new OldGuiNewGame();
        }

    }/* saveDraft() */

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckEditorBase#buildFilter()
     */
    @Override
    protected final Predicate<InventoryItem> buildFilter() {
        return Predicate.getTrue(InventoryItem.class);
    }
}
