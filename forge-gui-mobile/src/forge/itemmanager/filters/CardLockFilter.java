package forge.itemmanager.filters;

import forge.item.AdventurePaperCard;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.itemmanager.SItemManagerUtil.StatTypes;

import java.util.function.Predicate;


public class CardLockFilter extends StatTypeFilter<AdventurePaperCard> {
    public CardLockFilter(ItemManager<? super AdventurePaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<AdventurePaperCard> createCopy() {
        return new CardLockFilter(itemManager);
    }

    @Override
    protected void buildWidget(Widget widget) {
        addToggleButton(widget, StatTypes.LOCKED);
    }

    @Override
    protected final Predicate<AdventurePaperCard> buildPredicate() {
        return SFilterUtil.buildLockedFilter(buttonMap);
    }
}
