package forge.gui.toolbox.itemmanager.filters;

import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.InventoryItem;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class ValueRangeFilter<T extends InventoryItem> extends ItemFilter<T> {

    protected ValueRangeFilter(ItemManager<T> itemManager0) {
        super(itemManager0);
    }
    
    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @Override
    @SuppressWarnings("rawtypes")
    public boolean merge(ItemFilter filter) {
        return true;
    }
}
