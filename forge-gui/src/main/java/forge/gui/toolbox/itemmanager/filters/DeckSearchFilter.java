package forge.gui.toolbox.itemmanager.filters;

import forge.deck.Deck;
import forge.gui.toolbox.itemmanager.ItemManager;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckSearchFilter extends TextSearchFilter<Deck> {
    public DeckSearchFilter(ItemManager<? super Deck> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<Deck> createCopy() {
        DeckSearchFilter copy = new DeckSearchFilter(itemManager);
        copy.getWidget(); //initialize widget
        copy.txtSearch.setText(this.txtSearch.getText());
        return copy;
    }
}
