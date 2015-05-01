package forge.itemmanager.filters;

import java.util.HashSet;
import java.util.Set;

import forge.deck.DeckProxy;
import forge.game.GameFormat;
import forge.itemmanager.ItemManager;
import forge.model.FModel;
import forge.quest.QuestWorld;


public class DeckQuestWorldFilter extends DeckFormatFilter {
    private final Set<QuestWorld> questWorlds = new HashSet<QuestWorld>();

    public DeckQuestWorldFilter(final ItemManager<? super DeckProxy> itemManager0) {
        super(itemManager0);
    }
    public DeckQuestWorldFilter(final ItemManager<? super DeckProxy> itemManager0, final QuestWorld questWorld0) {
        super(itemManager0);
        this.questWorlds.add(questWorld0);
        this.formats.add(getQuestWorldFormat(questWorld0));
    }

    @Override
    public ItemFilter<DeckProxy> createCopy() {
        final DeckQuestWorldFilter copy = new DeckQuestWorldFilter(itemManager);
        copy.questWorlds.addAll(this.questWorlds);
        for (final QuestWorld w : this.questWorlds) {
            copy.formats.add(getQuestWorldFormat(w));
        }
        return copy;
    }

    @Override
    public void reset() {
        this.questWorlds.clear();
        super.reset();
    }

    public static boolean canAddQuestWorld(final QuestWorld questWorld, final ItemFilter<DeckProxy> existingFilter) {
        if (questWorld.getFormat() == null && FModel.getQuest().getMainFormat() == null) {
            return false; //must have format
        }
        return existingFilter == null || !((DeckQuestWorldFilter)existingFilter).questWorlds.contains(questWorld);
    }

    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @Override
    public boolean merge(final ItemFilter<?> filter) {
        final DeckQuestWorldFilter cardQuestWorldFilter = (DeckQuestWorldFilter)filter;
        this.questWorlds.addAll(cardQuestWorldFilter.questWorlds);
        for (final QuestWorld w : cardQuestWorldFilter.questWorlds) {
            this.formats.add(getQuestWorldFormat(w));
        }
        return true;
    }

    @Override
    protected String getCaption() {
        return "Quest World";
    }

    @Override
    protected int getCount() {
        return this.questWorlds.size();
    }

    @Override
    protected Iterable<String> getList() {
        final Set<String> strings = new HashSet<String>();
        for (final QuestWorld w : this.questWorlds) {
            strings.add(w.getName());
        }
        return strings;
    }

    private static GameFormat getQuestWorldFormat(final QuestWorld w) {
        GameFormat format = w.getFormat();
        if (format == null) {
            //assumes that no world other than the main world will have a null format
            format = FModel.getQuest().getMainFormat();
        }
        return format;
    }
}
