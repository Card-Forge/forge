package forge.screens.quest;

import java.util.HashMap;
import java.util.Map;
import forge.assets.FImage;
import forge.assets.FSkinImage;
import forge.card.CardZoom;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.SpellShopManager;
import forge.itemmanager.ItemManager.ContextMenuBuilder;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.model.FModel;
import forge.quest.QuestSpellShop;
import forge.screens.TabPageScreen;
import forge.toolbox.FEvent;
import forge.toolbox.GuiChoose;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Callback;
import forge.util.ItemPool;
import forge.util.ThreadUtil;

public class QuestSpellShopScreen extends TabPageScreen<QuestSpellShopScreen> {
    @SuppressWarnings("unchecked")
    public QuestSpellShopScreen() {
        super(new SpellShopPage(), new InventoryPage());
        ((SpellShopPage)tabPages[0]).parentScreen = this;
        ((InventoryPage)tabPages[1]).parentScreen = this;
    }

    @Override
    public void onActivate() {
        super.onActivate();
        update();
    }

    public void onClose(Callback<Boolean> canCloseCallback) {
        FModel.getQuest().save();
        super.onClose(canCloseCallback);
    }

    public void update() {
        QuestSpellShop.updateDecksForEachCard();
        QuestSpellShop.updateMultiplier();
        ((SpellShopPage)tabPages[0]).refresh();
        ((InventoryPage)tabPages[1]).refresh();
    }
    
    private static abstract class SpellShopBasePage extends TabPage<QuestSpellShopScreen> {
        protected final SpellShopManager itemManager;
        protected QuestSpellShopScreen parentScreen;

        protected SpellShopBasePage(String caption0, FImage icon0, boolean isShop0) {
            super(caption0, icon0);
            itemManager = add(new SpellShopManager(isShop0));
            itemManager.setItemActivateHandler(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                }
            });
            itemManager.setContextMenuBuilder(new ContextMenuBuilder<InventoryItem>() {
                @Override
                public void buildMenu(final FDropDownMenu menu, final InventoryItem item) {
                    menu.addItem(new FMenuItem(getVerb(), getVerbIcon(), new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            activateSelectedItem();
                        }
                    }));
                    if (item instanceof PaperCard) {
                        menu.addItem(new FMenuItem("Zoom/Details", new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                CardZoom.show((PaperCard)item);
                            }
                        }));
                    }
                }
            });
        }

        protected abstract void activateItems(ItemPool<InventoryItem> items);
        protected abstract void refresh();
        protected abstract String getVerb();
        protected abstract FSkinImage getVerbIcon();

        private void activateSelectedItem() {
            final InventoryItem item = itemManager.getSelectedItem();
            final int max = itemManager.getItemCount(item);
            if (max == 0) { return; }

            final Callback<Integer> callback = new Callback<Integer>() {
                @Override
                public void run(final Integer result) {
                    if (result == null || result <= 0) { return; }

                    //invoke in game thread so other dialogs can be shown properly
                    ThreadUtil.invokeInGameThread(new Runnable() {
                        @Override
                        public void run() {
                            ItemPool<InventoryItem> items = new ItemPool<InventoryItem>(InventoryItem.class);
                            items.add(item, result);
                            activateItems(items);
                        }
                    });
                }
            };
            if (max == 1) {
                callback.run(max);
            }
            else {
                GuiChoose.getInteger(item + " - " + getVerb() + " how many?", 1, max, 20, callback);
            }
        }

        @Override
        protected void doLayout(float width, float height) {
            itemManager.setBounds(0, 0, width, height);
        }
    }

    private static class SpellShopPage extends SpellShopBasePage {
        private SpellShopPage() {
            super("Cards for Sale", FSkinImage.QUEST_BOOK, true);
        }

        @Override
        protected void refresh() {
            Map<ColumnDef, ItemColumn> colOverrides = new HashMap<ColumnDef, ItemColumn>();
            ItemColumn.addColOverride(ItemManagerConfig.SPELL_SHOP, colOverrides, ColumnDef.PRICE, QuestSpellShop.fnPriceCompare, QuestSpellShop.fnPriceGet);
            ItemColumn.addColOverride(ItemManagerConfig.SPELL_SHOP, colOverrides, ColumnDef.OWNED, FModel.getQuest().getCards().getFnOwnedCompare(), FModel.getQuest().getCards().getFnOwnedGet());
            itemManager.setup(ItemManagerConfig.SPELL_SHOP, colOverrides);

            itemManager.setPool(FModel.getQuest().getCards().getShopList());
        }

        @Override
        protected void activateItems(ItemPool<InventoryItem> items) {
            QuestSpellShop.buy(items, itemManager, ((InventoryPage)parentScreen.tabPages[1]).itemManager, true);
        }

        @Override
        protected String getVerb() {
            return "Buy";
        }

        @Override
        protected FSkinImage getVerbIcon() {
            return FSkinImage.PLUS;
        }
    }

    private static class InventoryPage extends SpellShopBasePage {
        private InventoryPage() {
            super("Your Cards", FSkinImage.QUEST_BOX, false);
        }

        @Override
        protected void refresh() {
            Map<ColumnDef, ItemColumn> colOverrides = new HashMap<ColumnDef, ItemColumn>();
            ItemColumn.addColOverride(ItemManagerConfig.QUEST_INVENTORY, colOverrides, ColumnDef.PRICE, QuestSpellShop.fnPriceCompare, QuestSpellShop.fnPriceSellGet);
            ItemColumn.addColOverride(ItemManagerConfig.QUEST_INVENTORY, colOverrides, ColumnDef.NEW, FModel.getQuest().getCards().getFnNewCompare(), FModel.getQuest().getCards().getFnNewGet());
            ItemColumn.addColOverride(ItemManagerConfig.QUEST_INVENTORY, colOverrides, ColumnDef.DECKS, QuestSpellShop.fnDeckCompare, QuestSpellShop.fnDeckGet);
            itemManager.setup(ItemManagerConfig.QUEST_INVENTORY, colOverrides);

            final ItemPool<InventoryItem> ownedItems = new ItemPool<InventoryItem>(InventoryItem.class);
            ownedItems.addAll(FModel.getQuest().getCards().getCardpool().getView());
            itemManager.setPool(ownedItems);
        }

        @Override
        protected void activateItems(ItemPool<InventoryItem> items) {
            QuestSpellShop.sell(items, ((SpellShopPage)parentScreen.tabPages[0]).itemManager, itemManager, true);
        }

        @Override
        protected String getVerb() {
            return "Sell";
        }

        @Override
        protected FSkinImage getVerbIcon() {
            return FSkinImage.MINUS;
        }
    }
}
