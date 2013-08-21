package forge.gui.toolbox.itemmanager.filters;

import javax.swing.JPanel;

import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SItemManagerUtil.StatTypes;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardTypeFilter extends ToggleButtonsFilter<PaperCard> {
    public CardTypeFilter(ItemManager<PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    protected void buildPanel(JPanel panel) {
        addToggleButton(panel, StatTypes.LAND);
        addToggleButton(panel, StatTypes.ARTIFACT);
        addToggleButton(panel, StatTypes.CREATURE);
        addToggleButton(panel, StatTypes.ENCHANTMENT);
        addToggleButton(panel, StatTypes.PLANESWALKER);
        addToggleButton(panel, StatTypes.INSTANT);
        addToggleButton(panel, StatTypes.SORCERY);
    }

    @Override
    protected void onRemoved() {
        
    }
}
