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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;

import forge.Command;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.deck.DeckSection;
import forge.gui.CardListViewer;
import forge.gui.deckeditor.SEditorUtil;
import forge.gui.deckeditor.tables.DeckController;
import forge.gui.deckeditor.tables.EditorTableView;
import forge.gui.deckeditor.tables.SColumnUtil;
import forge.gui.deckeditor.tables.SColumnUtil.ColumnName;
import forge.gui.deckeditor.tables.TableColumnInfo;
import forge.gui.deckeditor.views.VAllDecks;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.deckeditor.views.VDeckgen;
import forge.gui.framework.DragCell;
import forge.gui.home.quest.CSubmenuQuestDecks;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.item.BoosterPack;
import forge.item.CardDb;
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
    
    // TODO: move these to the view where they belong
    private final JLabel sellPercentageLabel = new FLabel.Builder().text("0")
            .fontSize(11)
            .build();
    @SuppressWarnings("serial")
    private final JLabel fullCatalogToggle = new FLabel.Builder().text("See full catalog")
            .fontSize(14).hoverable(true).cmdClick(new Command() {
                @Override
                public void execute() {
                    toggleFullCatalog();
                }
            })
            .build();

    private double multiplier;
    private final QuestController questData;
    
    private ItemPoolView<InventoryItem> cardsForSale;
    private final ItemPool<InventoryItem> fullCatalogCards =
            ItemPool.createFrom(CardDb.instance().getAllTraditionalCards(), InventoryItem.class);
    private boolean showingFullCatalog = false;
    private DragCell allDecksParent = null;
    private DragCell deckGenParent = null;

    // get pricelist:
    private final ReadPriceList r = new ReadPriceList();
    private final Map<String, Integer> mapPrices = this.r.getPriceList();
    private ItemPool<InventoryItem> decksUsingMyCards;

    // remember changed gui elements
    private String CCTabLabel = new String();
    private String CCAddLabel = new String();
    private String CDTabLabel = new String();
    private String CDRemLabel = new String();
    private String prevRem4Label = null;
    private String prevRem4Tooltip = null;
    private Command prevRem4Cmd = null;

    /**
     * Child controller for quest card shop UI.
     * 
     * @param qd
     *            a {@link forge.quest.data.QuestData} object.
     */
    public CEditorQuestCardShop(final QuestController qd) {
        this.questData = qd;

        final EditorTableView<InventoryItem> tblCatalog = new EditorTableView<InventoryItem>(false, InventoryItem.class);
        final EditorTableView<InventoryItem> tblDeck = new EditorTableView<InventoryItem>(false, InventoryItem.class);

        tblCatalog.setAlwaysNonUnique(true);
        tblDeck.setAlwaysNonUnique(true);

        VCardCatalog.SINGLETON_INSTANCE.setTableView(tblCatalog.getTable());
        VCurrentDeck.SINGLETON_INSTANCE.setTableView(tblDeck.getTable());

        this.setTableCatalog(tblCatalog);
        this.setTableDeck(tblDeck);
    }

    private void toggleFullCatalog() {
        showingFullCatalog = !showingFullCatalog;
        
        if (showingFullCatalog) {
            this.getTableCatalog().setDeck(fullCatalogCards, true);
            VCardCatalog.SINGLETON_INSTANCE.getBtnAdd().setEnabled(false);
            VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove().setEnabled(false);
            VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4().setEnabled(false);
            fullCatalogToggle.setText("Return to spell shop");
        } else {
            this.getTableCatalog().setDeck(cardsForSale);
            VCardCatalog.SINGLETON_INSTANCE.getBtnAdd().setEnabled(true);
            VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove().setEnabled(true);
            VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4().setEnabled(true);
            fullCatalogToggle.setText("See full catalog");
        }
    }
    
    private void setup() {
        final List<TableColumnInfo<InventoryItem>> columnsCatalog = SColumnUtil.getCatalogDefaultColumns();
        final List<TableColumnInfo<InventoryItem>> columnsDeck = SColumnUtil.getDeckDefaultColumns();

        // Add spell shop-specific columns
        columnsCatalog.add(SColumnUtil.getColumn(ColumnName.CAT_PURCHASE_PRICE));
        columnsCatalog.get(columnsCatalog.size() - 1).setSortAndDisplayFunctions(
                this.fnPriceCompare, this.fnPriceGet);

        columnsCatalog.add(1, SColumnUtil.getColumn(ColumnName.CAT_OWNED));
        columnsCatalog.get(1).setSortAndDisplayFunctions(
                questData.getCards().getFnOwnedCompare(), questData.getCards().getFnOwnedGet());

        columnsDeck.add(SColumnUtil.getColumn(ColumnName.DECK_SALE_PRICE));
        columnsDeck.get(columnsDeck.size() - 1).setSortAndDisplayFunctions(
                this.fnPriceCompare, this.fnPriceSellGet);

        columnsDeck.add(SColumnUtil.getColumn(ColumnName.DECK_NEW));
        columnsDeck.get(columnsDeck.size() - 1).setSortAndDisplayFunctions(
                this.questData.getCards().getFnNewCompare(), this.questData.getCards().getFnNewGet());

        columnsDeck.add(SColumnUtil.getColumn(ColumnName.DECK_DECKS));
        columnsDeck.get(columnsDeck.size() - 1).setSortAndDisplayFunctions(
                this.fnDeckCompare, this.fnDeckGet);

        // don't need AI column for either table
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

        prevRem4Label = VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4().getText();
        prevRem4Tooltip = VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4().getToolTipText();
        prevRem4Cmd = VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4().getCommand();
        
        VCurrentDeck.SINGLETON_INSTANCE.getPnlHeader().setVisible(false);
    }

    // fills number of decks using each card
    private ItemPool<InventoryItem> countDecksForEachCard() {
        final ItemPool<InventoryItem> result = new ItemPool<InventoryItem>(InventoryItem.class);
        for (final Deck deck : this.questData.getMyDecks()) {
            for (final Entry<CardPrinted, Integer> e : deck.getMain()) {
                result.add(e.getKey());
            }
            if ( deck.has(DeckSection.Sideboard))
                for (final Entry<CardPrinted, Integer> e : deck.get(DeckSection.Sideboard)) {
                    result.add(e.getKey());
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

    private final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnPriceCompare = new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
        @Override
        public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
            return CEditorQuestCardShop.this.getCardValue(from.getKey());
        }
    };
    private final Function<Entry<InventoryItem, Integer>, Object> fnPriceGet = new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return CEditorQuestCardShop.this.getCardValue(from.getKey());
        }
    };
    private final Function<Entry<InventoryItem, Integer>, Object> fnPriceSellGet = new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return (int) (CEditorQuestCardShop.this.multiplier * CEditorQuestCardShop.this.getCardValue(from.getKey()));
        }
    };

    private final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnDeckCompare = new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
        @Override
        public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
            final Integer iValue = CEditorQuestCardShop.this.decksUsingMyCards.count(from.getKey());
            return iValue == null ? Integer.valueOf(0) : iValue;
        }
    };
    private final Function<Entry<InventoryItem, Integer>, Object> fnDeckGet = new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            final Integer iValue = CEditorQuestCardShop.this.decksUsingMyCards.count(from.getKey());
            return iValue == null ? "" : iValue.toString();
        }
    };

    //=========== Overridden from ACEditorBase

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#addCard()
     */
    @Override
    public void addCard(InventoryItem item, boolean toAlternate, int qty) {
        // disallow "buying" cards while showing the full catalog
        if (item == null || showingFullCatalog || toAlternate) {
            return;
        }

        final int value = this.getCardValue(item);

        if (value > this.questData.getAssets().getCredits()) {
            JOptionPane.showMessageDialog(null, "Not enough credits!");
            return;
        }
        
        if (item instanceof CardPrinted) {
            final CardPrinted card = (CardPrinted) item;
            this.getTableDeck().addCard(card, qty);
            this.getTableCatalog().removeCard(item, qty);
            this.questData.getCards().buyCard(card, value);

        } else if (item instanceof OpenablePack) {
            for (int i = 0; qty > i; ++i) {
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
                final List<InventoryItem> newInventory = new LinkedList<InventoryItem>(newCards);
                getTableDeck().addCards(newInventory);
                final CardListViewer c = new CardListViewer(booster.getName(),
                        "You have found the following cards inside:", newCards);
                c.show();
            }
            this.getTableCatalog().removeCard(item, qty);
        } else if (item instanceof PreconDeck) {
            final PreconDeck deck = (PreconDeck) item;
            this.questData.getCards().buyPreconDeck(deck, value);
            final ItemPool<InventoryItem> newInventory =
                    ItemPool.createFrom(deck.getDeck().getMain(), InventoryItem.class);
            for (int i = 0; qty > i; ++i) {
                getTableDeck().addCards(newInventory);
            }
            boolean one = 1 == qty;
            JOptionPane.showMessageDialog(null, String.format(
                    "%s '%s' %s added to your decklist.%n%n%s cards were also added to your pool.",
                    one ? "Deck" : String.format("%d copies of deck", qty),
                    deck.getName(), one ? "was" : "were", one ? "Its" : "Their"),
                    "Thanks for purchasing!", JOptionPane.INFORMATION_MESSAGE);
            this.getTableCatalog().removeCard(item, qty);
        }

        this.creditsLabel.setText("Credits: " + this.questData.getAssets().getCredits());
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#removeCard()
     */
    @Override
    public void removeCard(InventoryItem item, boolean toAlternate, int qty) {
        if ((item == null) || !(item instanceof CardPrinted) || showingFullCatalog || toAlternate) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;
        this.getTableCatalog().addCard(card, qty);
        this.getTableDeck().removeCard(card, qty);

        final int price = Math.min((int) (this.multiplier * this.getCardValue(card)), this.questData.getCards()
                .getSellPriceLimit());
        this.questData.getCards().sellCard(card, 1, price);

        this.creditsLabel.setText("Credits: " + this.questData.getAssets().getCredits());
    }
    
    @Override
    public void buildAddContextMenu(ContextMenuBuilder cmb) {
        if (!showingFullCatalog) {
            cmb.addMoveItems("Buy", "item", "items", null);
        }
        cmb.addTextFilterItem();
    }
    
    @Override
    public void buildRemoveContextMenu(ContextMenuBuilder cmb) {
        if (!showingFullCatalog) {
            cmb.addMoveItems("Sell", "card", "cards", null);
        }
    }

    public void removeCards(List<Map.Entry<InventoryItem, Integer>> cardsToRemove) {
        if (showingFullCatalog) {
            return;
        }
        
        this.getTableCatalog().addCards(cardsToRemove);
        this.getTableDeck().removeCards(cardsToRemove);

        for (Map.Entry<InventoryItem, Integer> item : cardsToRemove) {
            if (!(item.getKey() instanceof CardPrinted)) {
                continue;
            }
            CardPrinted card = (CardPrinted)item.getKey();
            final int price = Math.min((int) (this.multiplier * this.getCardValue(card)),
                    this.questData.getCards().getSellPriceLimit());
            this.questData.getCards().sellCard(card, item.getValue(), price);
        }

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
    @SuppressWarnings("serial")
    @Override
    public void init() {
        setup();

        this.decksUsingMyCards = this.countDecksForEachCard();
        this.multiplier = this.questData.getCards().getSellMultiplier();
        this.cardsForSale = this.questData.getCards().getShopList();

        final ItemPool<InventoryItem> ownedItems = new ItemPool<InventoryItem>(InventoryItem.class);
        ownedItems.addAll(this.questData.getCards().getCardpool().getView());

        this.getTableCatalog().setDeck(cardsForSale);
        this.getTableDeck().setDeck(ownedItems);

        VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4().setText("Sell all extras");
        VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4().setToolTipText("Sell unneeded extra copies of all cards");
        VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4().setCommand(new Command() {
            @Override
            public void execute() {
                List<Map.Entry<InventoryItem, Integer>> cardsToRemove = new LinkedList<Map.Entry<InventoryItem,Integer>>();
                for (Map.Entry<InventoryItem, Integer> item : getTableDeck().getCards()) {
                    CardPrinted card = (CardPrinted)item.getKey();
                    int numToKeep = card.getCard().getType().isBasic() ? 50 : 4;
                    if ("Relentless Rats".equals(card.getName())) {
                        numToKeep = Integer.MAX_VALUE;
                    }
                    if (numToKeep < item.getValue()) {
                        cardsToRemove.add(Pair.of(item.getKey(), item.getValue() - numToKeep));
                    }
                }
                removeCards(cardsToRemove);
            }
        });
        
        VCurrentDeck.SINGLETON_INSTANCE.getPnlRemButtons().add(creditsLabel, "gap 5px");
        this.creditsLabel.setText("Credits: " + this.questData.getAssets().getCredits());

        final double multiPercent = this.multiplier * 100;
        final NumberFormat formatter = new DecimalFormat("#0.00");
        String maxSellingPrice = "";
        final int maxSellPrice = this.questData.getCards().getSellPriceLimit();

        if (maxSellPrice < Integer.MAX_VALUE) {
            maxSellingPrice = String.format("Maximum selling price is %d credits.", maxSellPrice);
        }
        VCardCatalog.SINGLETON_INSTANCE.getPnlAddButtons().remove(VCardCatalog.SINGLETON_INSTANCE.getBtnAdd4());
        VCardCatalog.SINGLETON_INSTANCE.getPnlAddButtons().add(fullCatalogToggle, "w 25%, h 30!", 0);
        VCardCatalog.SINGLETON_INSTANCE.getPnlAddButtons().add(sellPercentageLabel);
        this.sellPercentageLabel.setText("<html>Selling cards at " + formatter.format(multiPercent)
                + "% of their value.<br>" + maxSellingPrice + "</html>");
        
        VCardCatalog.SINGLETON_INSTANCE.getStatLabel(SEditorUtil.StatTypes.PACK).setVisible(true);
        
        deckGenParent = removeTab(VDeckgen.SINGLETON_INSTANCE);
        allDecksParent = removeTab(VAllDecks.SINGLETON_INSTANCE);        
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#exit()
     */
    @Override
    public boolean exit() {
        if (showingFullCatalog) {
            toggleFullCatalog();
        }
        
        Singletons.getModel().getQuest().save();
        CSubmenuQuestDecks.SINGLETON_INSTANCE.update();

        // undo Card Shop Specifics
        VCardCatalog.SINGLETON_INSTANCE.getPnlAddButtons().remove(sellPercentageLabel);
        VCardCatalog.SINGLETON_INSTANCE.getPnlAddButtons().remove(fullCatalogToggle);
        VCardCatalog.SINGLETON_INSTANCE.getPnlAddButtons().add(VCardCatalog.SINGLETON_INSTANCE.getBtnAdd4());

        VCurrentDeck.SINGLETON_INSTANCE.getPnlRemButtons().remove(creditsLabel);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4().setText(prevRem4Label);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4().setToolTipText(prevRem4Tooltip);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4().setCommand(prevRem4Cmd);

        VCardCatalog.SINGLETON_INSTANCE.getTabLabel().setText(CCTabLabel);
        VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText(CDTabLabel);

        VCardCatalog.SINGLETON_INSTANCE.getBtnAdd().setText(CCAddLabel);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove().setText(CDRemLabel);
        
        VCardCatalog.SINGLETON_INSTANCE.getStatLabel(SEditorUtil.StatTypes.PACK).setVisible(false);
        
        //Re-add tabs
        if (deckGenParent != null) {
            deckGenParent.addDoc(VDeckgen.SINGLETON_INSTANCE);
        }
        if (allDecksParent != null) {
            allDecksParent.addDoc(VAllDecks.SINGLETON_INSTANCE);
        }
        
        return true;
    }
}
