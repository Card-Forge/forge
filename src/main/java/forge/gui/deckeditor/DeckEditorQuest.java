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

// import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;

import net.slightlymagic.maxmtg.Predicate;
import forge.Command;
import forge.Constant;
import forge.deck.Deck;
import forge.error.ErrorViewer;
import forge.game.GameType;
import forge.gui.GuiUtils;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;
import forge.quest.data.QuestData;

//import forge.quest.data.QuestBoosterPack;

/**
 * <p>
 * Gui_Quest_DeckEditor class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class DeckEditorQuest extends DeckEditorBase {
    /** Constant <code>serialVersionUID=152061168634545L</code>. */
    private static final long serialVersionUID = 152061168634545L;

    /** The custom menu. */
    private DeckEditorQuestMenu customMenu;

    // private ImageIcon upIcon = Constant.IO.upIcon;
    // private ImageIcon downIcon = Constant.IO.downIcon;

    // private JLabel labelSortHint = new JLabel();
    private final JButton addButton = new JButton();
    private final JButton removeButton = new JButton();
    private final JButton analysisButton = new JButton();

    private FilterNameTypeSetPanel filterNameTypeSet;

    private final QuestData questData;

    /**
     * Show.
     * 
     * @param exitCommand
     *            the exit command
     */
    public void show(final Command exitCommand) {
        final Command exit = new Command() {
            private static final long serialVersionUID = -7428793574300520612L;

            @Override
            public void execute() {
                DeckEditorQuest.this.dispose();
                exitCommand.execute();
            }
        };

        // do not change this!!!!
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent ev) {
                DeckEditorQuest.this.customMenu.close();
            }
        });

        this.setup();

        this.customMenu = new DeckEditorQuestMenu(this.questData, this, exit);
        this.setJMenuBar(this.customMenu);

        Deck deck = null;

        // open deck that the player used if QuestData has it
        if ((Constant.Runtime.HUMAN_DECK[0] != null)
                && this.questData.getDeckNames().contains(Constant.Runtime.HUMAN_DECK[0].getName())) {
            deck = this.questData.getDeck(Constant.Runtime.HUMAN_DECK[0].getName());
        } else {
            deck = new Deck(GameType.Sealed);
            deck.setName("");
        }

        // tell Gui_Quest_DeckEditor the name of the deck
        this.customMenu.setPlayerDeckName(deck.getName());

        final ItemPoolView<CardPrinted> bottomPool = deck.getMain();
        final ItemPool<CardPrinted> cardpool = new ItemPool<CardPrinted>(CardPrinted.class);
        cardpool.addAll(this.questData.getCards().getCardpool());

        // remove bottom cards that are in the deck from the card pool
        cardpool.removeAll(bottomPool);

        // show cards, makes this user friendly
        this.setDeck(cardpool, bottomPool, GameType.Quest);

        // this affects the card pool
        this.getTopTableWithCards().sort(4, true); // sort by type
        this.getTopTableWithCards().sort(3, true); // then sort by color

        this.getBottomTableWithCards().sort(1, true);
    } // show(Command)

    /**
     * <p>
     * setup.
     * </p>
     */
    public void setup() {
        this.setLayout(null);

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
        columns.add(new TableColumnInfo<InventoryItem>("New", 30, this.questData.getCards().getFnNewCompare(),
                this.questData.getCards().getFnNewGet()));

        columns.get(2).setCellRenderer(new ManaCostRenderer());

        this.getTopTableWithCards().setup(columns, this.getCardView());
        this.getBottomTableWithCards().setup(columns, this.getCardView());

        this.filterNameTypeSet.setListeners(new OnChangeTextUpdateDisplay(), this.getItemListenerUpdatesDisplay());

        // Window is too tall, lower height to min size used by constructed mode deck editor
        // this.setSize(1024, 768);
        this.setSize(1024, 740);
        GuiUtils.centerFrame(this);
        this.setResizable(false);

        // TODO use this as soon the deck editor has resizable GUI
        // //Use both so that when "un"maximizing, the frame isn't tiny
        // setSize(1024, 740);
        // setExtendedState(Frame.MAXIMIZED_BOTH);
    } // setupAndDisplay()

    /**
     * Instantiates a new deck editor quest.
     * 
     * @param questData2
     *            the quest data2
     */
    public DeckEditorQuest(final QuestData questData2) {
        super(GameType.Quest);
        this.questData = questData2;
        try {
            this.setFilterBoxes(new FilterCheckBoxes(false));
            this.setTopTableWithCards(new TableWithCards("All Cards", true));
            this.setBottomTableWithCards(new TableWithCards("Your deck", true));
            this.setCardView(new CardPanelHeavy());
            this.filterNameTypeSet = new FilterNameTypeSetPanel();
            this.jbInit();
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
        }
    }

    private void jbInit() throws Exception {
        this.getContentPane().setLayout(null);

        // labelSortHint.setText("Click on the column name (like name or color) to sort the cards");
        // labelSortHint.setBounds(new Rectangle(20, 27, 400, 19));

        this.filterNameTypeSet.setBounds(new Rectangle(19, 55, 726, 31));
        this.getTopTableWithCards().getTableDecorated().setBounds(new Rectangle(19, 96, 726, 283));
        this.getBottomTableWithCards().getTableDecorated().setBounds(new Rectangle(19, 444, 726, 219));

        this.removeButton.setBounds(new Rectangle(180, 408, 146, 25));
        // removeButton.setIcon(upIcon);
        this.removeButton.setFont(new java.awt.Font("Dialog", 0, 13));
        this.removeButton.setText("Remove Card");
        this.removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckEditorQuest.this.removeButtonActionPerformed(e);
            }
        });
        this.addButton.setText("Add Card");
        this.addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckEditorQuest.this.addButtonActionPerformed(e);
            }
        });
        // addButton.setIcon(downIcon);
        this.addButton.setFont(new java.awt.Font("Dialog", 0, 13));
        this.addButton.setBounds(new Rectangle(23, 408, 146, 25));

        this.analysisButton.setText("Deck Analysis");
        this.analysisButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckEditorQuest.this.analysisButtonActionPerformed(e);
            }
        });
        this.analysisButton.setFont(new java.awt.Font("Dialog", 0, 13));
        this.analysisButton.setBounds(new Rectangle(582, 408, 166, 25));

        /**
         * Type filtering
         */
        // Raise the filter boxes to top of window and move to the left.
        // Need to replace the text based filters with the graphic version
        this.getFilterBoxes().getLand().setBounds(17, 35, 62, 20);
        this.getFilterBoxes().getCreature().setBounds(88, 35, 87, 20);
        this.getFilterBoxes().getSorcery().setBounds(183, 35, 80, 20);
        this.getFilterBoxes().getInstant().setBounds(270, 35, 80, 20);
        this.getFilterBoxes().getPlaneswalker().setBounds(355, 35, 117, 20);
        this.getFilterBoxes().getArtifact().setBounds(479, 35, 80, 20);
        this.getFilterBoxes().getEnchantment().setBounds(570, 35, 115, 20);

        for (final JCheckBox box : this.getFilterBoxes().getAllTypes()) {
            // box.setFont(f);
            box.setOpaque(false);
            box.addItemListener(this.getItemListenerUpdatesDisplay());
        }

        /**
         * Color filtering
         */
        // Raise the color filtering boxes to top of window and move to the left.
        this.getFilterBoxes().getWhite().setBounds(17, 10, 67, 20);
        this.getFilterBoxes().getBlue().setBounds(94, 10, 60, 20);
        this.getFilterBoxes().getBlack().setBounds(162, 10, 65, 20);
        this.getFilterBoxes().getRed().setBounds(237, 10, 55, 20);
        this.getFilterBoxes().getGreen().setBounds(302, 10, 70, 20);
        this.getFilterBoxes().getColorless().setBounds(380, 10, 100, 20);

        for (final JCheckBox box : this.getFilterBoxes().getAllColors()) {
            box.setOpaque(false);
            box.addItemListener(this.getItemListenerUpdatesDisplay());
        }
        /**
         * Other
         */
        this.getCardView().setBounds(new Rectangle(765, 16, 239, 662));
        this.getTopTableWithCards().getLabel().setBounds(new Rectangle(19, 375, 720, 31));
        this.getBottomTableWithCards().getLabel().setBounds(new Rectangle(19, 660, 720, 31));

        this.setTitle("Deck Editor");

        this.getContentPane().add(this.filterNameTypeSet, null);
        this.getContentPane().add(this.getTopTableWithCards().getTableDecorated(), null);
        this.getContentPane().add(this.getBottomTableWithCards().getTableDecorated(), null);
        this.getContentPane().add(this.addButton, null);
        this.getContentPane().add(this.removeButton, null);
        this.getContentPane().add(this.analysisButton, null);
        this.getContentPane().add(this.getBottomTableWithCards().getLabel(), null);
        this.getContentPane().add(this.getTopTableWithCards().getLabel(), null);
        // this.getContentPane().add(labelSortHint, null);
        this.getContentPane().add(this.getCardView(), null);

        for (final JCheckBox box : this.getFilterBoxes().getAllTypes()) {
            this.getContentPane().add(box, null);
        }

        for (final JCheckBox box : this.getFilterBoxes().getAllColors()) {
            this.getContentPane().add(box, null);
        }

        this.getTopTableWithCards().getTable().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    DeckEditorQuest.this.addButtonActionPerformed(null);
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckEditorBase#buildFilter()
     */
    @Override
    protected Predicate<InventoryItem> buildFilter() {
        final Predicate<CardPrinted> cardFilter = Predicate.and(this.getFilterBoxes().buildFilter(),
                this.filterNameTypeSet.buildFilter());
        return Predicate.instanceOf(cardFilter, CardPrinted.class);
    }

    private void addButtonActionPerformed(final ActionEvent e) {
        final InventoryItem item = this.getTopTableWithCards().getSelectedCard();
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;

        this.setTitle("Deck Editor : " + this.customMenu.getDeckName() + " : unsaved");

        this.getTopTableWithCards().removeCard(card);
        this.getBottomTableWithCards().addCard(card);
    }

    private void removeButtonActionPerformed(final ActionEvent e) {
        final InventoryItem item = this.getBottomTableWithCards().getSelectedCard();
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;

        this.setTitle("Deck Editor : " + this.customMenu.getDeckName() + " : unsaved");

        this.getTopTableWithCards().addCard(card);
        this.getBottomTableWithCards().removeCard(card);
    }

    /**
     * Adds the cheat card.
     * 
     * @param card
     *            the card
     */
    public void addCheatCard(final CardPrinted card) {
        this.getTopTableWithCards().addCard(card);
        this.questData.getCards().getCardpool().add(card);
    }

}
