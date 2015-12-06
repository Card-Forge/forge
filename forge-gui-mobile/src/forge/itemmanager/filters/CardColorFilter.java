package forge.itemmanager.filters;

import com.google.common.base.Predicate;

import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;


public class CardColorFilter extends ColorFilter<PaperCard> {
    public CardColorFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardColorFilter(itemManager);
    }

    @Override
    protected final Predicate<PaperCard> buildPredicate() {
        return SFilterUtil.buildColorFilter(buttonMap);
    }
}
