package forge.gui.toolbox.itemmanager;

import javax.swing.JPopupMenu;

import forge.gui.toolbox.itemmanager.filters.ItemFilter;
import forge.item.InventoryItem;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
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
    protected void buildFilterMenu(JPopupMenu menu) {
        CardManager.buildFilterMenu(menu, this);
    }
}
