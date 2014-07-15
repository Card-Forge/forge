package forge.screens.quest;

import java.util.HashMap;
import java.util.Map;

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
import forge.toolbox.FEvent.FEventHandler;
import forge.util.ItemPool;

public class QuestSpellShopScreen extends TabPageScreen<QuestSpellShopScreen> {
    @SuppressWarnings("unchecked")
    public QuestSpellShopScreen() {
        super(new SpellShopPage(), new InventoryPage());
    }

    @Override
    public void onActivate() {
        super.onActivate();
        update();
    }

    public void update() {
        QuestSpellShop.updateDecksForEachCard();
        QuestSpellShop.updateMultiplier();
        ((SpellShopPage)tabPages[0]).refresh();
        ((InventoryPage)tabPages[1]).refresh();
    }

    private static class SpellShopPage extends TabPage<QuestSpellShopScreen> {
        private final SpellShopManager shopManager = add(new SpellShopManager(true));

        private SpellShopPage() {
            super("Spell Shop", FSkinImage.QUEST_BOOK);
            shopManager.setItemActivateHandler(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                }
            });
            shopManager.setContextMenuBuilder(new ContextMenuBuilder<InventoryItem>() {
                @Override
                public void buildMenu(final FDropDownMenu menu, final InventoryItem item) {
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

        public void refresh() {
            Map<ColumnDef, ItemColumn> colOverrides = new HashMap<ColumnDef, ItemColumn>();
            ItemColumn.addColOverride(ItemManagerConfig.SPELL_SHOP, colOverrides, ColumnDef.PRICE, QuestSpellShop.fnPriceCompare, QuestSpellShop.fnPriceGet);
            ItemColumn.addColOverride(ItemManagerConfig.SPELL_SHOP, colOverrides, ColumnDef.OWNED, FModel.getQuest().getCards().getFnOwnedCompare(), FModel.getQuest().getCards().getFnOwnedGet());
            shopManager.setup(ItemManagerConfig.SPELL_SHOP, colOverrides);

            shopManager.setPool(FModel.getQuest().getCards().getShopList());
        }

        @Override
        protected void doLayout(float width, float height) {
            shopManager.setBounds(0, 0, width, height);
        }
    }

    private static class InventoryPage extends TabPage<QuestSpellShopScreen> {
        private final SpellShopManager inventoryManager = add(new SpellShopManager(false));

        private InventoryPage() {
            super("Inventory", FSkinImage.QUEST_BOX);
            inventoryManager.setItemActivateHandler(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                }
            });
            inventoryManager.setContextMenuBuilder(new ContextMenuBuilder<InventoryItem>() {
                @Override
                public void buildMenu(final FDropDownMenu menu, final InventoryItem item) {
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

        public void refresh() {
            Map<ColumnDef, ItemColumn> colOverrides = new HashMap<ColumnDef, ItemColumn>();
            ItemColumn.addColOverride(ItemManagerConfig.QUEST_INVENTORY, colOverrides, ColumnDef.PRICE, QuestSpellShop.fnPriceCompare, QuestSpellShop.fnPriceSellGet);
            ItemColumn.addColOverride(ItemManagerConfig.QUEST_INVENTORY, colOverrides, ColumnDef.NEW, FModel.getQuest().getCards().getFnNewCompare(), FModel.getQuest().getCards().getFnNewGet());
            ItemColumn.addColOverride(ItemManagerConfig.QUEST_INVENTORY, colOverrides, ColumnDef.DECKS, QuestSpellShop.fnDeckCompare, QuestSpellShop.fnDeckGet);
            inventoryManager.setup(ItemManagerConfig.QUEST_INVENTORY, colOverrides);

            final ItemPool<InventoryItem> ownedItems = new ItemPool<InventoryItem>(InventoryItem.class);
            ownedItems.addAll(FModel.getQuest().getCards().getCardpool().getView());
            inventoryManager.setPool(ownedItems);
        }

        @Override
        protected void doLayout(float width, float height) {
            inventoryManager.setBounds(0, 0, width, height);
        }
    }
}
