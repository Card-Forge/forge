package forge.gui.toolbox.itemmanager.filters;

import javax.swing.JPanel;

import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardSearchFilter extends TextSearchFilter<PaperCard> {
    public CardSearchFilter(ItemManager<PaperCard> itemManager0, String text0) {
        super(itemManager0, text0);
    }

    @Override
    protected void buildPanel(JPanel panel) {

    }

    @Override
    protected void onRemoved() {
        
    }
}
