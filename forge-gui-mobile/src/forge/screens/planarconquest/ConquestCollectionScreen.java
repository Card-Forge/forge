package forge.screens.planarconquest;

import java.util.Collection;
import java.util.Map.Entry;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import forge.Forge;
import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.TextRenderer;
import forge.deck.CardPool;
import forge.gamemodes.planarconquest.ConquestData;
import forge.gamemodes.planarconquest.ConquestPlane;
import forge.gamemodes.planarconquest.ConquestPreferences;
import forge.gamemodes.planarconquest.ConquestUtil;
import forge.gamemodes.planarconquest.ConquestPreferences.CQPref;
import forge.gui.FThreads;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.filters.CardColorFilter;
import forge.itemmanager.filters.CardTypeFilter;
import forge.itemmanager.filters.ComboBoxFilter;
import forge.itemmanager.filters.ItemFilter;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.model.FModel;
import forge.screens.TabPageScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FLabel;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Localizer;

public class ConquestCollectionScreen extends TabPageScreen<ConquestCollectionScreen> {
    private final FLabel lblShards = add(new FLabel.Builder().font(ConquestAEtherScreen.LABEL_FONT).parseSymbols().build());
    private final FLabel lblInfo = add(new FLabel.Builder().font(FSkinFont.get(11)).build());
    private final FLabel btnExileRetrieveMultiple = add(new FLabel.ButtonBuilder().font(ConquestAEtherScreen.LABEL_FONT).parseSymbols().build());

