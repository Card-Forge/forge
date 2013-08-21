package forge.gui.toolbox.itemmanager.filters;

import javax.swing.JPanel;

import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardPowerFilter extends ValueRangeFilter<PaperCard> {
    public CardPowerFilter(ItemManager<PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    protected void buildPanel(JPanel panel) {
        
    }

    @Override
    protected void onRemoved() {
        
    }
}
