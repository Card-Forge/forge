package forge.gui.toolbox.itemmanager.filters;

import javax.swing.JPanel;

import com.google.common.base.Predicate;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SFilterUtil;
import forge.gui.toolbox.itemmanager.SpellShopManager;
import forge.gui.toolbox.itemmanager.SItemManagerUtil.StatTypes;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardColorFilter extends StatTypeFilter<PaperCard> {
    public CardColorFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardColorFilter(itemManager);
    }

    @Override
    protected void buildWidget(JPanel widget) {
        if (itemManager instanceof SpellShopManager) {
            addToggleButton(widget, StatTypes.PACK_OR_DECK);
        }
        addToggleButton(widget, StatTypes.WHITE);
        addToggleButton(widget, StatTypes.BLUE);
        addToggleButton(widget, StatTypes.BLACK);
        addToggleButton(widget, StatTypes.RED);
        addToggleButton(widget, StatTypes.GREEN);
        addToggleButton(widget, StatTypes.COLORLESS);
        addToggleButton(widget, StatTypes.MULTICOLOR);
    }

    @Override
    protected final Predicate<PaperCard> buildPredicate() {
        return SFilterUtil.buildColorFilter(buttonMap);
    }
}
