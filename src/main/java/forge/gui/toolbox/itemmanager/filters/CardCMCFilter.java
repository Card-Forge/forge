package forge.gui.toolbox.itemmanager.filters;

import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class CardCMCFilter extends ValueRangeFilter<PaperCard> {

    public CardCMCFilter(ItemManager<PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public FilterTypes getType() {
        return FilterTypes.CardCMC;
    }

    @Override
    protected void addComponents() {
        
    }

    @Override
    protected void onRemoved() {
        
    }
}
