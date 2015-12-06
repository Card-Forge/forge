package forge.screens.planarconquest;

import java.util.Map.Entry;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.google.common.base.Predicate;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.card.CardFaceSymbols;
import forge.card.CardRenderer;
import forge.card.ColorSet;
import forge.deck.FDeckChooser;
import forge.deck.FDeckViewer;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.SFilterUtil;
import forge.itemmanager.filters.AdvancedSearchFilter;
import forge.itemmanager.filters.ColorFilter;
import forge.itemmanager.filters.ComboBoxFilter;
import forge.itemmanager.filters.ItemFilter;
import forge.itemmanager.filters.TextSearchFilter;
import forge.model.FModel;
import forge.planarconquest.ConquestCommander;
import forge.planarconquest.ConquestPlane;
import forge.planarconquest.ConquestRecord;
import forge.screens.FScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FList.CompactModeHandler;

public class ConquestCommandersScreen extends FScreen {
    private final CommanderManager lstCommanders = add(new CommanderManager());
    private final FLabel lblTip = add(new FLabel.Builder()
        .text("Double-tap to edit deck (Long-press to view)")
        .textColor(FLabel.INLINE_LABEL_COLOR)
        .align(HAlignment.CENTER).font(FSkinFont.get(12)).build());

    private boolean needRefreshOnActivate = true;

