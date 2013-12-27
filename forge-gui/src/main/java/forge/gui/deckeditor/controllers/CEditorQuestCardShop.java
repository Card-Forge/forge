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
import java.util.ArrayList;
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
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.deck.DeckSection;
import forge.gui.CardListViewer;
import forge.gui.deckeditor.views.VAllDecks;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.deckeditor.views.VDeckgen;
import forge.gui.deckeditor.views.VProbabilities;
import forge.gui.framework.DragCell;
import forge.gui.framework.FScreen;
import forge.gui.home.quest.CSubmenuQuestDecks;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FOptionPane;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.itemmanager.SpellShopManager;
import forge.gui.toolbox.itemmanager.SItemManagerUtil;
import forge.gui.toolbox.itemmanager.views.ItemCellRenderer;
import forge.gui.toolbox.itemmanager.views.SColumnUtil;
import forge.gui.toolbox.itemmanager.views.TableColumnInfo;
import forge.gui.toolbox.itemmanager.views.SColumnUtil.ColumnName;
import forge.item.BoosterPack;
import forge.item.PaperCard;
import forge.item.FatPack;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.item.SealedProduct;
import forge.item.PreconDeck;
import forge.item.TournamentPack;
import forge.quest.QuestController;
import forge.quest.io.ReadPriceList;
import forge.util.ItemPool;
import forge.util.ItemPoolView;

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
                public void run() {
                    toggleFullCatalog();
                }
            })
            .build();

    private double multiplier;
    private final QuestController questData;
    
    private ItemPoolView<InventoryItem> cardsForSale;
    private final ItemPool<InventoryItem> fullCatalogCards =
            ItemPool.createFrom(Singletons.getMagicDb().getCommonCards().getAllCards(), InventoryItem.class);
    private boolean showingFullCatalog = false;
    private DragCell allDecksParent = null;
    private DragCell deckGenParent = null;
    private DragCell probsParent = null;

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
    private Runnable prevRem4Cmd = null;

    /**
     * Child controller for quest card shop UI.
     * 
     * @param qd
     *            a {@link forge.quest.data.QuestData} object.
     */
    public CEditorQuestCardShop(final QuestController qd) {
        super(FScreen.QUEST_CARD_SHOP);
        
        this.questData = qd;

        final SpellShopManager catalogManager = new SpellShopManager(false);
        final SpellShopManager deckManager = new SpellShopManager(false);

        catalogManager.setAlwaysNonUnique(true);
        deckManager.setAlwaysNonUnique(true);

        this.setCatalogManager(catalogManager);
        this.setDeckManager(deckManager);
    }

    private void toggleFullCatalog() {
        showingFullCatalog = !showingFullCatalog;
        
        if (showingFullCatalog) {
            this.getCatalogManager().setPool(fullCatalogCards, true);
            this.getBtnAdd().setEnabled(false);
            this.getBtnRemove().setEnabled(false);
            this.getBtnRemove4().setEnabled(false);
            fullCatalogToggle.setText("Return to spell shop");
        } else {
            this.getCatalogManager().setPool(cardsForSale);
            this.getBtnAdd().setEnabled(true);
            this.getBtnRemove().setEnabled(true);
            this.getBtnRemove4().setEnabled(true);
            fullCatalogToggle.setText("See full catalog");
        }
    }

    // fills number of decks using each card
    private ItemPool<InventoryItem> countDecksForEachCard() {
        final ItemPool<InventoryItem> result = new ItemPool<InventoryItem>(InventoryItem.class);
        for (final Deck deck : this.questData.getMyDecks()) {
            CardPool main = deck.getMain();
            for (final Entry<PaperCard, Integer> e : main) {
                result.add(e.getKey());
            }
            if (deck.has(DeckSection.Sideboard)) {
                for (final Entry<PaperCard, Integer> e : deck.get(DeckSection.Sideboard)) {
                    // only add card if we haven't already encountered it in main
                    if (!main.contains(e.getKey())) {
                        result.add(e.getKey());
                    }
                }
            }
        }
        return result;
    }

    private Integer getCardValue(final InventoryItem card) {
        String ns = null;
        int value = 1337; // previously this was the returned default
        boolean foil = false;
        int foilMultiplier = 1;

        if (card instanceof PaperCard) {
            ns = card.getName() + "|" + ((PaperCard) card).getEdition();
            foil = ((PaperCard) card).isFoil();
        } else {
            ns = card.getName();
        }

        if (this.mapPrices.containsKey(ns)) {
            value = this.mapPrices.get(ns);
        } else if (card instanceof PaperCard) {
            switch (((IPaperCard) card).getRarity()) {
                case BasicLand:
                    value = 4;
                    break;
                case Common:
                    value = 6;
                    break;
                case Uncommon:
                    value = 40;
                    break;
                case Rare:
                    value = 120;
                    break;
                case MythicRare:
                    value = 600;
                    break;
                default:
                    value = 15;
                    break;
            }
        } else if (card instanceof BoosterPack) {
            value = 395;
        } else if (card instanceof TournamentPack) {
            value = 995;
        } else if (card instanceof FatPack) {
            value = 2365;
        } else if (card instanceof PreconDeck) {
            value = QuestController.getPreconDeals((PreconDeck) card).getCost();
        }

        // TODO: make this changeable via a user-definable property?
        if (foil) {
            switch (((IPaperCard) card).getRarity()) {
                case BasicLand:
                    foilMultiplier = 2;
                    break;
                case Common:
                    foilMultiplier = 2;
                    break;
                case Uncommon:
                    foilMultiplier = 2;
                    break;
                case Rare:
                    foilMultiplier = 3;
                    break;
                case MythicRare:
                    foilMultiplier = 3;
                    break;
                default:
                    foilMultiplier = 2;
                    break;
            }
            value *= foilMultiplier;
        }
        
        return Integer.valueOf(value);
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
     * @see forge.gui.deckeditor.ACEditorBase#onAddItems()
     */
    @Override
    protected void onAddItems(Iterable<Entry<InventoryItem, Integer>> items, boolean toAlternate) {
        // disallow "buying" cards while showing the full catalog
        if (showingFullCatalog || toAlternate) {
            return;
        }

        ItemPool<InventoryItem> itemsToAdd = new ItemPool<InventoryItem>(InventoryItem.class);
        List<Entry<InventoryItem, Integer>> itemsToRemove = new ArrayList<Entry<InventoryItem, Integer>>();

        for (Entry<InventoryItem, Integer> itemEntry : items) {
            final InventoryItem item = itemEntry.getKey();
            final int qty = itemEntry.getValue();
            final int value = this.getCardValue(item);

            if (value > this.questData.getAssets().getCredits()) {
                FOptionPane.showMessageDialog("Not enough credits to purchase " + (qty == 1 ? "" : qty + " copies of ") + item.getName() + ".");
                continue;
            }

            if (item instanceof PaperCard) {
                this.questData.getCards().buyCard((PaperCard) item, qty, value);
                itemsToAdd.add(item, qty);
            }
            else if (item instanceof SealedProduct) {
                for (int i = 0; i < qty; i++) {
                    SealedProduct booster = null;
                    if (item instanceof BoosterPack) {
                        booster = (BoosterPack) ((BoosterPack) item).clone();
                    }
                    else if (item instanceof TournamentPack) {
                        booster = (TournamentPack) ((TournamentPack) item).clone();
                    }
                    else if (item instanceof FatPack) {
                        booster = (FatPack) ((FatPack) item).clone();
                    }
                    this.questData.getCards().buyPack(booster, value);
                    final List<PaperCard> newCards = booster.getCards();

                    itemsToAdd.addAllFlat(newCards);

                    final CardListViewer c = new CardListViewer(booster.getName(), "You have found the following cards inside:", newCards);
                    c.setVisible(true);
                    c.dispose();
                }
            }
            else if (item instanceof PreconDeck) {
                final PreconDeck deck = (PreconDeck) item;
                for (int i = 0; i < qty; i++) {
                    this.questData.getCards().buyPreconDeck(deck, value);
    
                    itemsToAdd.addAll(deck.getDeck().getMain());
                }

                boolean one = (qty == 1);
                JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), String.format(
                        "%s '%s' %s added to your decklist.%n%n%s cards were also added to your pool.",
                        one ? "Deck" : String.format("%d copies of deck", qty),
                        deck.getName(), one ? "was" : "were", one ? "Its" : "Their"),
                        "Thanks for purchasing!", JOptionPane.INFORMATION_MESSAGE);
            }
            else {
                continue; //don't remove item from Catalog if any other type
            }
            itemsToRemove.add(itemEntry);
        }

        if (itemsToRemove.isEmpty()) { return; }

        this.getDeckManager().addItems(itemsToAdd);
        this.getCatalogManager().removeItems(itemsToRemove);

        this.creditsLabel.setText("Credits: " + this.questData.getAssets().getCredits());
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onRemoveItems()
     */
    @Override
    protected void onRemoveItems(Iterable<Entry<InventoryItem, Integer>> items, boolean toAlternate) {
        if (showingFullCatalog || toAlternate) { return; }

        this.getCatalogManager().addItems(items);
        this.getDeckManager().removeItems(items);

        for (Entry<InventoryItem, Integer> itemEntry : items) {
            final InventoryItem item = itemEntry.getKey();
            if (item instanceof PaperCard) {
                final PaperCard card = (PaperCard) item;
                final int qty = itemEntry.getValue();
                final int price = Math.min((int) (this.multiplier * this.getCardValue(card)), this.questData.getCards().getSellPriceLimit());
                this.questData.getCards().sellCard(card, qty, price);
            }
        }

        this.creditsLabel.setText("Credits: " + this.questData.getAssets().getCredits());
    }
    
    @Override
    public void buildAddContextMenu(ContextMenuBuilder cmb) {
        if (!showingFullCatalog) {
            cmb.addMoveItems("Buy", "item", "items", null);
        }
    }
    
    @Override
    public void buildRemoveContextMenu(ContextMenuBuilder cmb) {
        if (!showingFullCatalog) {
            cmb.addMoveItems("Sell", "card", "cards", null);
        }
    }

    public void removeCards(List<Entry<InventoryItem, Integer>> cardsToRemove) {
        if (showingFullCatalog) {
            return;
        }
        
        this.getCatalogManager().addItems(cardsToRemove);
        this.getDeckManager().removeItems(cardsToRemove);

        for (Entry<InventoryItem, Integer> item : cardsToRemove) {
            if (!(item.getKey() instanceof PaperCard)) {
                continue;
            }
            PaperCard card = (PaperCard)item.getKey();
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
    public void update() {
        final List<TableColumnInfo<InventoryItem>> columnsCatalog = SColumnUtil.getCatalogDefaultColumns();
        final List<TableColumnInfo<InventoryItem>> columnsDeck = SColumnUtil.getDeckDefaultColumns();

        // Add spell shop-specific columns
        columnsCatalog.add(SColumnUtil.getColumn(ColumnName.CAT_PURCHASE_PRICE));
        columnsCatalog.get(columnsCatalog.size() - 1).setSortAndDisplayFunctions(
                this.fnPriceCompare, this.fnPriceGet, new ItemCellRenderer());

        columnsCatalog.add(1, SColumnUtil.getColumn(ColumnName.CAT_OWNED));
        columnsCatalog.get(1).setSortAndDisplayFunctions(
                questData.getCards().getFnOwnedCompare(), questData.getCards().getFnOwnedGet(), new ItemCellRenderer());

        columnsDeck.add(SColumnUtil.getColumn(ColumnName.DECK_SALE_PRICE));
        columnsDeck.get(columnsDeck.size() - 1).setSortAndDisplayFunctions(
                this.fnPriceCompare, this.fnPriceSellGet, new ItemCellRenderer());

        columnsDeck.add(SColumnUtil.getColumn(ColumnName.DECK_NEW));
        columnsDeck.get(columnsDeck.size() - 1).setSortAndDisplayFunctions(
                this.questData.getCards().getFnNewCompare(), this.questData.getCards().getFnNewGet(), new ItemCellRenderer());

        columnsDeck.add(SColumnUtil.getColumn(ColumnName.DECK_DECKS));
        columnsDeck.get(columnsDeck.size() - 1).setSortAndDisplayFunctions(
                this.fnDeckCompare, this.fnDeckGet, new ItemCellRenderer());

        // don't need AI column for either table
        columnsCatalog.remove(SColumnUtil.getColumn(ColumnName.CAT_AI));
        columnsDeck.remove(SColumnUtil.getColumn(ColumnName.DECK_AI));

        // Setup with current column set
        this.getCatalogManager().getTable().setup(columnsCatalog);
        this.getDeckManager().getTable().setup(columnsDeck);

        SItemManagerUtil.resetUI(this);

        CCTabLabel = VCardCatalog.SINGLETON_INSTANCE.getTabLabel().getText();
        VCardCatalog.SINGLETON_INSTANCE.getTabLabel().setText("Cards for sale");

        CCAddLabel = this.getBtnAdd().getText();
        this.getBtnAdd().setText("Buy Card");

        CDTabLabel = VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().getText();
        VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText("Your Cards");

        CDRemLabel = this.getBtnRemove().getText();
        this.getBtnRemove().setText("Sell Card");
        
        VProbabilities.SINGLETON_INSTANCE.getTabLabel().setVisible(false);

        prevRem4Label = this.getBtnRemove4().getText();
        prevRem4Tooltip = this.getBtnRemove4().getToolTipText();
        prevRem4Cmd = this.getBtnRemove4().getCommand();
        
        VCurrentDeck.SINGLETON_INSTANCE.getPnlHeader().setVisible(false);

        this.decksUsingMyCards = this.countDecksForEachCard();
        this.multiplier = this.questData.getCards().getSellMultiplier();
        this.cardsForSale = this.questData.getCards().getShopList();

        final ItemPool<InventoryItem> ownedItems = new ItemPool<InventoryItem>(InventoryItem.class);
        ownedItems.addAll(this.questData.getCards().getCardpool().getView());

        this.getCatalogManager().setPool(cardsForSale);
        this.getDeckManager().setPool(ownedItems);

        this.getBtnRemove4().setText("Sell all extras");
        this.getBtnRemove4().setToolTipText("Sell unneeded extra copies of all cards");
        this.getBtnRemove4().setCommand(new Command() {
            @Override
            public void run() {
                List<Entry<InventoryItem, Integer>> cardsToRemove = new LinkedList<Map.Entry<InventoryItem,Integer>>();
                for (Entry<InventoryItem, Integer> item : getDeckManager().getPool()) {
                    PaperCard card = (PaperCard)item.getKey();
                    int numToKeep = card.getRules().getType().isBasic() ? 50 : 4;
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
        
        this.getDeckManager().getPnlButtons().add(creditsLabel, "gap 5px");
        this.creditsLabel.setText("Credits: " + this.questData.getAssets().getCredits());

        final double multiPercent = this.multiplier * 100;
        final NumberFormat formatter = new DecimalFormat("#0.00");
        String maxSellingPrice = "";
        final int maxSellPrice = this.questData.getCards().getSellPriceLimit();

        if (maxSellPrice < Integer.MAX_VALUE) {
            maxSellingPrice = String.format("Maximum selling price is %d credits.", maxSellPrice);
        }
        this.getCatalogManager().getPnlButtons().remove(this.getBtnAdd4());
        this.getCatalogManager().getPnlButtons().add(fullCatalogToggle, "w 25%, h 30!", 0);
        this.getCatalogManager().getPnlButtons().add(sellPercentageLabel);
        this.sellPercentageLabel.setText("<html>Selling cards at " + formatter.format(multiPercent)
                + "% of their value.<br>" + maxSellingPrice + "</html>");

        //TODO: Add filter for SItemManagerUtil.StatTypes.PACK

        deckGenParent = removeTab(VDeckgen.SINGLETON_INSTANCE);
        allDecksParent = removeTab(VAllDecks.SINGLETON_INSTANCE);
        probsParent = removeTab(VProbabilities.SINGLETON_INSTANCE);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#canSwitchAway()
     */
    @Override
    public boolean canSwitchAway(boolean isClosing) {
        Singletons.getModel().getQuest().save();
        return true;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#resetUIChanges()
     */
    @Override
    public void resetUIChanges() {
        if (showingFullCatalog) {
            toggleFullCatalog();
        }
        
        CSubmenuQuestDecks.SINGLETON_INSTANCE.update();

        // undo Card Shop Specifics
        this.getCatalogManager().getPnlButtons().remove(sellPercentageLabel);
        this.getCatalogManager().getPnlButtons().remove(fullCatalogToggle);
        this.getCatalogManager().getPnlButtons().add(this.getBtnAdd4());

        this.getDeckManager().getPnlButtons().remove(creditsLabel);
        this.getBtnRemove4().setText(prevRem4Label);
        this.getBtnRemove4().setToolTipText(prevRem4Tooltip);
        this.getBtnRemove4().setCommand(prevRem4Cmd);

        VCardCatalog.SINGLETON_INSTANCE.getTabLabel().setText(CCTabLabel);
        VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText(CDTabLabel);

        this.getBtnAdd().setText(CCAddLabel);
        this.getBtnRemove().setText(CDRemLabel);

        //TODO: Remove filter for SItemManagerUtil.StatTypes.PACK
        
        //Re-add tabs
        if (deckGenParent != null) {
            deckGenParent.addDoc(VDeckgen.SINGLETON_INSTANCE);
        }
        if (allDecksParent != null) {
            allDecksParent.addDoc(VAllDecks.SINGLETON_INSTANCE);
        }
        if (probsParent != null) {
            probsParent.addDoc(VProbabilities.SINGLETON_INSTANCE);
        }
    }
}
