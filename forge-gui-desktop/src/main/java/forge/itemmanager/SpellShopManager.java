package forge.itemmanager;

import javax.swing.JMenu;

import forge.item.InventoryItem;
import forge.itemmanager.filters.ItemFilter;
import forge.screens.match.controllers.CDetailPicture;

/**
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public final class SpellShopManager extends ItemManager<InventoryItem> {

    private final CDetailPicture cDetailPicture2;
    private final boolean wantUnique02;

    public SpellShopManager(final CDetailPicture cDetailPicture, final boolean wantUnique0) {
        super(InventoryItem.class, cDetailPicture, wantUnique0, false);
        cDetailPicture2 = cDetailPicture;
        wantUnique02 = wantUnique0;
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
        CardManager CM = new CardManager(cDetailPicture2, wantUnique02, true, false);
        CM.buildAddFilterMenu(menu, this);
    }
}
