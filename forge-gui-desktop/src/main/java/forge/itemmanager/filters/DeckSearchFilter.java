package forge.itemmanager.filters;

import forge.deck.DeckProxy;
import forge.itemmanager.ItemManager;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckSearchFilter extends TextSearchFilter<DeckProxy> {
    public DeckSearchFilter(ItemManager<? super DeckProxy> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<DeckProxy> createCopy() {
        DeckSearchFilter copy = new DeckSearchFilter(itemManager);
        copy.getWidget(); //initialize widget
        copy.txtSearch.setText(this.txtSearch.getText());
        return copy;
    }
}
