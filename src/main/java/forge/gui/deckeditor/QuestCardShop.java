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

import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;


import forge.Command;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.error.ErrorViewer;
import forge.gui.CardListViewer;
import forge.gui.deckeditor.elements.CardPanelLite;
import forge.gui.deckeditor.elements.FilterCheckBoxes;
import forge.gui.deckeditor.elements.FilterNameTypeSetPanel;
import forge.gui.deckeditor.elements.ManaCostRenderer;
import forge.gui.deckeditor.elements.TableColumnInfo;
import forge.gui.deckeditor.elements.TableView;
import forge.item.BoosterPack;
import forge.item.CardPrinted;
import forge.item.FatPack;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;
import forge.item.ItemPredicate;
import forge.item.OpenablePack;
import forge.item.PreconDeck;
import forge.item.TournamentPack;
import forge.quest.QuestController;
import forge.quest.io.ReadPriceList;
import forge.util.Lambda1;
import forge.util.Predicate;
import net.miginfocom.swing.MigLayout;

/**
 * <p>
 * Gui_CardShop class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class QuestCardShop extends DeckEditorBase<InventoryItem, DeckBase> {
    /** Constant <code>serialVersionUID=3988857075791576483L</code>. */
    private static final long serialVersionUID = 3988857075791576483L;

    private final JButton clearFilterButton = new JButton();
    private FilterNameTypeSetPanel filterNameTypeSet;

    private final JButton buyButton = new JButton();
    private final JButton sellButton = new JButton();

    private final JLabel creditsLabel = new JLabel();
    private final JLabel sellPercentageLabel = new JLabel();

    private double multiplier;

    private final QuestController questData;

    // get pricelist:
    private final ReadPriceList r = new ReadPriceList();
    private final Map<String, Integer> mapPrices = this.r.getPriceList();
    private Map<CardPrinted, Integer> decksUsingMyCards;

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
                QuestCardShop.this.dispose();
                exitCommand.execute();
            }
        };

        // do not change this!!!!
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent ev) {
                exit.execute();
            }
        });

        this.setup();

        this.decksUsingMyCards = this.countDecksForEachCard();

        this.multiplier = this.questData.getCards().getSellMutliplier();

        ItemPoolView<InventoryItem> forSale = this.questData.getCards().getShopList();
        if (forSale.isEmpty()) {
            this.questData.getCards().generateCardsInShop();
            forSale = this.questData.getCards().getShopList();
        }
        // newCardsList = questData.getCards().getNewCards();
        final ItemPool<InventoryItem> ownedItems = new ItemPool<InventoryItem>(InventoryItem.class);
        ownedItems.addAll(this.questData.getCards().getCardpool().getView());

        this.getTopTableModel().setDeck(forSale);
        this.getBottomTableWithCards().setDeck(ownedItems);

        this.creditsLabel.setText("Credits: " + this.questData.getAssets().getCredits());

        final double multiPercent = this.multiplier * 100;
        final NumberFormat formatter = new DecimalFormat("#0.00");
        String maxSellingPrice = "";
        final int maxSellPrice = this.questData.getCards().getSellPriceLimit();

        if (maxSellPrice < Integer.MAX_VALUE) {
            maxSellingPrice = String.format("Maximum selling price is %d credits.", maxSellPrice);
        }
        this.sellPercentageLabel.setText("<html>Selling cards at " + formatter.format(multiPercent)
                + "% of their value.<br>" + maxSellingPrice + "</html>");

        this.filterNameTypeSet.setListeners(new OnChangeTextUpdateDisplay(), this.getItemListenerUpdatesDisplay());

        this.getTopTableWithCards().sort(1, true);
        this.getBottomTableWithCards().sort(1, true);
    } // show(Command)

    // fills number of decks using each card
    private Map<CardPrinted, Integer> countDecksForEachCard() {
        final Map<CardPrinted, Integer> result = new HashMap<CardPrinted, Integer>();
        for (final Deck deck : this.questData.getMyDecks()) {
            for (final Entry<CardPrinted, Integer> e : deck.getMain()) {
                final CardPrinted card = e.getKey();
                final Integer amount = result.get(card);
                result.put(card, Integer.valueOf(amount == null ? 1 : 1 + amount.intValue()));
            }
        }
        return result;
    }

    /**
     * <p>
     * setup.
     * </p>
     */
    private void setup() {
        final List<TableColumnInfo<InventoryItem>> columns = new ArrayList<TableColumnInfo<InventoryItem>>();
        columns.add(new TableColumnInfo<InventoryItem>("Qty", 30, PresetColumns.FN_QTY_COMPARE,
                PresetColumns.FN_QTY_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Name", 176, PresetColumns.FN_NAME_COMPARE,
                PresetColumns.FN_NAME_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Cost", 70, PresetColumns.FN_COST_COMPARE,
                PresetColumns.FN_COST_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Color", 50, PresetColumns.FN_COLOR_COMPARE,
                PresetColumns.FN_COLOR_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Type", 100, PresetColumns.FN_TYPE_COMPARE,
                PresetColumns.FN_TYPE_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Stats", 40, PresetColumns.FN_STATS_COMPARE,
                PresetColumns.FN_STATS_GET));
        columns.add(new TableColumnInfo<InventoryItem>("R", 25, PresetColumns.FN_RARITY_COMPARE,
                PresetColumns.FN_RARITY_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Set", 35, PresetColumns.FN_SET_COMPARE,
                PresetColumns.FN_SET_GET));
        columns.get(2).setCellRenderer(new ManaCostRenderer());

        final List<TableColumnInfo<InventoryItem>> columnsBelow = new ArrayList<TableColumnInfo<InventoryItem>>(columns);
        columns.add(new TableColumnInfo<InventoryItem>("Price", 45, this.fnPriceCompare, this.fnPriceGet));
        this.getTopTableWithCards().setup(columns, this.getCardView());

        columnsBelow.add(new TableColumnInfo<InventoryItem>("Dks", 30, this.fnDeckCompare, this.fnDeckGet));
        columnsBelow.add(new TableColumnInfo<InventoryItem>("New", 35, this.questData.getCards().getFnNewCompare(),
                this.questData.getCards().getFnNewGet()));
        columnsBelow.add(new TableColumnInfo<InventoryItem>("Price", 45, this.fnPriceCompare, this.fnPriceSellGet));
        this.getBottomTableWithCards().setup(columnsBelow, this.getCardView());

        this.setSize(1024, 740);
    }

    /**
     * <p>
     * Constructor for Gui_CardShop.
     * </p>
     * 
     * @param parent the parent frame for this deck editor instance
     * @param qd
     *            a {@link forge.quest.data.QuestData} object.
     */
    public QuestCardShop(JFrame parent, final QuestController qd) {
        super(parent);
        this.questData = qd;
        try {
            this.setFilterBoxes(new FilterCheckBoxes(true));
            this.setTopTableWithCards(new TableView<InventoryItem>("Cards for sale", false, false, InventoryItem.class));
            this.setBottomTableWithCards(new TableView<InventoryItem>("Owned Cards", false, InventoryItem.class));
            this.setCardView(new CardPanelLite());
            this.filterNameTypeSet = new FilterNameTypeSetPanel();
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

        final Font font = new java.awt.Font("Dialog", 0, 13);
        this.clearFilterButton.setFont(font);
        this.buyButton.setFont(font);
        this.sellButton.setFont(font);
        this.creditsLabel.setFont(font);
        this.sellPercentageLabel.setFont(font);

        this.clearFilterButton.setText("Clear Filter");
        this.buyButton.setText("Buy Card");
        this.sellButton.setText("Sell Card");

        this.clearFilterButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                QuestCardShop.this.clearFilterButtonActionPerformed(e);
            }
        });
        this.buyButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                QuestCardShop.this.buyButtonActionPerformed(e);
            }
        });
        this.sellButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                QuestCardShop.this.sellButtonActionPerformed(e);
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

        this.setTitle("Card Shop");

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

        content.add(this.buyButton, "w 100, h 49, sg button, cell 0 5, split 5");
        content.add(this.sellButton, "w 100, h 49, sg button");

        content.add(this.creditsLabel, "w 100, h 49");
        content.add(this.sellPercentageLabel, "w 300, h 49, wrap");

        content.add(this.getBottomTableWithCards().getTableDecorated(), "cell 0 6, grow");

        content.add(this.getCardView(), "cell 1 0 1 7, flowy, grow");
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckEditorBase#buildFilter()
     */
    @Override
    protected Predicate<InventoryItem> buildFilter() {
        final Predicate<CardPrinted> cardFilter = Predicate.and(
                this.getFilterBoxes().buildFilter(),
                this.filterNameTypeSet.buildFilter());

        // Until this is filterable, always show packs and decks in the card shop
        Predicate<InventoryItem> filter = Predicate.instanceOf(cardFilter, CardPrinted.class);
        filter = Predicate.or(filter, ItemPredicate.Presets.IS_PACK);
        filter = Predicate.or(filter, ItemPredicate.Presets.IS_DECK);

        return filter;
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

    // TODO: move to cardshop
    private Integer getCardValue(final InventoryItem card) {
        if (this.mapPrices.containsKey(card.getName())) {
            return this.mapPrices.get(card.getName());
        } else if (card instanceof CardPrinted) {
            switch (((CardPrinted) card).getRarity()) {
            case BasicLand:
                return Integer.valueOf(4);
            case Common:
                return Integer.valueOf(6);
            case Uncommon:
                return Integer.valueOf(40);
            case Rare:
                return Integer.valueOf(120);
            case MythicRare:
                return Integer.valueOf(600);
            default:
                return Integer.valueOf(15);
            }
        } else if (card instanceof BoosterPack) {
            return 395;
        } else if (card instanceof TournamentPack) {
            return 995;
        } else if (card instanceof FatPack) {
            return 2365;
        } else if (card instanceof PreconDeck) {
            return ((PreconDeck) card).getRecommendedDeals().getCost();
        }
        return 1337;
    }

    private void buyButtonActionPerformed(final ActionEvent e) {
        final InventoryItem item = this.getTopTableWithCards().getSelectedCard();
        if (item == null) {
            return;
        }

        final int value = this.getCardValue(item);

        if (value <= this.questData.getAssets().getCredits()) {

            if (item instanceof CardPrinted) {
                this.getTopTableWithCards().removeCard(item);

                final CardPrinted card = (CardPrinted) item;
                this.getBottomTableWithCards().addCard(card);
                this.questData.getCards().buyCard(card, value);

            } else if (item instanceof OpenablePack) {
                this.getTopTableWithCards().removeCard(item);

                OpenablePack booster = null;
                if (item instanceof BoosterPack) {
                    booster = (BoosterPack) ((BoosterPack) item).clone();
                } else if (item instanceof TournamentPack) {
                    booster = (TournamentPack) ((TournamentPack) item).clone();
                } else if (item instanceof FatPack) {
                    booster = (FatPack) ((FatPack) item).clone();
                }
                this.questData.getCards().buyPack(booster, value);
                final List<CardPrinted> newCards = booster.getCards();
                for (final CardPrinted card : newCards) {
                    this.getBottomTableWithCards().addCard(card);
                }
                final CardListViewer c = new CardListViewer(booster.getName(),
                        "You have found the following cards inside:", newCards);
                c.show();
            } else if (item instanceof PreconDeck) {
                this.getTopTableWithCards().removeCard(item);
                final PreconDeck deck = (PreconDeck) item;
                this.questData.getCards().buyPreconDeck(deck, value);

                for (final CardPrinted card : deck.getDeck().getMain().toFlatList()) {
                    this.getBottomTableWithCards().addCard(card);
                }
                JOptionPane.showMessageDialog(null, String.format(
                        "Deck '%s' was added to your decklist.%n%nCards from it were also added to your pool.",
                        deck.getName()), "Thanks for purchasing!", JOptionPane.INFORMATION_MESSAGE);

            }

            this.creditsLabel.setText("Credits: " + this.questData.getAssets().getCredits());
        } else {
            JOptionPane.showMessageDialog(null, "Not enough credits!");
        }
    }

    private void sellButtonActionPerformed(final ActionEvent e) {
        final InventoryItem item = this.getBottomTableWithCards().getSelectedCard();
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;
        this.getBottomTableWithCards().removeCard(card);
        this.getTopTableWithCards().addCard(card);

        final int price = Math.min((int) (this.multiplier * this.getCardValue(card)), this.questData.getCards()
                .getSellPriceLimit());
        this.questData.getCards().sellCard(card, price);

        this.creditsLabel.setText("Credits: " + this.questData.getAssets().getCredits());
    }

    @SuppressWarnings("rawtypes")
    private final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnPriceCompare = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return QuestCardShop.this.getCardValue(from.getKey());
        }
    };
    private final Lambda1<Object, Entry<InventoryItem, Integer>> fnPriceGet = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return QuestCardShop.this.getCardValue(from.getKey());
        }
    };
    private final Lambda1<Object, Entry<InventoryItem, Integer>> fnPriceSellGet = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return (int) (QuestCardShop.this.multiplier * QuestCardShop.this.getCardValue(from.getKey()));
        }
    };

    @SuppressWarnings("rawtypes")
    private final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnDeckCompare = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            final Integer iValue = QuestCardShop.this.decksUsingMyCards.get(from.getKey());
            return iValue == null ? Integer.valueOf(0) : iValue;
        }
    };
    private final Lambda1<Object, Entry<InventoryItem, Integer>> fnDeckGet = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            final Integer iValue = QuestCardShop.this.decksUsingMyCards.get(from.getKey());
            return iValue == null ? "" : iValue.toString();
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckEditorBase#getController()
     */
    @Override
    public DeckController<DeckBase> getController() {
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

}
