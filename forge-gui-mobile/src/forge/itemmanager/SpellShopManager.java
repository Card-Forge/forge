package forge.itemmanager;

import java.util.Map.Entry;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.item.InventoryItem;
import forge.itemmanager.filters.ItemFilter;
import forge.itemmanager.views.ItemListView.ItemRenderer;
import forge.menu.FPopupMenu;


public final class SpellShopManager extends ItemManager<InventoryItem> {
    public SpellShopManager(boolean wantUnique0) {
        super(InventoryItem.class, wantUnique0);
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
    public ItemRenderer<InventoryItem> getListItemRenderer() {
        return new ItemRenderer<InventoryItem>() {
            @Override
            public float getItemHeight() {
                return 0;
            }

            @Override
            public void drawValue(Graphics g, Entry<InventoryItem, Integer> value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
                
            }

            @Override
            public boolean tap(Entry<InventoryItem, Integer> value, float x, float y, int count) {
                return false;
            }
        };
    }
}
