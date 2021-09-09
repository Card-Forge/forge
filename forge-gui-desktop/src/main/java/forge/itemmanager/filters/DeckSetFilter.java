package forge.itemmanager.filters;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Predicate;
import forge.deck.DeckProxy;
import forge.game.GameFormat;
import forge.itemmanager.ItemManager;
import forge.screens.home.quest.DialogChooseSets;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;


public class DeckSetFilter extends DeckFormatFilter {
    protected final Set<String> sets = new HashSet<>();
    protected final Set<String> limitedSets = new HashSet<>(); // Set of all sets constrained by catalog

    public DeckSetFilter(ItemManager<? super DeckProxy> itemManager0, Collection<String> sets0, boolean allowReprints0) {
        super(itemManager0);
        this.sets.addAll(sets0);
        this.formats.add(new GameFormat(null, this.sets, null));
        this.allowReprints = allowReprints0;
    }

    public DeckSetFilter(ItemManager<? super DeckProxy> itemManager0, Collection<String> sets0,
                         Collection<String> limitedSets0, boolean allowReprints0) {
        this(itemManager0, sets0, allowReprints0);
        this.limitedSets.addAll(limitedSets0);
    }

    @Override
    public ItemFilter<DeckProxy> createCopy() {
        return new DeckSetFilter(itemManager, this.sets, this.allowReprints);
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
        DeckSetFilter cardSetFilter = (DeckSetFilter)filter;
        this.sets.addAll(cardSetFilter.sets);
        this.limitedSets.addAll(cardSetFilter.limitedSets);
        this.allowReprints = cardSetFilter.allowReprints;
        this.formats.clear();
        this.formats.add(new GameFormat(null, this.sets, null));
        return true;
    }

    public void edit() {
        final DialogChooseSets dialog = new DialogChooseSets(this.sets, null, this.limitedSets,
                                                            true, this.allowReprints);
        final DeckSetFilter itemFilter = this;

        dialog.setOkCallback(new Runnable() {
            @Override
            public void run() {
                sets.clear();
                sets.addAll(dialog.getSelectedSets());
                allowReprints = dialog.getWantReprints();
                formats.clear();
                formats.add(new GameFormat(null, sets, null));
                itemManager.addFilter(itemFilter); // this adds/updates the current filter
            }
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

    @Override
    protected Predicate<DeckProxy> buildPredicate() {
        return new Predicate<DeckProxy>() {
            @Override
            public boolean apply(@NullableDecl DeckProxy input) {
                return input != null && sets.contains(input.getEdition().getCode());
            }
        };
    }
}
