package forge.gui.toolbox.itemmanager.filters;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;

import forge.Singletons;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.PaperCard;
import forge.quest.QuestWorld;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardQuestWorldFilter extends ListLabelFilter<PaperCard> {
    private final Set<QuestWorld> questWorlds = new HashSet<QuestWorld>();

    public CardQuestWorldFilter(ItemManager<PaperCard> itemManager0, QuestWorld questWorld0) {
        super(itemManager0);
        this.questWorlds.add(questWorld0);
    }
    
    public static boolean canAddQuestWorld(QuestWorld questWorld, ItemFilter<PaperCard> existingFilter) {
        if (questWorld.getFormat() == null && Singletons.getModel().getQuest().getMainFormat() == null) {
            return false; //must have format
        }
        return existingFilter == null || !((CardQuestWorldFilter)existingFilter).questWorlds.contains(questWorld);
    }
    
    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @Override
    @SuppressWarnings("rawtypes")
    public boolean merge(ItemFilter filter) {
        CardQuestWorldFilter cardQuestWorldFilter = (CardQuestWorldFilter)filter;
        this.questWorlds.addAll(cardQuestWorldFilter.questWorlds);
        return true;
    }

    @Override
    protected void buildPanel(JPanel panel) {
        
    }

    @Override
    protected void onRemoved() {
        
    }
    
    @Override
    protected Iterable<String> getList() {
        Set<String> strings = new HashSet<String>();
        for (QuestWorld w : this.questWorlds) {
            strings.add(w.getName());
        }
        return strings;
    }
}
