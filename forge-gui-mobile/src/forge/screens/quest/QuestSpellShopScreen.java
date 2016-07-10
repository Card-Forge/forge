package forge.screens.quest;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.FThreads;
import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.SpellShopManager;
import forge.itemmanager.ItemManager.ContextMenuBuilder;
import forge.itemmanager.filters.ItemFilter;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.model.FModel;
import forge.quest.QuestSpellShop;
import forge.quest.QuestUtil;
import forge.screens.TabPageScreen;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FLabel;
import forge.toolbox.FTextArea;
import forge.toolbox.GuiChoose;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Callback;
import forge.util.ItemPool;
import forge.util.Utils;

public class QuestSpellShopScreen extends TabPageScreen<QuestSpellShopScreen> {
    private final SpellShopPage spellShopPage;
    private final InventoryPage inventoryPage;
    private final FLabel btnBuySellMultiple = add(new FLabel.ButtonBuilder().font(FSkinFont.get(16)).parseSymbols().build());

    public QuestSpellShopScreen() {
        super("", QuestMenu.getMenu(), new SpellShopBasePage[] { new SpellShopPage(), new InventoryPage() });
        spellShopPage = ((SpellShopPage)tabPages[0]);
        inventoryPage = ((InventoryPage)tabPages[1]);

        btnBuySellMultiple.setVisible(false); //hide unless in multi-select mode
        btnBuySellMultiple.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                final SpellShopManager itemManager = ((SpellShopBasePage)getSelectedPage()).itemManager;
                final ItemPool<InventoryItem> items = itemManager.getSelectedItemPool();

                if (items.isEmpty()) {
                    //toggle off multi-select mode if no items selected
                    itemManager.toggleMultiSelectMode(-1);
                    return;
                }

                FThreads.invokeInBackgroundThread(new Runnable() {
                    @Override
                    public void run() {
                        if (getSelectedPage() == spellShopPage) {
                            spellShopPage.activateItems(items);
                        }
                        else {
                            inventoryPage.activateItems(items);
                        }
                        FThreads.invokeInEdtLater(new Runnable() {
                            @Override
                            public void run() {
                                updateCreditsLabel();
                            }
                        });
                    }
                });
            }
        });
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
        QuestUtil.updateQuestView(QuestMenu.getMenu());
        setHeaderCaption(FModel.getQuest().getName() + " - Spell Shop\n(" + FModel.getQuest().getRank() + ")");

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
        String credits = "Credits: " + QuestUtil.formatCredits(FModel.getQuest().getAssets().getCredits());
        spellShopPage.lblCredits.setText(credits);
        inventoryPage.lblCredits.setText(credits);
    }

    private void updateBuySellButtonCaption() {
        String caption;
        ItemPool<InventoryItem> items;
        long total;
        if (getSelectedPage() == spellShopPage) {
            caption = "Buy";
            items = spellShopPage.itemManager.getSelectedItemPool();
            total = QuestSpellShop.getTotalBuyCost(items);
        }
        else {
            caption = "Sell";
            items = inventoryPage.itemManager.getSelectedItemPool();
            total = QuestSpellShop.getTotalSellValue(items);
        }

        int count = items.countAll();
        if (count == 0) {
            caption = "Cancel";
        }
        else {
            if (count > 1) {
                String itemType = "card";
                for (Entry<InventoryItem, Integer> item : items) {
                    if (!(item.getKey() instanceof PaperCard)) {
                        itemType = "item";
                        break;
                    }
                }
                caption += " " + count + " " + itemType + "s";
            }
            caption += " for {CR} " + total;
        }
        btnBuySellMultiple.setText(caption);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        super.doLayout(startY, width, height);

        float padding = ItemFilter.PADDING;
        float buttonHeight = tabHeader.getHeight() - 2 * padding;
        btnBuySellMultiple.setBounds(padding, height - buttonHeight - padding, width - 2 * padding, buttonHeight);
    }

    private static abstract class SpellShopBasePage extends TabPage<QuestSpellShopScreen> {
        protected final SpellShopManager itemManager;
        protected FLabel lblCredits = new FLabel.Builder().icon(FSkinImage.QUEST_COINSTACK).iconScaleFactor(0.75f).font(FSkinFont.get(16)).build();

        protected SpellShopBasePage(String caption0, FImage icon0, boolean isShop0) {
            super(caption0, icon0);
            itemManager = add(new SpellShopManager(isShop0) {
                @Override
                public void toggleMultiSelectMode(int indexToSelect) {
                    super.toggleMultiSelectMode(indexToSelect);

                    //hide tabs and show Buy/Sell button while in multi-select mode
                    boolean multiSelectMode = getMultiSelectMode();
                    if (multiSelectMode) {
                        parentScreen.updateBuySellButtonCaption();
                    }
                    parentScreen.btnBuySellMultiple.setVisible(multiSelectMode);
                    parentScreen.tabHeader.setVisible(!multiSelectMode);
                }
            });
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
            itemManager.setSelectionChangedHandler(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    if (itemManager.getMultiSelectMode()) {
                        parentScreen.updateBuySellButtonCaption();
                    }
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

                    //invoke in background thread so other dialogs can be shown properly
                    FThreads.invokeInBackgroundThread(new Runnable() {
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
        public boolean fling(float velocityX, float velocityY) {
            if (itemManager.getMultiSelectMode()) {
                return false; //prevent changing tabs while in multi-select mode
            }
            return super.fling(velocityX, velocityY);
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
            QuestSpellShop.buy(items, itemManager, parentScreen.inventoryPage.itemManager, true);
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
                //invoke in background thread so other dialogs can be shown properly
                FThreads.invokeInBackgroundThread(new Runnable() {
                    @Override
                    public void run() {
                        QuestSpellShop.sellExtras(parentScreen.spellShopPage.itemManager, itemManager);
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
            QuestSpellShop.sell(items, parentScreen.spellShopPage.itemManager, itemManager, true);
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
