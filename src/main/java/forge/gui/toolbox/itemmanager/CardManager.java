package forge.gui.toolbox.itemmanager;

import java.util.Map;

import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.itemmanager.SItemManagerUtil.StatTypes;
import forge.gui.toolbox.itemmanager.filters.CardColorFilter;
import forge.gui.toolbox.itemmanager.filters.CardTypeFilter;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public final class CardManager extends ItemManager<PaperCard> {

    public CardManager(Map<StatTypes, FLabel> statLabels0, boolean wantUnique0) {
        super(PaperCard.class, statLabels0, wantUnique0);
        
        this.addFilter(new CardColorFilter(this));
        this.addFilter(new CardTypeFilter(this));
    }
}
