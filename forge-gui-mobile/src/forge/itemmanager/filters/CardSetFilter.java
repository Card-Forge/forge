package forge.itemmanager.filters;

import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class CardSetFilter extends CardFormatFilter {
    private final Set<String> sets = new HashSet<String>();

    public CardSetFilter(ItemManager<? super PaperCard> itemManager0, Collection<String> sets0) {
        super(itemManager0);
        this.sets.addAll(sets0);
        this.formats.add(new GameFormat(null, this.sets, null));
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardSetFilter(itemManager, this.sets);
    }

    @Override
    public void reset() {
        this.sets.clear();
        super.reset();
    }
}
