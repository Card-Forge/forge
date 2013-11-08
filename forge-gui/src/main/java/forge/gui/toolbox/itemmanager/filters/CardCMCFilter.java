package forge.gui.toolbox.itemmanager.filters;

import javax.swing.JPanel;

import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardCMCFilter extends ValueRangeFilter<PaperCard> {

    public CardCMCFilter(ItemManager<PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    protected String getTitle() {
        return "Card CMC";
    }

    @Override
    protected void buildPanel(JPanel panel) {
        
    }

    @Override
    protected void onRemoved() {
        
    }
}