    public ConquestCollectionScreen() {
        super("", ConquestMenu.getMenu(), new CollectionTab[] {
            new CollectionTab(Localizer.getInstance().getMessage("lblCollection"), FSkinImage.SPELLBOOK),
            new CollectionTab(Localizer.getInstance().getMessage("lblExile"), FSkinImage.EXILE)
        }, true);
        btnExileRetrieveMultiple.setVisible(false); //hide unless in multi-select mode
        btnExileRetrieveMultiple.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                final CardManager list = ((CollectionTab)getSelectedPage()).list;
                final Collection<PaperCard> cards = list.getSelectedItems();

                if (cards.isEmpty()) {
                    //toggle off multi-select mode if no items selected
                    list.toggleMultiSelectMode(-1);
                    return;
                }

                FThreads.invokeInBackgroundThread(new Runnable() {
                    @Override
                    public void run() {
                        if (getSelectedPage() == tabPages[0]) {
                            int value = 0;
                            for (PaperCard card : cards) {
                                value += ConquestUtil.getShardValue(card, CQPref.AETHER_BASE_EXILE_VALUE);
                            }
                            if (FModel.getConquest().getModel().exileCards(cards, value)) {
                                FThreads.invokeInEdtLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateShards();
                                        getCollectionTab().list.removeItemsFlat(cards);
                                        getExileTab().list.addItemsFlat(cards);
                                        updateTabCaptions();
                                    }
                                });
                            }
                        }
                        else {
                            int cost = 0;
                            for (PaperCard card : cards) {
                                cost += ConquestUtil.getShardValue(card, CQPref.AETHER_BASE_RETRIEVE_COST);
                            }
                            if (FModel.getConquest().getModel().retrieveCardsFromExile(cards, cost)) {
                                FThreads.invokeInEdtLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateShards();
                                        getCollectionTab().list.addItemsFlat(cards);
                                        getExileTab().list.removeItemsFlat(cards);
                                        updateTabCaptions();
                                    }
                                });
                            }
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onActivate() {
        setHeaderCaption(FModel.getConquest().getModel().getName());
        updateShards();
        updateInfo();
        refreshCards();
    }

    private void updateShards() {
        int availableShards = FModel.getConquest().getModel().getAEtherShards();
        lblShards.setText(Localizer.getInstance().getMessage("lblHaveNAEShards", String.valueOf(availableShards) ,"{AE}"));
    }
    
    private void updateInfo() {
        ConquestPreferences prefs = FModel.getConquestPreferences();
        double baseValue = prefs.getPrefInt(CQPref.AETHER_BASE_DUPLICATE_VALUE);
        double exileValue = prefs.getPrefInt(CQPref.AETHER_BASE_EXILE_VALUE);
        double retrieveCost = prefs.getPrefInt(CQPref.AETHER_BASE_RETRIEVE_COST);

        lblInfo.setText(Localizer.getInstance().getMessage("lblExileRetrieveProportion", Math.round(100 * exileValue / baseValue), Math.round(100 * retrieveCost / baseValue)));
    }

    private void refreshCards() {
        ConquestData model = FModel.getConquest().getModel();
        CardPool collection = new CardPool();
        CardPool exile = new CardPool();
        collection.add(model.getUnlockedCards());
        collection.removeAllFlat(model.getExiledCards());
        exile.add(model.getExiledCards());
        getCollectionTab().list.setPool(collection, true);
        getExileTab().list.setPool(exile, true);
        updateTabCaptions();
    }

    private void updateTabCaptions() {
        getCollectionTab().updateCaption();
        getExileTab().updateCaption();
    }

    private void updateExileRetrieveButtonCaption() {
        String caption;
        CQPref baseValuePref;
        Collection<PaperCard> cards;
        if (getSelectedPage() == tabPages[0]) {
            caption = Localizer.getInstance().getMessage("lblExile");
            baseValuePref = CQPref.AETHER_BASE_EXILE_VALUE;
            cards = getCollectionTab().list.getSelectedItems();
        }
        else {
            caption = Localizer.getInstance().getMessage("lblRetrieve");
            baseValuePref = CQPref.AETHER_BASE_RETRIEVE_COST;
            cards = getExileTab().list.getSelectedItems();
        }

        int count = cards.size();
        if (count == 0) {
            caption = Localizer.getInstance().getMessage("lblCancel");
        }
        else {
            if (count > 1) {
                caption += " " + count + " " + Localizer.getInstance().getMessage("lblCards");
            }
            int total = 0;
            for (PaperCard card : cards) {
                total += ConquestUtil.getShardValue(card, baseValuePref);
            }
            caption += " for {AE}" + total;
        }
        btnExileRetrieveMultiple.setText(caption);
    }

    private CollectionTab getCollectionTab() {
        return (CollectionTab)tabPages[0];
    }

    private CollectionTab getExileTab() {
        return (CollectionTab)tabPages[1];
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = ItemFilter.PADDING;
        float y = startY + ItemFilter.PADDING;
        float w = width - 2 * x;
        float labelWidth = w * 0.4f;
        float labelHeight = lblShards.getAutoSizeBounds().height;

        lblShards.setBounds(x, y, labelWidth, labelHeight);
        labelWidth += ItemFilter.PADDING;
        lblInfo.setBounds(x + labelWidth, y, w - labelWidth, labelHeight);
        y += labelHeight;
        super.doLayout(y, width, height);

        float buttonHeight = tabHeader.getHeight() - 2 * ItemFilter.PADDING;
        btnExileRetrieveMultiple.setBounds(x, height - buttonHeight - ItemFilter.PADDING, w, buttonHeight);
    }

    private static class CollectionTab extends TabPage<ConquestCollectionScreen> {
        private final CollectionManager list;

        private CollectionTab(String caption0, FImage icon0) {
            super(caption0, icon0);
            list = add(new CollectionManager(caption0));

            ItemManagerConfig config = ItemManagerConfig.CONQUEST_COLLECTION;
            list.setup(config, ConquestData.getColOverrides(config));
        }

        private void updateCaption() {
            caption = list.getCaption() + " (" + list.getItemCount() + ")";
        }

        @Override
        protected void doLayout(float width, float height) {
            list.setBounds(0, 0, width, height);
        }

        @Override
        public boolean fling(float velocityX, float velocityY) {
            if (list.getMultiSelectMode()) {
                return false; //prevent changing tabs while in multi-select mode
            }
            return super.fling(velocityX, velocityY);
        }

        private class CollectionManager extends CardManager {
            public CollectionManager(String caption0) {
                super(false);
                setCaption(caption0);
                setContextMenuBuilder(new ContextMenuBuilder<PaperCard>() {
                    @Override
                    public void buildMenu(final FDropDownMenu menu, final PaperCard card) {
                        final FMenuItem item;
                        final ConquestData model = FModel.getConquest().getModel();
                        if (model.isInExile(card)) {
                            final int cost = ConquestUtil.getShardValue(card, CQPref.AETHER_BASE_RETRIEVE_COST);
                            item = new FMenuItem(Localizer.getInstance().getMessage("lblRetrieveForNAE", String.valueOf(cost), "{AE}"), Forge.hdbuttons ? FSkinImage.HDPLUS : FSkinImage.PLUS, new FEventHandler() {
                                @Override
                                public void handleEvent(FEvent e) {
                                    FThreads.invokeInBackgroundThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (model.retrieveCardsFromExile(ImmutableList.of(card), cost)) {
                                                FThreads.invokeInEdtLater(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        parentScreen.updateShards();
                                                        parentScreen.getCollectionTab().list.addItem(card, 1);
                                                        parentScreen.getExileTab().list.removeItem(card, 1);
                                                        parentScreen.updateTabCaptions();
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            }, true);
                        }
                        else {
                            final int value = ConquestUtil.getShardValue(card, CQPref.AETHER_BASE_EXILE_VALUE);
                            item = new FMenuItem(Localizer.getInstance().getMessage("lblExileForNAE", String.valueOf(value), "{AE}"), FSkinImage.EXILE, new FEventHandler() {
                                @Override
                                public void handleEvent(FEvent e) {
                                    FThreads.invokeInBackgroundThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (model.exileCards(ImmutableList.of(card), value)) {
                                                FThreads.invokeInEdtLater(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        parentScreen.updateShards();
                                                        parentScreen.getCollectionTab().list.removeItem(card, 1);
                                                        parentScreen.getExileTab().list.addItem(card, 1);
                                                        parentScreen.updateTabCaptions();
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            }, true);
                        }
                        item.setTextRenderer(new TextRenderer());
                        menu.addItem(item);
                    }
                });
                setSelectionChangedHandler(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        if (getMultiSelectMode()) {
                            parentScreen.updateExileRetrieveButtonCaption();
                        }
                    }
                });
            }

            @Override
            protected void addDefaultFilters() {
                addFilter(new CardColorFilter(this));
                addFilter(new CardOriginFilter(this));
                addFilter(new CardTypeFilter(this));
            }

            @Override
            public void toggleMultiSelectMode(int indexToSelect) {
                super.toggleMultiSelectMode(indexToSelect);

                //hide tabs and show Exile/Retrieve button while in multi-select mode
                boolean multiSelectMode = getMultiSelectMode();
                if (multiSelectMode) {
                    parentScreen.updateExileRetrieveButtonCaption();
                }
                parentScreen.btnExileRetrieveMultiple.setVisible(multiSelectMode);
                parentScreen.tabHeader.setVisible(!multiSelectMode);
            }

            @Override
            protected void onCardLongPress(int index, Entry<PaperCard, Integer> value, float x, float y) {
                toggleMultiSelectMode(index);
            }
        }

        private static class CardOriginFilter extends ComboBoxFilter<PaperCard, ConquestPlane> {
            public CardOriginFilter(ItemManager<? super PaperCard> itemManager0) {
                super(Localizer.getInstance().getMessage("lblAllPlanes"), FModel.getPlanes(), itemManager0);
            }

            @Override
            public ItemFilter<PaperCard> createCopy() {
                CardOriginFilter copy = new CardOriginFilter(itemManager);
                copy.filterValue = filterValue;
                return copy;
            }

            @Override
            protected Predicate<PaperCard> buildPredicate() {
                return new Predicate<PaperCard>() {
                    @Override
                    public boolean apply(PaperCard input) {
                        if (filterValue == null) {
                            return true;
                        }
                        return filterValue.getCardPool().contains(input);
                    }
                };
            }
        }
    }
}