    public ConquestCommandersScreen() {
        super("", ConquestMenu.getMenu());

        lstCommanders.setup(ItemManagerConfig.CONQUEST_COMMANDERS);
        lstCommanders.setItemActivateHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                editSelectedDeck();
            }
        });
    }

    @Override
    public void onActivate() {
        setHeaderCaption(FModel.getConquest().getName());

        if (needRefreshOnActivate) {
            needRefreshOnActivate = false;
            refreshCommanders();
        }
    }

    public void refreshCommanders() {
        lstCommanders.setPool(FModel.getConquest().getModel().getCommanders());
        lstCommanders.setup(ItemManagerConfig.CONQUEST_COMMANDERS);
    }

    private void editSelectedDeck() {
        final ConquestCommander commander = lstCommanders.getSelectedItem();
        if (commander == null) { return; }

        needRefreshOnActivate = true;
        Forge.openScreen(new ConquestDeckEditor(commander));
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = ItemFilter.PADDING;
        float y = startY;
        float w = width - 2 * x;
        float labelHeight = lblTip.getAutoSizeBounds().height;
        float listHeight = height - labelHeight - y - 2 * FDeckChooser.PADDING;

        lstCommanders.setBounds(x, y, w, listHeight);
        y += listHeight + FDeckChooser.PADDING;
        lblTip.setBounds(x, y, w, labelHeight);
    }

    private static class CommanderManager extends ItemManager<ConquestCommander> {
        private CommanderManager() {
            super(ConquestCommander.class, true);
            setCaption("Commanders");
        }

        @Override
        protected void addDefaultFilters() {
            addFilter(new CommanderColorFilter(this));
            addFilter(new CommanderOriginFilter(this));
        }

        @Override
        protected TextSearchFilter<? extends ConquestCommander> createSearchFilter() {
            return new TextSearchFilter<ConquestCommander>(this);
        }

        @Override
        protected AdvancedSearchFilter<? extends ConquestCommander> createAdvancedSearchFilter() {
            return new AdvancedSearchFilter<ConquestCommander>(this);
        }

        @Override
        public ItemRenderer getListItemRenderer(final CompactModeHandler compactModeHandler) {
            return new ItemRenderer() {
                @Override
                public float getItemHeight() {
                    return CardRenderer.getCardListItemHeight(compactModeHandler.isCompactMode()); //use same height for commanders as for cards
                }

                @Override
                public boolean tap(Integer index, Entry<ConquestCommander, Integer> value, float x, float y, int count) {
                    return CardRenderer.cardListItemTap(model.getOrderedList(), index, CommanderManager.this, x, y, count, compactModeHandler.isCompactMode());
                }

                @Override
                public boolean longPress(Integer index, Entry<ConquestCommander, Integer> value, float x, float y) {
                    FDeckViewer.show(value.getKey().getDeck());
                    return true;
                }

                @Override
                public void drawValue(Graphics g, Entry<ConquestCommander, Integer> value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                    ConquestCommander commander = value.getKey();
                    PaperCard card = commander.getCard();
                    ConquestRecord record = commander.getRecord();

                    //draw card art
                    FImage cardArt = CardRenderer.getCardArt(card);
                    float cardArtHeight = h + 2 * FList.PADDING;
                    float cardArtWidth = cardArtHeight * CardRenderer.CARD_ART_RATIO;
                    if (cardArt != null) {
                        g.drawImage(cardArt, x - FList.PADDING, y - FList.PADDING, cardArtWidth, cardArtHeight);
                    }

                    //draw name and color on first line
                    x += cardArtWidth;
                    float imageSize = CardRenderer.MANA_SYMBOL_SIZE;
                    ColorSet cardColor = card.getRules().getColor();
                    float availableWidth = w - cardArtWidth - CardFaceSymbols.getWidth(cardColor, imageSize) - FList.PADDING;
                    g.drawText(card.getName(), font, foreColor, x, y, availableWidth, imageSize, false, HAlignment.LEFT, true);
                    CardFaceSymbols.drawColorSet(g, cardColor, x + availableWidth + FList.PADDING, y, imageSize);

                    if (compactModeHandler.isCompactMode()) {
                        return; //skip second line if compact mode
                    }

                    //draw origin, record, and set/rarity on second line
                    font = FSkinFont.get(12);
                    float lineHeight = font.getLineHeight();

                    y += imageSize + FList.PADDING + CardRenderer.SET_BOX_MARGIN;
                    String set = card.getEdition();
                    float setWidth = CardRenderer.getSetWidth(font, set);
                    availableWidth = w - cardArtWidth - setWidth;

                    g.drawText(commander.getOrigin() + " (" + record.getWins() + "W / " + record.getLosses() + "L)", font, foreColor, x, y, availableWidth, lineHeight, false, HAlignment.LEFT, true);

                    x += availableWidth + CardRenderer.SET_BOX_MARGIN;
                    y -= CardRenderer.SET_BOX_MARGIN;
                    CardRenderer.drawSetLabel(g, font, set, card.getRarity(), x, y, setWidth, lineHeight + 2 * CardRenderer.SET_BOX_MARGIN);
                }
            };
        }
    }

    private static class CommanderColorFilter extends ColorFilter<ConquestCommander> {
        public CommanderColorFilter(ItemManager<? super ConquestCommander> itemManager0) {
            super(itemManager0);
        }

        @Override
        public ItemFilter<ConquestCommander> createCopy() {
            return new CommanderColorFilter(itemManager);
        }

        @Override
        protected final Predicate<ConquestCommander> buildPredicate() {
            return new Predicate<ConquestCommander>() {
                private final Predicate<PaperCard> pred = SFilterUtil.buildColorFilter(buttonMap);

                @Override
                public boolean apply(ConquestCommander input) {
                    return pred.apply(input.getCard());
                }
            };
        }
    }

    private static class CommanderOriginFilter extends ComboBoxFilter<ConquestCommander, ConquestPlane> {
        public CommanderOriginFilter(ItemManager<? super ConquestCommander> itemManager0) {
            super("All Planes", ConquestPlane.values(), itemManager0);
        }

        @Override
        public ItemFilter<ConquestCommander> createCopy() {
            CommanderOriginFilter copy = new CommanderOriginFilter(itemManager);
            copy.filterValue = filterValue;
            return copy;
        }

        @Override
        protected Predicate<ConquestCommander> buildPredicate() {
            return new Predicate<ConquestCommander>() {
                @Override
                public boolean apply(ConquestCommander input) {
                    if (filterValue == null) {
                        return true;
                    }
                    return input.getOriginPlane() == filterValue;
                }
            };
        }
    }
}
