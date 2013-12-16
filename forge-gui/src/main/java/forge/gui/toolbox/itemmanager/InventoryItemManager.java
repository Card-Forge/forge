package forge.gui.toolbox.itemmanager;

import javax.swing.JPopupMenu;

import forge.gui.toolbox.itemmanager.filters.ItemFilter;
import forge.item.InventoryItem;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public final class InventoryItemManager extends ItemManager<InventoryItem> {
    public InventoryItemManager(boolean wantUnique0) {
        super(InventoryItem.class, wantUnique0);
    }

    @Override
    protected ItemFilter<InventoryItem> createSearchFilter() {
        return null;
    }

    @Override
    protected void buildFilterMenu(JPopupMenu menu) {
        
    }
}
