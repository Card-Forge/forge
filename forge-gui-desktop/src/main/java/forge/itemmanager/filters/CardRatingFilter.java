package forge.itemmanager.filters;

import javax.swing.JPanel;

import com.google.common.base.Predicate;

import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.itemmanager.SItemManagerUtil.StatTypes;
import forge.model.FModel;

public class CardRatingFilter extends StatTypeFilter<PaperCard> {
    public CardRatingFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardRatingFilter(itemManager);
    }

    @Override
    protected void buildWidget(JPanel widget) {
        /*if (itemManager instanceof SpellShopManager) {
            addToggleButton(widget, StatTypes.PACK_OR_DECK);
        }*/
        addToggleButton(widget, StatTypes.RATE_NONE);
        addToggleButton(widget, StatTypes.RATE_1);
        addToggleButton(widget, StatTypes.RATE_2);
        addToggleButton(widget, StatTypes.RATE_3);
        addToggleButton(widget, StatTypes.RATE_4);
        addToggleButton(widget, StatTypes.RATE_5);
    }

    @Override
    protected final Predicate<PaperCard> buildPredicate() {
        return SFilterUtil.buildStarRatingFilter(buttonMap, FModel.getQuest().GetRating());
    }
}
