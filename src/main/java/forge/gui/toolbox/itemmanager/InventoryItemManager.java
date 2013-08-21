package forge.gui.toolbox.itemmanager;

import java.util.Map;

import javax.swing.JPopupMenu;

import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.itemmanager.SItemManagerUtil.StatTypes;
import forge.gui.toolbox.itemmanager.filters.ItemFilter;
import forge.item.InventoryItem;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public final class InventoryItemManager extends ItemManager<InventoryItem> {

    public InventoryItemManager(Map<StatTypes, FLabel> statLabels0, boolean wantUnique0) {
        super(InventoryItem.class, statLabels0, wantUnique0);
    }

    @Override
    protected ItemFilter<InventoryItem> createSearchFilter(String text) {
        return null;
    }

    @Override
    protected void buildFilterMenu(JPopupMenu menu) {
        
    }
}
