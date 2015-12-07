package forge.screens.quest;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.FThreads;
import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.item.InventoryItem;
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
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FLabel;
import forge.toolbox.FTextArea;
import forge.toolbox.GuiChoose;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Callback;
import forge.util.ItemPool;
import forge.util.ThreadUtil;
import forge.util.Utils;

public class QuestSpellShopScreen extends TabPageScreen<QuestSpellShopScreen> {
    private final SpellShopPage spellShopPage;
    private final InventoryPage inventoryPage;

    @SuppressWarnings("unchecked")
    public QuestSpellShopScreen() {
        super(new SpellShopPage(), new InventoryPage());
        spellShopPage = ((SpellShopPage)tabPages[0]);
        inventoryPage = ((InventoryPage)tabPages[1]);
        spellShopPage.parentScreen = this;
        inventoryPage.parentScreen = this;
    }

    @Override
    public void onActivate() {
        super.onActivate();
        update();
    }

    @Override
    protected boolean allowBackInLandscapeMode() {
        return true;
    }

    public void onClose(Callback<Boolean> canCloseCallback) {
        FModel.getQuest().save();
        super.onClose(canCloseCallback);
    }

    public void update() {
        QuestSpellShop.updateDecksForEachCard();
        double multiplier = QuestSpellShop.updateMultiplier();
        spellShopPage.refresh();
        inventoryPage.refresh();
        updateCreditsLabel();

        final double multiPercent = multiplier * 100;
        final NumberFormat formatter = new DecimalFormat("#0.00");
        String maxSellingPrice = "";
        final int maxSellPrice = FModel.getQuest().getCards().getSellPriceLimit();

        if (maxSellPrice < Integer.MAX_VALUE) {
            maxSellingPrice = String.format("Maximum selling price is %d credits.", maxSellPrice);
        }
        spellShopPage.lblSellPercentage.setText("Selling cards at " + formatter.format(multiPercent)
                + "% of their value.\n" + maxSellingPrice);
    }

    public void updateCreditsLabel() {
        String credits = "Credits: " + FModel.getQuest().getAssets().getCredits();
        spellShopPage.lblCredits.setText(credits);
        inventoryPage.lblCredits.setText(credits);
    }

    private static abstract class SpellShopBasePage extends TabPage<QuestSpellShopScreen> {
        protected final SpellShopManager itemManager;
        protected QuestSpellShopScreen parentScreen;
        protected FLabel lblCredits = new FLabel.Builder().icon(FSkinImage.QUEST_COINSTACK).iconScaleFactor(1f).font(FSkinFont.get(16)).build();

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
                }
            });
            add(lblCredits);
        }

        protected abstract void activateItems(ItemPool<InventoryItem> items);
        protected abstract void refresh();
        protected abstract String getVerb();
        protected abstract FSkinImage getVerbIcon();
        protected abstract FDisplayObject getSecondLabel();

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
                            FThreads.invokeInEdtLater(new Runnable() {
                                @Override
                                public void run() {
                                    parentScreen.updateCreditsLabel();
                                }
                            });
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
            float y = Utils.scale(2); //move credits label down a couple pixels so it looks better
            float halfWidth = width / 2;
            lblCredits.setBounds(0, y, halfWidth, lblCredits.getAutoSizeBounds().height);
            getSecondLabel().setBounds(halfWidth, y, halfWidth, lblCredits.getHeight());
            itemManager.setBounds(0, lblCredits.getHeight(), width, height - lblCredits.getHeight());
        }
    }

    private static class SpellShopPage extends SpellShopBasePage {
        private FTextArea lblSellPercentage = add(new FTextArea(false));

        private SpellShopPage() {
            super("Cards for Sale", FSkinImage.QUEST_BOOK, true);
            lblSellPercentage.setFont(FSkinFont.get(11));
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

        @Override
        protected FDisplayObject getSecondLabel() {
            return lblSellPercentage;
        }
    }

    private static class InventoryPage extends SpellShopBasePage {
        protected FLabel lblSellExtras = add(new FLabel.Builder().text("Sell all extras")
                .icon(FSkinImage.MINUS).iconScaleFactor(1f).align(HAlignment.RIGHT).font(FSkinFont.get(16))
                .command(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                //invoke in game thread so other dialogs can be shown properly
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        QuestSpellShop.sellExtras(((SpellShopPage)parentScreen.tabPages[0]).itemManager, itemManager);
                        FThreads.invokeInEdtLater(new Runnable() {
                            @Override
                            public void run() {
                                parentScreen.updateCreditsLabel();
                            }
                        });
                    }
                });
            }
        }).build());

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
            ownedItems.addAllOfType(FModel.getQuest().getCards().getCardpool().getView());
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

        @Override
        protected FDisplayObject getSecondLabel() {
            return lblSellExtras;
        }
    }
}
