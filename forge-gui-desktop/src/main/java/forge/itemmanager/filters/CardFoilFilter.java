package forge.itemmanager.filters;

import javax.swing.JPanel;

import com.google.common.base.Predicate;

import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.itemmanager.SItemManagerUtil.StatTypes;


public class CardFoilFilter extends StatTypeFilter<PaperCard> {
    public CardFoilFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardFoilFilter(itemManager);
    }

    @Override
    protected void buildWidget(JPanel widget) {
/*        if (itemManager instanceof SpellShopManager) {
            addToggleButton(widget, StatTypes.PACK_OR_DECK);
        }*/
        addToggleButton(widget, StatTypes.FOIL_OLD);
        addToggleButton(widget, StatTypes.FOIL_NEW);
        addToggleButton(widget, StatTypes.FOIL_NONE);
    }

    @Override
    protected final Predicate<PaperCard> buildPredicate() {
        return SFilterUtil.buildFoilFilter(buttonMap);
    }
}
