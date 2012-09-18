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
package forge.gui.deckeditor.controllers;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import forge.AllZone;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.gui.CardListViewer;
import forge.gui.deckeditor.SEditorUtil;
import forge.gui.deckeditor.tables.DeckController;
import forge.gui.deckeditor.tables.SColumnUtil;
import forge.gui.deckeditor.tables.SColumnUtil.ColumnName;
import forge.gui.deckeditor.tables.TableColumnInfo;
import forge.gui.deckeditor.tables.TableView;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.home.quest.CSubmenuQuestDecks;
import forge.gui.home.quest.SSubmenuQuestUtil;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.item.BoosterPack;
import forge.item.CardPrinted;
import forge.item.FatPack;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;
import forge.item.OpenablePack;
import forge.item.PreconDeck;
import forge.item.TournamentPack;
import forge.quest.QuestController;
import forge.quest.io.ReadPriceList;
import forge.util.closures.Lambda1;

/**
 * Child controller for quest card shop UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 * 
 * @author Forge
 * @version $Id: CEditorQuestCardShop.java 15088 2012-04-07 11:34:05Z Max mtg $
 */
public final class CEditorQuestCardShop extends ACEditorBase<InventoryItem, DeckBase> {
    private final JLabel creditsLabel = new FLabel.Builder()
        .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_COINSTACK))
        .fontSize(15).build();
    private final JLabel sellPercentageLabel = new FLabel.Builder().text("0")
            .fontSize(11)
            .build();

    private double multiplier;
    private final QuestController questData;

    // get pricelist:
    private final ReadPriceList r = new ReadPriceList();
    private final Map<String, Integer> mapPrices = this.r.getPriceList();
    private Map<CardPrinted, Integer> decksUsingMyCards;

    // remember changed gui elements
    private String CCTabLabel = new String();
    private String CCAddLabel = new String();
    private String CDTabLabel = new String();
    private String CDRemLabel = new String();

    /**
     * Child controller for quest card shop UI.
     * 
     * @param qd
     *            a {@link forge.quest.data.QuestData} object.
     */
    public CEditorQuestCardShop(final QuestController qd) {
        this.questData = qd;

        final TableView<InventoryItem> tblCatalog = new TableView<InventoryItem>(false, InventoryItem.class);
        final TableView<InventoryItem> tblDeck = new TableView<InventoryItem>(false, InventoryItem.class);

        VCardCatalog.SINGLETON_INSTANCE.setTableView(tblCatalog.getTable());
        VCurrentDeck.SINGLETON_INSTANCE.setTableView(tblDeck.getTable());

        this.setTableCatalog(tblCatalog);
        this.setTableDeck(tblDeck);
    }

    /**
     * <p>
     * setup.
     * </p>
     */
    private void setup() {
        final List<TableColumnInfo<InventoryItem>> columnsCatalog = SColumnUtil.getCatalogDefaultColumns();
        final List<TableColumnInfo<InventoryItem>> columnsDeck = SColumnUtil.getDeckDefaultColumns();

        // Add "price", "decks", and "new" column in catalog and deck
        columnsCatalog.add(SColumnUtil.getColumn(ColumnName.CAT_PURCHASE_PRICE));
        columnsCatalog.get(columnsCatalog.size() - 1).setSortAndDisplayFunctions(
                this.fnPriceCompare, this.fnPriceGet);

        // card shop doesn't need "New" column
        //columnsCatalog.add(SColumnUtil.getColumn(ColumnName.CAT_NEW));
        //columnsCatalog.get(columnsCatalog.size() - 1).setSortAndDisplayFunctions(
        //        this.questData.getCards().getFnNewCompare(), this.questData.getCards().getFnNewGet());

        columnsDeck.add(SColumnUtil.getColumn(ColumnName.DECK_SALE_PRICE));
        columnsDeck.get(columnsDeck.size() - 1).setSortAndDisplayFunctions(
                this.fnPriceCompare, this.fnPriceSellGet);

        columnsDeck.add(SColumnUtil.getColumn(ColumnName.DECK_NEW));
        columnsDeck.get(columnsDeck.size() - 1).setSortAndDisplayFunctions(
                this.questData.getCards().getFnNewCompare(), this.questData.getCards().getFnNewGet());

        columnsDeck.add(SColumnUtil.getColumn(ColumnName.DECK_DECKS));
        columnsDeck.get(columnsDeck.size() - 1).setSortAndDisplayFunctions(
                this.fnDeckCompare, this.fnDeckGet);

        // don't need AI column for eaither table
        columnsCatalog.remove(SColumnUtil.getColumn(ColumnName.CAT_AI));
        columnsDeck.remove(SColumnUtil.getColumn(ColumnName.DECK_AI));

        // Setup with current column set
        this.getTableCatalog().setup(VCardCatalog.SINGLETON_INSTANCE, columnsCatalog);
        this.getTableDeck().setup(VCurrentDeck.SINGLETON_INSTANCE, columnsDeck);

        SEditorUtil.resetUI();

        CCTabLabel = VCardCatalog.SINGLETON_INSTANCE.getTabLabel().getText();
        VCardCatalog.SINGLETON_INSTANCE.getTabLabel().setText("Cards for sale");

        CCAddLabel = VCardCatalog.SINGLETON_INSTANCE.getBtnAdd().getText();
        VCardCatalog.SINGLETON_INSTANCE.getBtnAdd().setText("Buy Card");

        CDTabLabel = VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().getText();
        VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText("Your Cards");

        CDRemLabel = VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove().getText();
        VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove().setText("Sell Card");

        VCurrentDeck.SINGLETON_INSTANCE.getPnlHeader().setVisible(false);
    }

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

    private Integer getCardValue(final InventoryItem card) {
        String ns = null;
        if (card instanceof CardPrinted) {
            ns = card.getName() + "|" + ((CardPrinted) card).getEdition();
        } else {
            ns = card.getName();
        }

        if (this.mapPrices.containsKey(ns)) {
            return this.mapPrices.get(ns);
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

    @SuppressWarnings("rawtypes")
    private final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnPriceCompare = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return CEditorQuestCardShop.this.getCardValue(from.getKey());
        }
    };
    private final Lambda1<Object, Entry<InventoryItem, Integer>> fnPriceGet = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return CEditorQuestCardShop.this.getCardValue(from.getKey());
        }
    };
    private final Lambda1<Object, Entry<InventoryItem, Integer>> fnPriceSellGet = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return (int) (CEditorQuestCardShop.this.multiplier * CEditorQuestCardShop.this.getCardValue(from.getKey()));
        }
    };

    @SuppressWarnings("rawtypes")
    private final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnDeckCompare = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            final Integer iValue = CEditorQuestCardShop.this.decksUsingMyCards.get(from.getKey());
            return iValue == null ? Integer.valueOf(0) : iValue;
        }
    };
    private final Lambda1<Object, Entry<InventoryItem, Integer>> fnDeckGet = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            final Integer iValue = CEditorQuestCardShop.this.decksUsingMyCards.get(from.getKey());
            return iValue == null ? "" : iValue.toString();
        }
    };

    //=========== Overridden from ACEditorBase

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#addCard()
     */
    @Override
    public void addCard() {
        final InventoryItem item = this.getTableCatalog().getSelectedCard();
        if (item == null) {
            return;
        }

        final int value = this.getCardValue(item);

        if (value <= this.questData.getAssets().getCredits()) {

            if (item instanceof CardPrinted) {
                this.getTableCatalog().removeCard(item);

                final CardPrinted card = (CardPrinted) item;
                this.getTableDeck().addCard(card);
                this.questData.getCards().buyCard(card, value);

            } else if (item instanceof OpenablePack) {
                this.getTableCatalog().removeCard(item);

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
                    this.getTableDeck().addCard(card);
                }
                final CardListViewer c = new CardListViewer(booster.getName(),
                        "You have found the following cards inside:", newCards);
                c.show();
            } else if (item instanceof PreconDeck) {
                this.getTableCatalog().removeCard(item);
                final PreconDeck deck = (PreconDeck) item;
                this.questData.getCards().buyPreconDeck(deck, value);

                for (final CardPrinted card : deck.getDeck().getMain().toFlatList()) {
                    this.getTableDeck().addCard(card);
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

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#removeCard()
     */
    @Override
    public void removeCard() {
        final InventoryItem item = this.getTableDeck().getSelectedCard();
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;
        this.getTableDeck().removeCard(card);
        this.getTableCatalog().addCard(card);

        final int price = Math.min((int) (this.multiplier * this.getCardValue(card)), this.questData.getCards()
                .getSellPriceLimit());
        this.questData.getCards().sellCard(card, price);

        this.creditsLabel.setText("Credits: " + this.questData.getAssets().getCredits());
    }


    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.ACEditorBase#resetTables()
     */
    @Override
    public void resetTables() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.ACEditorBase#getController()
     */
    @Override
    public DeckController<DeckBase> getDeckController() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#show(forge.Command)
     */
    @Override
    public void init() {
        this.setup();

        this.decksUsingMyCards = this.countDecksForEachCard();

        this.multiplier = this.questData.getCards().getSellMultiplier();

        ItemPoolView<InventoryItem> forSale = this.questData.getCards().getShopList();
        if (forSale.isEmpty()) {
            this.questData.getCards().generateCardsInShop();
            forSale = this.questData.getCards().getShopList();
        }

        // newCardsList = questData.getCards().getNewCards();
        final ItemPool<InventoryItem> ownedItems = new ItemPool<InventoryItem>(InventoryItem.class);
        ownedItems.addAll(this.questData.getCards().getCardpool().getView());

        this.getTableCatalog().setDeck(forSale);
        this.getTableDeck().setDeck(ownedItems);

        VCurrentDeck.SINGLETON_INSTANCE.getPnlRemButtons().remove(VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4());
        VCurrentDeck.SINGLETON_INSTANCE.getPnlRemButtons().add(creditsLabel);
        this.creditsLabel.setText("Credits: " + this.questData.getAssets().getCredits());

        final double multiPercent = this.multiplier * 100;
        final NumberFormat formatter = new DecimalFormat("#0.00");
        String maxSellingPrice = "";
        final int maxSellPrice = this.questData.getCards().getSellPriceLimit();

        if (maxSellPrice < Integer.MAX_VALUE) {
            maxSellingPrice = String.format("Maximum selling price is %d credits.", maxSellPrice);
        }
        VCardCatalog.SINGLETON_INSTANCE.getPnlAddButtons().remove(VCardCatalog.SINGLETON_INSTANCE.getBtnAdd4());
        VCardCatalog.SINGLETON_INSTANCE.getPnlAddButtons().add(sellPercentageLabel);
        this.sellPercentageLabel.setText("<html>Selling cards at " + formatter.format(multiPercent)
                + "% of their value.<br>" + maxSellingPrice + "</html>");
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#exit()
     */
    @Override
    public boolean exit() {
        SSubmenuQuestUtil.updateStatsAndPet();
        AllZone.getQuest().save();
        CSubmenuQuestDecks.SINGLETON_INSTANCE.update();

        // undo Card Shop Specifics
        VCardCatalog.SINGLETON_INSTANCE.getPnlAddButtons().remove(sellPercentageLabel);
        VCardCatalog.SINGLETON_INSTANCE.getPnlAddButtons().add(VCardCatalog.SINGLETON_INSTANCE.getBtnAdd4());

        VCurrentDeck.SINGLETON_INSTANCE.getPnlRemButtons().remove(creditsLabel);
        VCurrentDeck.SINGLETON_INSTANCE.getPnlRemButtons().add(VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4());

        VCardCatalog.SINGLETON_INSTANCE.getTabLabel().setText(CCTabLabel);
        VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText(CDTabLabel);

        VCardCatalog.SINGLETON_INSTANCE.getBtnAdd().setText(CCAddLabel);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove().setText(CDRemLabel);

        return true;
    }
}
