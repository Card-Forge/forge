package forge.itemmanager.filters;

import forge.game.GameFormat;
import forge.itemmanager.ItemManager;
import forge.deck.DeckProxy;
import forge.model.FModel;
import forge.quest.QuestWorld;

import java.util.HashSet;
import java.util.Set;


public class DeckQuestWorldFilter extends DeckFormatFilter {
    private final Set<QuestWorld> questWorlds = new HashSet<QuestWorld>();

    public DeckQuestWorldFilter(ItemManager<? super DeckProxy> itemManager0) {
        super(itemManager0);
    }
    public DeckQuestWorldFilter(ItemManager<? super DeckProxy> itemManager0, QuestWorld questWorld0) {
        super(itemManager0);
        this.questWorlds.add(questWorld0);
        this.formats.add(getQuestWorldFormat(questWorld0));
    }

    @Override
    public ItemFilter<DeckProxy> createCopy() {
        DeckQuestWorldFilter copy = new DeckQuestWorldFilter(itemManager);
        copy.questWorlds.addAll(this.questWorlds);
        for (QuestWorld w : this.questWorlds) {
            copy.formats.add(getQuestWorldFormat(w));
        }
        return copy;
    }

    @Override
    public void reset() {
        this.questWorlds.clear();
        super.reset();
    }

    public static boolean canAddQuestWorld(QuestWorld questWorld, ItemFilter<DeckProxy> existingFilter) {
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
    public boolean merge(ItemFilter<?> filter) {
        DeckQuestWorldFilter cardQuestWorldFilter = (DeckQuestWorldFilter)filter;
        this.questWorlds.addAll(cardQuestWorldFilter.questWorlds);
        for (QuestWorld w : cardQuestWorldFilter.questWorlds) {
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
        Set<String> strings = new HashSet<String>();
        for (QuestWorld w : this.questWorlds) {
            strings.add(w.getName());
        }
        return strings;
    }

    private GameFormat getQuestWorldFormat(QuestWorld w) {
        GameFormat format = w.getFormat();
        if (format == null) {
            //assumes that no world other than the main world will have a null format
            format = FModel.getQuest().getMainFormat();
        }
        return format;
    }
}
