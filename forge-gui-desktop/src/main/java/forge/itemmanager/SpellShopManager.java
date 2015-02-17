package forge.itemmanager;

import forge.item.InventoryItem;
import forge.itemmanager.filters.ItemFilter;
import forge.screens.match.controllers.CDetailPicture;

import javax.swing.*;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public final class SpellShopManager extends ItemManager<InventoryItem> {
    public SpellShopManager(final CDetailPicture cDetailPicture, final boolean wantUnique0) {
        super(InventoryItem.class, cDetailPicture, wantUnique0);
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
    protected void buildAddFilterMenu(JMenu menu) {
        CardManager.buildAddFilterMenu(menu, this);
    }
}
