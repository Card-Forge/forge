package forge.itemmanager;

import java.util.Map.Entry;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.google.common.base.Function;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.filters.ItemFilter;
import forge.menu.FPopupMenu;
import forge.quest.QuestSpellShop;
import forge.toolbox.FList;


public final class SpellShopManager extends ItemManager<InventoryItem> {
    private final Function<Entry<? extends InventoryItem, Integer>, Object> fnGetPrice;

    public SpellShopManager(boolean isShop0) {
        super(InventoryItem.class, false);

        fnGetPrice = isShop0 ? QuestSpellShop.fnPriceGet : QuestSpellShop.fnPriceSellGet;
        if (!isShop0) {
            setCaption("Cards");
        }
    }

    @Override
    protected void addDefaultFilters() {
        CardManager.addDefaultFilters(this);
    }

    @Override
    protected ItemFilter<? extends InventoryItem> createSearchFilter() {
        return CardManager.createSearchFilter(this);
    }

    @Override
    protected void buildAddFilterMenu(FPopupMenu menu) {
        CardManager.buildAddFilterMenu(menu, this);
    }

    @Override
    public ItemRenderer getListItemRenderer() {
        return new ItemRenderer() {
            @Override
            public float getItemHeight() {
                return CardRenderer.getCardListItemHeight();
            }

            @Override
            public void drawValue(Graphics g, Entry<InventoryItem, Integer> value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                if (value.getKey() instanceof PaperCard) {
                    CardRenderer.drawCardListItem(g, font, foreColor, (PaperCard)value.getKey(), value.getValue(), x, y, w, h);
                }
                //TODO: render list item for non-card item

                //render price on top of card art
                h += 2 * FList.PADDING;
                float cardArtWidth = h * CardRenderer.CARD_ART_RATIO;
                float priceHeight = font.getLineHeight();
                y += h - priceHeight - FList.PADDING;
                g.fillRect(backColor, x - FList.PADDING, y, cardArtWidth, priceHeight);
                g.drawImage(FSkinImage.QUEST_COINSTACK, x, y, priceHeight, priceHeight);
                float offset = priceHeight * 1.1f;
                g.drawText(fnGetPrice.apply(value).toString(), font, foreColor, x + offset, y, cardArtWidth - offset - 2 * FList.PADDING, priceHeight, false, HAlignment.LEFT, true);
            }

            @Override
            public boolean tap(Entry<InventoryItem, Integer> value, float x, float y, int count) {
                if (value.getKey() instanceof PaperCard) {
                    return CardRenderer.cardListItemTap((PaperCard)value.getKey(), x, y, count);
                }
                return false;
            }

            @Override
            public boolean longPress(Entry<InventoryItem, Integer> value, float x, float y) {
                if (value.getKey() instanceof PaperCard) {
                    CardZoom.show((PaperCard)value.getKey());
                    return true;
                }
                return false;
            }
        };
    }
}
