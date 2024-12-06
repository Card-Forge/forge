package forge.itemmanager.filters;

import forge.item.PaperCard;
import forge.itemmanager.ItemManager;

import java.util.function.Predicate;


public class CardFormatFilter extends FormatFilter<PaperCard> {
    public CardFormatFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        CardFormatFilter copy = new CardFormatFilter(itemManager);
        copy.format = this.format;
        return copy;
    }

    @Override
    protected final Predicate<PaperCard> buildPredicate() {
        if (format == null) {
            return x -> true;
        }
        if (format.getName() == null) {
            return format.getFilterPrinted(); //if format is collection of sets, don't show reprints in other sets
        }
        return format.getFilterRules();
    }
}
