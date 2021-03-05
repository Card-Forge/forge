package forge.itemmanager;

import java.util.Map.Entry;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Align;
import com.google.common.base.Function;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.ImageCache;
import forge.card.CardRenderer;
import forge.gamemodes.quest.QuestSpellShop;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.filters.AdvancedSearchFilter;
import forge.itemmanager.filters.TextSearchFilter;
import forge.toolbox.FList;
import forge.toolbox.FList.CompactModeHandler;
import forge.util.Localizer;


public class SpellShopManager extends ItemManager<InventoryItem> {
    private final Function<Entry<? extends InventoryItem, Integer>, Object> fnGetPrice;

    public SpellShopManager(boolean isShop0) {
        super(InventoryItem.class, false);

        fnGetPrice = isShop0 ? QuestSpellShop.fnPriceGet : QuestSpellShop.fnPriceSellGet;
        if (!isShop0) {
            setCaption(Localizer.getInstance().getMessage("lblCards"));
        }
    }

    @Override
    protected void addDefaultFilters() {
        CardManager.addDefaultFilters(this);
    }

    @Override
    protected TextSearchFilter<? extends InventoryItem> createSearchFilter() {
        return CardManager.createSearchFilter(this);
    }

    @Override
    protected AdvancedSearchFilter<? extends InventoryItem> createAdvancedSearchFilter() {
        return CardManager.createAdvancedSearchFilter(this);
    }

    @Override
    public ItemRenderer getListItemRenderer(final CompactModeHandler compactModeHandler) {
        return new ItemRenderer() {
            @Override
            public float getItemHeight() {
                return CardRenderer.getCardListItemHeight(compactModeHandler.isCompactMode());
            }

            @Override
            public void drawValue(Graphics g, Entry<InventoryItem, Integer> value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                if (value.getValue() == null) { return; } //prevent crash after item removed on different thread

                float totalHeight = h + 2 * FList.PADDING;
                float cardArtWidth = totalHeight * CardRenderer.CARD_ART_RATIO;

                if (value.getKey() instanceof PaperCard) {
                    CardRenderer.drawCardListItem(g, font, foreColor, (PaperCard)value.getKey(), value.getValue(), getItemSuffix(value), x, y, w, h, compactModeHandler.isCompactMode());
                }
                else {
                    g.drawText(value.getValue().toString() + " " + value.getKey().toString(), font, foreColor, x + cardArtWidth, y, w - cardArtWidth, h, false, Align.left, true);
                    Texture image = ImageCache.getImage(value.getKey());
                    if (image != null) {
                        float imageRatio = (float)image.getWidth() / (float)image.getHeight();
                        float imageHeight = totalHeight;
                        float imageWidth = imageHeight * imageRatio;
                        if (imageWidth > cardArtWidth) {
                            imageWidth = cardArtWidth;
                            imageHeight = imageWidth / imageRatio;
                        }
                        g.drawImage(image, x - FList.PADDING + (cardArtWidth - imageWidth) / 2, y - FList.PADDING + (totalHeight - imageHeight) / 2, imageWidth, imageHeight);
                    }
                }

                //render price on top of card art
                float priceHeight = font.getLineHeight();
                y += totalHeight - priceHeight - FList.PADDING;
                g.fillRect(backColor, x - FList.PADDING, y, cardArtWidth, priceHeight);
                g.drawImage(FSkinImage.QUEST_COINSTACK, x, y, priceHeight, priceHeight);
                float offset = priceHeight * 1.1f;
                g.drawText(fnGetPrice.apply(value).toString(), font, foreColor, x + offset, y, cardArtWidth - offset - 2 * FList.PADDING, priceHeight, false, Align.left, true);
            }

            @Override
            public boolean tap(Integer index, Entry<InventoryItem, Integer> value, float x, float y, int count) {
                if (value.getKey() instanceof PaperCard) {
                    return CardRenderer.cardListItemTap(model.getOrderedList(), index, SpellShopManager.this, x, y, count, compactModeHandler.isCompactMode());
                }
                return false;
            }

            @Override
            public boolean longPress(Integer index, Entry<InventoryItem, Integer> value, float x, float y) {
                if (value.getKey() instanceof PaperCard && CardRenderer.cardListItemTap(model.getOrderedList(), index, SpellShopManager.this, x, y, 1, compactModeHandler.isCompactMode())) {
                    return true; //avoid calling toggleMultiSelectMode if user long presses on card art
                }
                toggleMultiSelectMode(index);
                return true;
            }

            @Override
            public boolean allowPressEffect(FList<Entry<InventoryItem, Integer>> list, float x, float y) {
                Entry<InventoryItem, Integer> value = list.getItemAtPoint(x, y);
                if (value != null && value.getKey() instanceof PaperCard) {
                    //only allow press effect for cards if right of card art
                    return x > CardRenderer.getCardListItemHeight(compactModeHandler.isCompactMode()) * CardRenderer.CARD_ART_RATIO;
                }
                return true;
            }
        };
    }
}
