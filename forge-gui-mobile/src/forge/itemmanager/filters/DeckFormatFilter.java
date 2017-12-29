package forge.itemmanager.filters;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.deck.DeckProxy;
import forge.itemmanager.ItemManager;


public class DeckFormatFilter extends FormatFilter<DeckProxy> {
    public DeckFormatFilter(ItemManager<? super DeckProxy> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<DeckProxy> createCopy() {
        DeckFormatFilter copy = new DeckFormatFilter(itemManager);
        copy.format = format;
        return copy;
    }

    @Override
    protected final Predicate<DeckProxy> buildPredicate() {
        if (format == null) {
            return Predicates.alwaysTrue();
        }
        return new Predicate<DeckProxy>() {
            @Override
            public boolean apply(DeckProxy input) {
                return format.isDeckLegal(input.getDeck());
            }
        };
    }
}
