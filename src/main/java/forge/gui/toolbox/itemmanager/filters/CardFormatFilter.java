package forge.gui.toolbox.itemmanager.filters;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;

import forge.game.GameFormat;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardFormatFilter extends ListLabelFilter<PaperCard> {
    private final Set<GameFormat> formats = new HashSet<GameFormat>();

    public CardFormatFilter(ItemManager<PaperCard> itemManager0, GameFormat format0) {
        super(itemManager0);
        this.formats.add(format0);
    }
    
    public static boolean canAddFormat(GameFormat format, ItemFilter<PaperCard> existingFilter) {
        return existingFilter == null || !((CardFormatFilter)existingFilter).formats.contains(format);
    }
    
    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @Override
    @SuppressWarnings("rawtypes")
    public boolean merge(ItemFilter filter) {
        CardFormatFilter cardFormatFilter = (CardFormatFilter)filter;
        this.formats.addAll(cardFormatFilter.formats);
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
        for (GameFormat f : this.formats) {
            strings.add(f.getName());
        }
        return strings;
    }
}
