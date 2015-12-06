package forge.itemmanager.filters;

import com.google.common.base.Predicate;

import forge.deck.DeckProxy;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;


public class DeckColorFilter extends ColorFilter<DeckProxy> {
    public DeckColorFilter(ItemManager<? super DeckProxy> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<DeckProxy> createCopy() {
        return new DeckColorFilter(itemManager);
    }

    @Override
    protected final Predicate<DeckProxy> buildPredicate() {
        return SFilterUtil.buildDeckColorFilter(buttonMap);
    }
}
