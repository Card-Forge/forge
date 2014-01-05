package forge.gui.toolbox.itemmanager.filters;

import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.DeckBox;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckSearchFilter extends TextSearchFilter<DeckBox> {
    public DeckSearchFilter(ItemManager<? super DeckBox> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<DeckBox> createCopy() {
        DeckSearchFilter copy = new DeckSearchFilter(itemManager);
        copy.getWidget(); //initialize widget
        copy.txtSearch.setText(this.txtSearch.getText());
        return copy;
    }
}
