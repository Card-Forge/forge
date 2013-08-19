package forge.gui.toolbox.itemmanager.filters;

import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class CardFormatFilter extends ListLabelFilter<PaperCard> {
    public CardFormatFilter(ItemManager<PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public FilterTypes getType() {
        return FilterTypes.CardFormat;
    }

    @Override
    protected void addComponents() {
        
    }

    @Override
    protected void onRemoved() {
        
    }
}
