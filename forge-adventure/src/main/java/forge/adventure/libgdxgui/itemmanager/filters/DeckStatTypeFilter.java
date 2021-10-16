package forge.adventure.libgdxgui.itemmanager.filters;

import forge.deck.DeckProxy;
import forge.item.InventoryItem;
import forge.adventure.libgdxgui.itemmanager.ItemManager;

public abstract class DeckStatTypeFilter extends StatTypeFilter<DeckProxy> {
    public DeckStatTypeFilter(ItemManager<? super DeckProxy> itemManager0) {
        super(itemManager0);
    }

    @Override
    protected <U extends InventoryItem> boolean showUnsupportedItem(U item) {
        return false;
    }
}
