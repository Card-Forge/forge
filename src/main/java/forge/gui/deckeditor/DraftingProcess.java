/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;

import forge.Command;
import forge.Constant;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.error.ErrorViewer;
import forge.game.limited.IBoosterDraft;
import forge.gui.GuiUtils;
import forge.gui.SOverlayUtils;
import forge.gui.deckeditor.elements.CardPanelLite;
import forge.gui.deckeditor.elements.ManaCostRenderer;
import forge.gui.deckeditor.elements.TableColumnInfo;
import forge.gui.deckeditor.elements.TableView;
import forge.gui.home.sanctioned.CSubmenuDraft;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPoolView;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.Lang.GuiBoosterDraft;
import forge.util.closures.Predicate;

/**
 * <p>
 * DeckEditorDraft class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class DraftingProcess extends DeckEditorBase<CardPrinted, DeckGroup> {
    /**
     * Constant <code>serialVersionUID=-6055633915602448260L</code>.
     */
    private static final long serialVersionUID = -6055633915602448260L;

    private IBoosterDraft boosterDraft;

    private final JButton jButtonPick = new JButton();

    private CardPanelLite cardView = new CardPanelLite();

    private final MouseListener pickWithMouse = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
            // Pick on left-button double click
            if (((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) && (e.getClickCount() == 2)) {
                DraftingProcess.this.jButtonPickClicked(null);
            } else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
                // pick on right click
                final JTable table = DraftingProcess.this.getTopTableWithCards().getTable();
                final int rowNumber = table.rowAtPoint(e.getPoint());
                // after hittest - if it was outside of rows - discard this
                // click
                if (rowNumber == -1) {
                    return;
                }

                // if row was not selected, select it. If it was, pick a card
                if (rowNumber != table.getSelectedRow()) {
                    table.getSelectionModel().setSelectionInterval(rowNumber, rowNumber);
                } else {
                    DraftingProcess.this.jButtonPickClicked(null);
                }
            }
        }
    };

    /**
     * Show gui.
     * 
     * @param inBoosterDraft
     *            the in_booster draft
     */
    public final void showGui(final IBoosterDraft inBoosterDraft) {
        this.boosterDraft = inBoosterDraft;
        this.show(null);
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
                final int n = JOptionPane.showConfirmDialog(null,
                        ForgeProps.getLocalized(GuiBoosterDraft.CLOSE_MESSAGE), "", JOptionPane.YES_NO_OPTION);
                if (n == JOptionPane.YES_OPTION) {
                    DraftingProcess.this.dispose();
                    CSubmenuDraft.SINGLETON_INSTANCE.update();
                    SOverlayUtils.hideOverlay();
                }
            } // windowClosing()
        });
    } // addListeners()

    /**
     * <p>
     * setup.
     * </p>
     */
    private void setup() {
        this.addListeners();
        // setupMenu();

        final List<TableColumnInfo<InventoryItem>> columns = new ArrayList<TableColumnInfo<InventoryItem>>();
        columns.add(new TableColumnInfo<InventoryItem>("Qty", 30, PresetColumns.FN_QTY_COMPARE,
                PresetColumns.FN_QTY_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Name", 180, PresetColumns.FN_NAME_COMPARE,
                PresetColumns.FN_NAME_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Cost", 70, PresetColumns.FN_COST_COMPARE,
                PresetColumns.FN_COST_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Color", 50, PresetColumns.FN_COLOR_COMPARE,
                PresetColumns.FN_COLOR_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Type", 100, PresetColumns.FN_TYPE_COMPARE,
                PresetColumns.FN_TYPE_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Stats", 40, PresetColumns.FN_STATS_COMPARE,
                PresetColumns.FN_STATS_GET));
        columns.add(new TableColumnInfo<InventoryItem>("R", 35, PresetColumns.FN_RARITY_COMPARE,
                PresetColumns.FN_RARITY_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Set", 40, PresetColumns.FN_SET_COMPARE,
                PresetColumns.FN_SET_GET));
        columns.add(new TableColumnInfo<InventoryItem>("AI", 30, PresetColumns.FN_AI_STATUS_COMPARE,
                PresetColumns.FN_AI_STATUS_GET));
        columns.get(2).setCellRenderer(new ManaCostRenderer());

        this.getTopTableWithCards().setup(columns, this.cardView);
        this.getBottomTableWithCards().setup(columns, this.cardView);

        this.setSize(980, 740);
        GuiUtils.centerFrame(this);
        this.setResizable(false);

        this.getTopTableWithCards().getTable().addMouseListener(this.pickWithMouse);
        this.getTopTableWithCards().getTable().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    DraftingProcess.this.jButtonPickClicked(null);
                }
            }
        });

    }

    /**
     * Instantiates a new deck editor draft.
     */
    public DraftingProcess(JFrame parent) {
        super(parent);
        try {
            this.setTopTableWithCards(new TableView<CardPrinted>("Choose one card", false, CardPrinted.class));
            this.setBottomTableWithCards(new TableView<CardPrinted>("Previously picked cards", true, CardPrinted.class));
            this.setFilterBoxes(null);
            this.cardView = new CardPanelLite();
            this.jbInit();
        } catch (final Exception ex) {
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

        this.getTopTableWithCards().getTableDecorated().setBounds(new Rectangle(19, 28, 680, 344));
        this.getBottomTableWithCards().getTableDecorated().setBounds(new Rectangle(19, 478, 680, 184));
        this.getBottomTableWithCards().getLabel().setBounds(new Rectangle(19, 680, 665, 31));

        this.cardView.setBounds(new Rectangle(715, 23, 240, 666));

        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setTitle("Booster Draft");

        this.jButtonPick.setBounds(new Rectangle(238, 418, 147, 44));
        this.jButtonPick.setFont(new java.awt.Font("Dialog", 0, 16));
        this.jButtonPick.setText("Choose Card");
        this.jButtonPick.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DraftingProcess.this.jButtonPickClicked(e);
            }
        });

        this.getContentPane().add(this.cardView, null);
        this.getContentPane().add(this.getTopTableWithCards().getTableDecorated(), null);
        this.getContentPane().add(this.getBottomTableWithCards().getLabel(), null);
        this.getContentPane().add(this.getBottomTableWithCards().getTableDecorated(), null);
        this.getContentPane().add(this.jButtonPick, null);
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
        final InventoryItem item = this.getTopTableWithCards().getSelectedCard();
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;

        this.getBottomTableWithCards().addCard(card);

        // get next booster pack
        this.boosterDraft.setChoice(card);
        if (this.boosterDraft.hasNextChoice()) {
            this.showChoices(this.boosterDraft.nextChoice());
        } else {
            this.boosterDraft.finishedDrafting();

            // quit
            this.saveDraft();
            this.dispose();
        }
    } /* OK Button */

    /**
     * <p>
     * showChoices.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    private void showChoices(final ItemPoolView<CardPrinted> list) {
        this.getTopTableWithCards().setDeck(list);
        this.cardView.showCard(null);
        this.getTopTableWithCards().fixSelection(0);
    } // showChoices()

    /**
     * <p>
     * getPlayersDeck.
     * </p>
     * 
     * @return a {@link forge.deck.Deck} object.
     */
    private Deck getPlayersDeck() {
        final Deck deck = new Deck();
        Constant.Runtime.HUMAN_DECK[0] = deck;

        // add sideboard to deck
        deck.getSideboard().addAll(this.getBottomTableWithCards().getCards());

        final String landSet = IBoosterDraft.LAND_SET_CODE[0];
        final int landsCount = 20;
        deck.getSideboard().add(CardDb.instance().getCard("Forest", landSet), landsCount);
        deck.getSideboard().add(CardDb.instance().getCard("Mountain", landSet), landsCount);
        deck.getSideboard().add(CardDb.instance().getCard("Swamp", landSet), landsCount);
        deck.getSideboard().add(CardDb.instance().getCard("Island", landSet), landsCount);
        deck.getSideboard().add(CardDb.instance().getCard("Plains", landSet), landsCount);

        return deck;
    } // getPlayersDeck()

    /**
     * <p>
     * saveDraft.
     * </p>
     */
    private void saveDraft() {
        String s = "";
        while ((s == null) || (s.length() == 0)) {
            s = JOptionPane.showInputDialog(null, ForgeProps.getLocalized(GuiBoosterDraft.SAVE_DRAFT_MESSAGE),
                    ForgeProps.getLocalized(GuiBoosterDraft.SAVE_DRAFT_TITLE), JOptionPane.QUESTION_MESSAGE);
        }
        // TODO: check if overwriting the same name, and let the user delete old
        // drafts

        // construct computer's decks
        // save draft
        final Deck[] computer = this.boosterDraft.getDecks();

        final DeckGroup finishedDraft = new DeckGroup(s);
        finishedDraft.setHumanDeck((Deck) this.getPlayersDeck().copyTo(s));
        finishedDraft.addAiDecks(computer);

        // DeckManager deckManager = new
        // DeckManager(ForgeProps.getFile(NEW_DECKS));
        Singletons.getModel().getDecks().getDraft().add(finishedDraft); // write
                                                                        // file
                                                                        // right
                                                                        // here

        // close and open next screen
        DraftingProcess.this.dispose();

        // This should be handled in the exit command of the editor, not here.
        CSubmenuDraft.SINGLETON_INSTANCE.update();
        SOverlayUtils.hideOverlay();
    } /* saveDraft() */

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckEditorBase#buildFilter()
     */
    @Override
    protected final Predicate<CardPrinted> buildFilter() {
        return Predicate.getTrue(CardPrinted.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckEditorBase#getController()
     */
    @Override
    public DeckController<DeckGroup> getController() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckEditorBase#updateView()
     */
    @Override
    public void updateView() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckEditorBase#show(forge.Command)
     */
    @Override
    public void show(final Command exitCommand) {
        this.setup();
        this.showChoices(this.boosterDraft.nextChoice());
        this.getBottomTableWithCards().setDeck((Iterable<InventoryItem>) null);

        this.getTopTableWithCards().sort(1, true);
        this.getBottomTableWithCards().sort(1, true);

        this.setVisible(true);
    }
}
