package forge.gui.toolbox.itemmanager.filters;

import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.InventoryItem;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public abstract class ValueRangeFilter<T extends InventoryItem> extends ItemFilter<T> {

    protected ValueRangeFilter(ItemManager<T> itemManager0) {
        super(itemManager0);
    }
}
