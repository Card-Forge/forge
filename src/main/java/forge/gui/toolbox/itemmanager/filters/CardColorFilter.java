package forge.gui.toolbox.itemmanager.filters;

import javax.swing.JPanel;

import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SItemManagerUtil.StatTypes;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardColorFilter extends ToggleButtonsFilter<PaperCard> {
    public CardColorFilter(ItemManager<PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    protected String getTitle() {
        return "Card Color";
    }

    @Override
    protected void buildPanel(JPanel panel) {
        addToggleButton(panel, StatTypes.WHITE);
        addToggleButton(panel, StatTypes.BLUE);
        addToggleButton(panel, StatTypes.BLACK);
        addToggleButton(panel, StatTypes.RED);
        addToggleButton(panel, StatTypes.GREEN);
        addToggleButton(panel, StatTypes.COLORLESS);
        addToggleButton(panel, StatTypes.MULTICOLOR);
    }

    @Override
    protected void onRemoved() {
        
    }
}
