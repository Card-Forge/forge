package forge.itemmanager.filters;

import com.google.common.base.Predicate;
import forge.deck.DeckProxy;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.itemmanager.SItemManagerUtil.StatTypes;


public class DeckColorFilter extends StatTypeFilter<DeckProxy> {
    public DeckColorFilter(ItemManager<? super DeckProxy> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<DeckProxy> createCopy() {
        return new DeckColorFilter(itemManager);
    }

    @Override
    protected void buildWidget(Widget widget) {
        addToggleButton(widget, StatTypes.DECK_WHITE);
        addToggleButton(widget, StatTypes.DECK_BLUE);
        addToggleButton(widget, StatTypes.DECK_BLACK);
        addToggleButton(widget, StatTypes.DECK_RED);
        addToggleButton(widget, StatTypes.DECK_GREEN);
        addToggleButton(widget, StatTypes.DECK_COLORLESS);
        addToggleButton(widget, StatTypes.DECK_MULTICOLOR);
    }

    @Override
    protected final Predicate<DeckProxy> buildPredicate() {
        return SFilterUtil.buildDeckColorFilter(buttonMap);
    }
}
