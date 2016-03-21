package forge.screens.planarconquest;

import java.util.Map.Entry;

import com.google.common.base.Predicate;

import forge.FThreads;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.TextRenderer;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.deck.CardPool;
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
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestPlane;
import forge.planarconquest.ConquestPreferences;
import forge.planarconquest.ConquestUtil;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.screens.FScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FList.CompactModeHandler;

public class ConquestCollectionScreen extends FScreen {
    private static final float EXILED_ALPHA_COMPOSITE = 0.4f;
    private static final float EXILE_ICON_ALPHA_COMPOSITE = 0.7f;

    private final FLabel lblShards = add(new FLabel.Builder().font(ConquestAEtherScreen.LABEL_FONT).parseSymbols().build());
    private final FLabel lblInfo = add(new FLabel.Builder().font(FSkinFont.get(11)).build());
    private final CollectionManager lstCollection = add(new CollectionManager());

    public ConquestCollectionScreen() {
        super("", ConquestMenu.getMenu());

        ItemManagerConfig config = ItemManagerConfig.CONQUEST_COLLECTION;
        lstCollection.setup(config, ConquestData.getColOverrides(config));
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
        lblShards.setText("Shards: {AE}" + availableShards);
    }
    
    private void updateInfo() {
        ConquestPreferences prefs = FModel.getConquestPreferences();
        double baseValue = prefs.getPrefInt(CQPref.AETHER_BASE_DUPLICATE_VALUE);
        double exileValue = prefs.getPrefInt(CQPref.AETHER_BASE_EXILE_VALUE);
        double retrieveCost = prefs.getPrefInt(CQPref.AETHER_BASE_RETRIEVE_COST);

        lblInfo.setText("Exile unneeded cards at " + Math.round(100 * exileValue / baseValue) +
                "% value.\nRetrieve exiled cards for " + Math.round(100 * retrieveCost / baseValue) +
                "% value.");
    }

    private void refreshCards() {
        CardPool pool = new CardPool();
        pool.add(FModel.getConquest().getModel().getUnlockedCards());
        lstCollection.setPool(pool, true);
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
        lstCollection.setBounds(x, y, w, height - y - ItemFilter.PADDING);
    }

    private class CollectionManager extends CardManager {
        public CollectionManager() {
            super(false);
            setCaption("Collection");
            setContextMenuBuilder(new ContextMenuBuilder<PaperCard>() {
                @Override
                public void buildMenu(final FDropDownMenu menu, final PaperCard card) {
                    final FMenuItem item;
                    final ConquestData model = FModel.getConquest().getModel();
                    if (model.isInExile(card)) {
                        final int cost = ConquestUtil.getShardValue(card, CQPref.AETHER_BASE_RETRIEVE_COST);
                        item = new FMenuItem("Retrieve for {AE}" + cost, FSkinImage.PLUS, new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                FThreads.invokeInBackgroundThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (model.retrieveCardFromExile(card, cost)) {
                                            FThreads.invokeInEdtLater(afterShardsChanged);
                                        }
                                    }
                                });
                            }
                        }, true);
                    }
                    else {
                        final int value = ConquestUtil.getShardValue(card, CQPref.AETHER_BASE_EXILE_VALUE);
                        item = new FMenuItem("Exile for {AE}" + value, FSkinImage.EXILE, new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                FThreads.invokeInBackgroundThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (model.exileCard(card, value)) {
                                            FThreads.invokeInEdtLater(afterShardsChanged);
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
        }

        private final Runnable afterShardsChanged = new Runnable() {
            @Override
            public void run() {
                updateShards();
            }
        };

        @Override
        protected void addDefaultFilters() {
            addFilter(new CardColorFilter(this));
            addFilter(new CardOriginFilter(this));
            addFilter(new CardTypeFilter(this));
        }

        @Override
        public ItemRenderer getListItemRenderer(final CompactModeHandler compactModeHandler) {
            return new ItemRenderer() {
                @Override
                public float getItemHeight() {
                    return CardRenderer.getCardListItemHeight(compactModeHandler.isCompactMode());
                }

                @Override
                public void drawValue(Graphics g, Entry<PaperCard, Integer> value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                    boolean exiled = FModel.getConquest().getModel().isInExile(value.getKey());
                    if (exiled) { //fade out exiled cards
                        g.setAlphaComposite(EXILED_ALPHA_COMPOSITE);
                    }
                    CardRenderer.drawCardListItem(g, font, foreColor, value.getKey(), isInfinite() ? 0 : value.getValue(), getItemSuffix(value), x, y, w, h, compactModeHandler.isCompactMode());
                    if (exiled) {
                        g.setAlphaComposite(EXILE_ICON_ALPHA_COMPOSITE);

                        //also draw exile icon on top of card art
                        float artX = x - FList.PADDING;
                        float artY = y - FList.PADDING;
                        float cardArtHeight = h + 2 * FList.PADDING;
                        float cardArtWidth = cardArtHeight * CardRenderer.CARD_ART_RATIO;
                        float iconSize = cardArtHeight / 2;
                        g.drawImage(FSkinImage.EXILE, artX + (cardArtWidth - iconSize) / 2, artY + (cardArtHeight - iconSize) / 2, iconSize, iconSize);

                        g.resetAlphaComposite();
                    }
                }

                @Override
                public boolean tap(Integer index, Entry<PaperCard, Integer> value, float x, float y, int count) {
                    return CardRenderer.cardListItemTap(model.getOrderedList(), index, CollectionManager.this, x, y, count, compactModeHandler.isCompactMode());
                }

                @Override
                public boolean longPress(Integer index, Entry<PaperCard, Integer> value, float x, float y) {
                    CardZoom.show(model.getOrderedList(), index, CollectionManager.this);
                    return true;
                }
            };
        }
    }

    private static class CardOriginFilter extends ComboBoxFilter<PaperCard, ConquestPlane> {
        public CardOriginFilter(ItemManager<? super PaperCard> itemManager0) {
            super("All Planes", FModel.getPlanes(), itemManager0);
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
