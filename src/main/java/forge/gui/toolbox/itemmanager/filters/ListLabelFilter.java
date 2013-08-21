package forge.gui.toolbox.itemmanager.filters;

import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.InventoryItem;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class ListLabelFilter<T extends InventoryItem> extends ItemFilter<T> {

    protected ListLabelFilter(ItemManager<T> itemManager0) {
        super(itemManager0);
    }
    
    protected abstract Iterable<String> getList();
    
    public void buildPanel() {
        StringBuilder label = new StringBuilder();
        boolean truncated = false;
        for (String str : getList()) {
            // don't let the full label get too long
            if (label.length() < 32) {
                label.append(" ").append(str).append(";");
            } else {
                truncated = true;
                break;
            }
        }
        
        // chop off last semicolons
        label.delete(label.length() - 1, label.length());
        
        if (truncated) {
            label.append("...");
        }
    }
}
