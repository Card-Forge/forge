package forge.itemmanager.filters;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.screens.home.quest.DialogChooseSets;

/**
 * TODO: Write javadoc for this type.
 *
 */
public class CardSetFilter extends CardFormatFilter {
    protected final Set<String> sets = new HashSet<>();
    protected final Set<String> limitedSets = new HashSet<>();

    public CardSetFilter(ItemManager<? super PaperCard> itemManager0, Collection<String> sets0, boolean allowReprints0) {
        super(itemManager0);
        this.sets.addAll(sets0);
        this.formats.add(new GameFormat(null, this.sets, null));
        this.allowReprints = allowReprints0;
    }

    public CardSetFilter(ItemManager<? super PaperCard> itemManager0, Collection<String> sets0, Collection<String> limitedSets0, boolean allowReprints0){
        this(itemManager0, sets0, allowReprints0);
        if (limitedSets0 != null) {
            this.limitedSets.addAll(limitedSets0);
        }
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardSetFilter(itemManager, this.sets, this.limitedSets, this.allowReprints);
    }

    @Override
    public void reset() {
        this.sets.clear();
        super.reset();
    }

    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @Override
    public boolean merge(ItemFilter<?> filter) {
        CardSetFilter cardSetFilter = (CardSetFilter)filter;
        this.sets.addAll(cardSetFilter.sets);
        this.limitedSets.addAll(cardSetFilter.limitedSets);
        this.allowReprints = cardSetFilter.allowReprints;
        this.formats.clear();
        this.formats.add(new GameFormat(null, this.sets, null));
        return true;
    }

    public void edit(final ItemManager<? super PaperCard> itemManager) {
        final DialogChooseSets dialog = new DialogChooseSets(this.sets, null, this.limitedSets,
                                                            true, this.allowReprints);
        final CardSetFilter itemFilter = this;

        dialog.setOkCallback(() -> {
            sets.clear();
            sets.addAll(dialog.getSelectedSets());
            allowReprints = dialog.getWantReprints();
            itemManager.addFilter(itemFilter); // this adds/updates the current filter
        });
    }

    @Override
    protected String getCaption() {
        return "Set";
    }

    @Override
    protected int getCount() {
        return this.sets.size();
    }

    @Override
    protected Iterable<String> getList() {
        return this.sets;
    }
}
