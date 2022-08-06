package forge.itemmanager.filters;

import com.google.common.base.Predicate;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.itemmanager.SItemManagerUtil.StatTypes;


public class CardColorFilter extends StatTypeFilter<PaperCard> {
    public CardColorFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardColorFilter(itemManager);
    }

    @Override
    protected void buildWidget(Widget widget) {
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
