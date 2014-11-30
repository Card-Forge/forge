package forge.itemmanager;

import java.util.Map.Entry;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.item.PaperCard;
import forge.itemmanager.filters.CardColorFilter;
import forge.itemmanager.filters.CardFormatFilter;
import forge.itemmanager.filters.CardSearchFilter;
import forge.itemmanager.filters.CardTypeFilter;
import forge.itemmanager.filters.TextSearchFilter;
import forge.toolbox.FList.CompactModeHandler;
import forge.util.Callback;

/** 
 * ItemManager for cards
 *
 */
public class CardManager extends ItemManager<PaperCard> {
    public CardManager(boolean wantUnique0) {
        super(PaperCard.class, wantUnique0);
    }

    @Override
    protected void addDefaultFilters() {
        addDefaultFilters(this);
    }

    @Override
    protected TextSearchFilter<PaperCard> createSearchFilter() {
        return createSearchFilter(this);
    }

    /* Static overrides shared with SpellShopManager*/

    public static void addDefaultFilters(final ItemManager<? super PaperCard> itemManager) {
        itemManager.addFilter(new CardColorFilter(itemManager));
        itemManager.addFilter(new CardFormatFilter(itemManager));
        itemManager.addFilter(new CardTypeFilter(itemManager));
    }

    public static TextSearchFilter<PaperCard> createSearchFilter(final ItemManager<? super PaperCard> itemManager) {
        return new CardSearchFilter(itemManager);
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
                CardRenderer.drawCardListItem(g, font, foreColor, value.getKey(), isInfinite() ? 0 : value.getValue(), getItemSuffix(value), x, y, w, h, compactModeHandler.isCompactMode());
            }

            @Override
            public boolean tap(Integer index, Entry<PaperCard, Integer> value, float x, float y, int count) {
                return CardRenderer.cardListItemTap(model.getOrderedList(), index, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        setSelectedIndex(result);
                        showMenu();
                    }
                }, x, y, count, compactModeHandler.isCompactMode());
            }

            @Override
            public boolean longPress(Integer index, Entry<PaperCard, Integer> value, float x, float y) {
                CardZoom.show(model.getOrderedList(), index, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        setSelectedIndex(result);
                        showMenu();
                    }
                });
                return true;
            }
        };
    }
}
