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
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import forge.Command;
import forge.Constant;
import forge.deck.Deck;
import forge.error.ErrorViewer;
import forge.gui.deckeditor.elements.CardPanelHeavy;
import forge.gui.deckeditor.elements.FilterCheckBoxes;
import forge.gui.deckeditor.elements.FilterNameTypeSetPanel;
import forge.gui.deckeditor.elements.ManaCostRenderer;
import forge.gui.deckeditor.elements.TableColumnInfo;
import forge.gui.deckeditor.elements.TableView;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.quest.QuestController;
import forge.util.Lambda0;
import forge.util.Predicate;
import net.miginfocom.swing.MigLayout;

//import forge.quest.data.QuestBoosterPack;

/**
 * <p>
 * Gui_Quest_DeckEditor class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class DeckEditorQuest extends DeckEditorBase<CardPrinted, Deck> {
    /** Constant <code>serialVersionUID=152061168634545L</code>. */
    private static final long serialVersionUID = 152061168634545L;

    /** The custom menu. */
    private final JButton clearFilterButton = new JButton();
    private final JButton addButton = new JButton();
    private final JButton removeButton = new JButton();
    private final JLabel jLabelAnalysisGap = new JLabel("");
    private final JButton analysisButton = new JButton();

    private FilterNameTypeSetPanel filterNameTypeSet;

    private final QuestController questData;
    private final DeckController<Deck> controller;

    /**
     * Show.
     * 
     * @param exitCommand
     *            the exit command
     */
    @Override
    public void show(final Command exitCommand) {
        final Command exit = new Command() {
            private static final long serialVersionUID = -7428793574300520612L;

            @Override
            public void execute() {
                DeckEditorQuest.this.dispose();
                exitCommand.execute();
            }
        };

        this.setup();

        final MenuQuest menu = new MenuQuest(this.getController(), exit);
        this.setJMenuBar(menu);

        // do not change this!!!!
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent ev) {
                menu.close();
            }
        });

        Deck deck = Constant.Runtime.HUMAN_DECK[0] == null ? null : this.questData.getMyDecks().get(
                Constant.Runtime.HUMAN_DECK[0].getName());

        if (deck == null) {
            deck = new Deck();
        }

        // tell Gui_Quest_DeckEditor the name of the deck

        this.getController().setModel(deck);

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

        setSize(1024, 740);
    }

    /**
     * Instantiates a new deck editor quest.
     * 
     * @param parent
     *            the parent frame for this deck editor instance
     * @param questData2
     *            the quest data2
     */
    public DeckEditorQuest(JFrame parent, final QuestController questData2) {
        super(parent);
        this.questData = questData2;
        try {
            this.setFilterBoxes(new FilterCheckBoxes(true));
            this.setTopTableWithCards(new TableView<CardPrinted>("All Cards", true, true, CardPrinted.class));
            this.setBottomTableWithCards(new TableView<CardPrinted>("Your deck", true, CardPrinted.class));
            this.setCardView(new CardPanelHeavy());
            this.filterNameTypeSet = new FilterNameTypeSetPanel();
            this.jbInit();
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
        }

        final Lambda0<Deck> newCreator = new Lambda0<Deck>() {
            @Override
            public Deck apply() {
                return new Deck();
            }
        };
        this.controller = new DeckController<Deck>(questData2.getMyDecks(), this, newCreator);
    }

    private void jbInit() throws Exception {

        this.setTitle("Deck Editor");
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        final Font fButtons = new java.awt.Font("Dialog", 0, 13);
        this.removeButton.setFont(fButtons);
        this.addButton.setFont(fButtons);
        this.clearFilterButton.setFont(fButtons);
        this.analysisButton.setFont(fButtons);

        this.addButton.setText("Add to Deck");
        this.removeButton.setText("Remove from Deck");
        this.clearFilterButton.setText("Clear Filter");
        this.analysisButton.setText("Deck Analysis");

        this.removeButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckEditorQuest.this.removeButtonActionPerformed(e);
            }
        });
        this.addButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckEditorQuest.this.addButtonActionPerformed(e);
            }
        });
        this.clearFilterButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckEditorQuest.this.clearFilterButtonActionPerformed(e);
            }
        });
        this.analysisButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckEditorQuest.this.analysisButtonActionPerformed(e);
            }
        });

        // Type filtering
        final Font f = new Font("Tahoma", Font.PLAIN, 10);
        for (final JCheckBox box : this.getFilterBoxes().getAllTypes()) {
            box.setFont(f);
            box.setOpaque(false);
        }

        // Color filtering
        for (final JCheckBox box : this.getFilterBoxes().getAllColors()) {
            box.setOpaque(false);
        }

        final Container content = this.getContentPane();
        final MigLayout layout = new MigLayout("fill");
        content.setLayout(layout);

        boolean isFirst = true;
        for (final JCheckBox box : this.getFilterBoxes().getAllTypes()) {
            String growParameter = "grow";
            if (isFirst) {
                growParameter = "cell 0 0, egx checkbox, grow, split 14";
                isFirst = false;
            }
            content.add(box, growParameter);
            box.addItemListener(this.getItemListenerUpdatesDisplay());
        }

        for (final JCheckBox box : this.getFilterBoxes().getAllColors()) {
            content.add(box, "grow");
            box.addItemListener(this.getItemListenerUpdatesDisplay());
        }

        content.add(this.clearFilterButton, "wmin 100, hmin 25, wmax 140, hmax 25, grow");

        content.add(this.filterNameTypeSet, "cell 0 1, grow");
        content.add(this.getTopTableWithCards().getTableDecorated(), "cell 0 2 1 2, push, grow");
        content.add(this.getTopTableWithCards().getLabel(), "cell 0 4");

        content.add(this.addButton, "w 100, h 49, sg button, cell 0 5, split 5");
        content.add(this.removeButton, "w 100, h 49, sg button");

        content.add(this.jLabelAnalysisGap, "wmin 75, growx");
        content.add(this.analysisButton, "w 100, h 49, wrap");

        content.add(this.getBottomTableWithCards().getTableDecorated(), "cell 0 6, grow");
        content.add(this.getBottomTableWithCards().getLabel(), "cell 0 7");

        content.add(this.getCardView(), "cell 1 0 1 8, flowy, growy");

        this.getTopTableWithCards().getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    DeckEditorQuest.this.addCardToDeck();
                }
            }
        });
        this.getTopTableWithCards().getTable().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    DeckEditorQuest.this.addCardToDeck();
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
    protected Predicate<CardPrinted> buildFilter() {
        final Predicate<CardPrinted> cardFilter = Predicate.and(this.getFilterBoxes().buildFilter(),
                this.filterNameTypeSet.buildFilter());
        return Predicate.instanceOf(cardFilter, CardPrinted.class);
    }

    private void addButtonActionPerformed(final ActionEvent e) {
        addCardToDeck();
    }

    /**
     * Clear filter button_action performed.
     * 
     * @param e
     *            the e
     */
    void clearFilterButtonActionPerformed(final ActionEvent e) {
        // disable automatic update triggered by listeners
        this.setFiltersChangeFiringUpdate(false);

        for (final JCheckBox box : this.getFilterBoxes().getAllTypes()) {
            if (!box.isSelected()) {
                box.doClick();
            }
        }
        for (final JCheckBox box : this.getFilterBoxes().getAllColors()) {
            if (!box.isSelected()) {
                box.doClick();
            }
        }

        this.filterNameTypeSet.clearFilters();

        this.setFiltersChangeFiringUpdate(true);

        this.getTopTableWithCards().setFilter(null);
    }

    /**
     * Adds the card to deck.
     */
    void addCardToDeck() {
        final InventoryItem item = this.getTopTableWithCards().getSelectedCard();
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;
        this.getTopTableWithCards().removeCard(card);
        this.getBottomTableWithCards().addCard(card);
        this.controller.notifyModelChanged();
    }

    private void removeButtonActionPerformed(final ActionEvent e) {
        final InventoryItem item = this.getBottomTableWithCards().getSelectedCard();
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;
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

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckEditorBase#getController()
     */
    @Override
    public DeckController<Deck> getController() {
        return this.controller;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckEditorBase#updateView()
     */
    @Override
    public void updateView() {
        final Deck deck = this.controller.getModel();

        final ItemPool<CardPrinted> cardpool = new ItemPool<CardPrinted>(CardPrinted.class);
        cardpool.addAll(this.questData.getCards().getCardpool());
        // remove bottom cards that are in the deck from the card pool
        cardpool.removeAll(deck.getMain());
        // show cards, makes this user friendly
        this.getTopTableWithCards().setDeck(cardpool);
        this.getBottomTableWithCards().setDeck(deck.getMain());
    }

}
