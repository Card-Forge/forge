package forge.itemmanager.filters;

import forge.deck.DeckProxy;
import forge.itemmanager.ItemManager;

import java.util.function.Predicate;


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
            return x -> true;
        }
        return input -> format.isDeckLegal(input.getDeck());
    }
}
