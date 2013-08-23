package forge.gui.toolbox.itemmanager.filters;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;

import forge.gui.home.quest.DialogChooseSets;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardSetFilter extends ListLabelFilter<PaperCard> {
    private final Set<String> sets = new HashSet<String>();

    public CardSetFilter(ItemManager<PaperCard> itemManager0, Collection<String> sets) {
        super(itemManager0);
        this.sets.addAll(sets);
    }

    @Override
    protected String getTitle() {
        return "Card Set";
    }
    
    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @Override
    @SuppressWarnings("rawtypes")
    public boolean merge(ItemFilter filter) {
        CardSetFilter cardSetFilter = (CardSetFilter)filter;
        this.sets.addAll(cardSetFilter.sets);
        return true;
    }
    
    public void edit() {
        final DialogChooseSets dialog = new DialogChooseSets(this.sets, null, true);
        dialog.setOkCallback(new Runnable() {
            @Override
            public void run() {
                sets.clear();
                sets.addAll(dialog.getSelectedSets());
            }
        });
    }

    @Override
    protected void buildPanel(JPanel panel) {
        
    }

    @Override
    protected void onRemoved() {
        
    }

    @Override
    protected Iterable<String> getList() {
        return this.sets;
    }
}
