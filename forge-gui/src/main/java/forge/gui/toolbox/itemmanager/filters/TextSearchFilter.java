package forge.gui.toolbox.itemmanager.filters;

import javax.swing.JPanel;

import forge.gui.toolbox.FTextField;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.InventoryItem;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class TextSearchFilter<T extends InventoryItem> extends ItemFilter<T> {
    private String text;

    protected TextSearchFilter(ItemManager<T> itemManager0, String text0) {
        super(itemManager0);
        this.text = text0;
    }
    
    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @Override
    @SuppressWarnings("rawtypes")
    public boolean merge(ItemFilter filter) {
        return false;
    }
    
    @Override
    protected void buildPanel(JPanel panel) {
        panel.add(new FTextField.Builder().text(this.text).build());
    }
}
