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
package forge.screens.deckeditor.controllers;

import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.deck.DeckBase;
import forge.gui.framework.DragCell;
import forge.gui.framework.FScreen;
import forge.item.*;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.SpellShopManager;
import forge.itemmanager.views.ItemTableColumn;
import forge.model.FModel;
import forge.quest.QuestController;
import forge.quest.QuestSpellShop;
import forge.quest.QuestUtil;
import forge.screens.deckeditor.views.*;
import forge.screens.home.quest.CSubmenuQuestDecks;
import forge.screens.match.controllers.CDetailPicture;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin;
import forge.util.ItemPool;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Child controller for quest card shop UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 * 
 * @author Forge
 * @version $Id: CEditorQuestCardShop.java 15088 2012-04-07 11:34:05Z Max mtg $
 */
public final class CEditorQuestCardShop extends ACEditorBase<InventoryItem, DeckBase> {
    private final FLabel creditsLabel = new FLabel.Builder()
            .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_COINSTACK))
            .fontSize(15).build();

    // TODO: move these to the view where they belong
    private final FLabel sellPercentageLabel = new FLabel.Builder().text("0")
            .fontSize(11)
            .build();
    @SuppressWarnings("serial")
    private final FLabel fullCatalogToggle = new FLabel.Builder().text("See full catalog")
            .fontSize(14).hoverable(true).cmdClick(new UiCommand() {
                @Override
                public void run() {
                    toggleFullCatalog();
                }
            })
            .build();

    private final QuestController questData;

    private ItemPool<InventoryItem> cardsForSale;
    private final ItemPool<InventoryItem> fullCatalogCards =
            ItemPool.createFrom(FModel.getMagicDb().getCommonCards().getAllCards(), InventoryItem.class);
    private boolean showingFullCatalog = false;
    private DragCell allDecksParent = null;
    private DragCell deckGenParent = null;
    private DragCell probsParent = null;

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
    public CEditorQuestCardShop(final QuestController qd, final CDetailPicture cDetailPicture) {
        super(FScreen.QUEST_CARD_SHOP, cDetailPicture);

        this.questData = qd;

        final SpellShopManager catalogManager = new SpellShopManager(getCDetailPicture(), false);
        final SpellShopManager deckManager = new SpellShopManager(getCDetailPicture(), false);

        catalogManager.setCaption("Spell Shop");
        deckManager.setCaption("Quest Inventory");

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
        }
        else {
            this.getCatalogManager().setPool(cardsForSale);
            this.getBtnAdd().setEnabled(true);
            this.getBtnRemove().setEnabled(true);
            this.getBtnRemove4().setEnabled(true);
            fullCatalogToggle.setText("See full catalog");
        }
    }

    //=========== Overridden from ACEditorBase

    @Override
    protected CardLimit getCardLimit() {
        return CardLimit.None;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onAddItems()
     */
    @Override
    protected void onAddItems(Iterable<Entry<InventoryItem, Integer>> items, boolean toAlternate) {
        // disallow "buying" cards while showing the full catalog
        if (showingFullCatalog || toAlternate) {
            return;
        }

        QuestSpellShop.buy(items, this.getCatalogManager(), this.getDeckManager(), true);
        updateCreditsLabel();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onRemoveItems()
     */
    @Override
    protected void onRemoveItems(Iterable<Entry<InventoryItem, Integer>> items, boolean toAlternate) {
        if (showingFullCatalog || toAlternate) { return; }

        QuestSpellShop.sell(items, this.getCatalogManager(), this.getDeckManager(), true);
        updateCreditsLabel();
    }

    @Override
    protected void buildAddContextMenu(EditorContextMenuBuilder cmb) {
        if (!showingFullCatalog) {
            cmb.addMoveItems("Buy", null);
        }
    }

    @Override
    protected void buildRemoveContextMenu(EditorContextMenuBuilder cmb) {
        if (!showingFullCatalog) {
            cmb.addMoveItems("Sell", null);
        }
    }

    private void updateCreditsLabel() {
        this.creditsLabel.setText("Credits: " + QuestUtil.formatCredits(this.questData.getAssets().getCredits()));
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
        final Map<ColumnDef, ItemTableColumn> colOverridesCatalog = new HashMap<ColumnDef, ItemTableColumn>();
        final Map<ColumnDef, ItemTableColumn> colOverridesDeck = new HashMap<ColumnDef, ItemTableColumn>();

        // Add spell shop-specific columns
        ItemTableColumn.addColOverride(ItemManagerConfig.SPELL_SHOP, colOverridesCatalog, ColumnDef.PRICE, QuestSpellShop.fnPriceCompare, QuestSpellShop.fnPriceGet);
        ItemTableColumn.addColOverride(ItemManagerConfig.SPELL_SHOP, colOverridesCatalog, ColumnDef.OWNED, questData.getCards().getFnOwnedCompare(), questData.getCards().getFnOwnedGet());
        ItemTableColumn.addColOverride(ItemManagerConfig.QUEST_INVENTORY, colOverridesDeck, ColumnDef.PRICE, QuestSpellShop.fnPriceCompare, QuestSpellShop.fnPriceSellGet);
        ItemTableColumn.addColOverride(ItemManagerConfig.QUEST_INVENTORY, colOverridesDeck, ColumnDef.NEW, this.questData.getCards().getFnNewCompare(), this.questData.getCards().getFnNewGet());
        ItemTableColumn.addColOverride(ItemManagerConfig.QUEST_INVENTORY, colOverridesDeck, ColumnDef.DECKS, QuestSpellShop.fnDeckCompare, QuestSpellShop.fnDeckGet);

        // Setup with current column set
        this.getCatalogManager().setup(ItemManagerConfig.SPELL_SHOP, colOverridesCatalog);
        this.getDeckManager().setup(ItemManagerConfig.QUEST_INVENTORY, colOverridesDeck);

        resetUI();

        CCTabLabel = VCardCatalog.SINGLETON_INSTANCE.getTabLabel().getText();
        VCardCatalog.SINGLETON_INSTANCE.getTabLabel().setText("Cards for sale");

        CCAddLabel = this.getBtnAdd().getText();
        this.getBtnAdd().setText("Buy Card");

        CDTabLabel = VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().getText();
        VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText("Your Cards");

        CDRemLabel = this.getBtnRemove().getText();
        this.getBtnRemove().setText("Sell Card");

        this.getBtnAddBasicLands().setVisible(false);

        VProbabilities.SINGLETON_INSTANCE.getTabLabel().setVisible(false);

        prevRem4Label = this.getBtnRemove4().getText();
        prevRem4Tooltip = this.getBtnRemove4().getToolTipText();
        prevRem4Cmd = this.getBtnRemove4().getCommand();

        VCurrentDeck.SINGLETON_INSTANCE.getPnlHeader().setVisible(false);

        QuestSpellShop.updateDecksForEachCard();
        double multiplier = QuestSpellShop.updateMultiplier();
        this.cardsForSale = this.questData.getCards().getShopList();

        final ItemPool<InventoryItem> ownedItems = new ItemPool<InventoryItem>(InventoryItem.class);
        ownedItems.addAllOfType(this.questData.getCards().getCardpool().getView());

        this.getCatalogManager().setPool(cardsForSale);
        this.getDeckManager().setPool(ownedItems);

        this.getBtnRemove4().setText("Sell all extras");
        this.getBtnRemove4().setToolTipText("Sell unneeded extra copies of all cards");
        this.getBtnRemove4().setCommand(new UiCommand() {
            @Override
            public void run() {
                QuestSpellShop.sellExtras(getCatalogManager(), getDeckManager());
                updateCreditsLabel();
            }
        });

        this.getDeckManager().getPnlButtons().add(creditsLabel, "gap 5px");
        updateCreditsLabel();

        final double multiPercent = multiplier * 100;
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
        FModel.getQuest().save();
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
