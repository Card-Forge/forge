package forge.itemmanager.filters;

import com.google.common.base.Predicate;

import forge.deck.DeckProxy;
import forge.itemmanager.ItemManager;

import java.util.HashSet;
import java.util.Set;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckFolderFilter extends ListLabelFilter<DeckProxy> {
    protected final Set<String> folders = new HashSet<String>();

    public DeckFolderFilter(ItemManager<? super DeckProxy> itemManager0) {
        super(itemManager0);
    }

    public DeckFolderFilter(ItemManager<? super DeckProxy> itemManager0, String folder0) {
        super(itemManager0);
        this.folders.add(folder0);
    }

    @Override
    public ItemFilter<DeckProxy> createCopy() {
        DeckFolderFilter copy = new DeckFolderFilter(itemManager);
        copy.folders.addAll(this.folders);
        return copy;
    }

    @Override
    protected final Predicate<DeckProxy> buildPredicate() {
        return new Predicate<DeckProxy>() {
            @Override
            public boolean apply(DeckProxy input) {
                String path = input.getPath();
                for (String folder : folders) {
                    if (path.startsWith(folder)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    @Override
    protected String getCaption() {
        return "Folder";
    }

    @Override
    protected Iterable<String> getList() {
        return this.folders;
    }

    @Override
    protected String getTooltip() {
        return null;
    }

    @Override
    protected int getCount() {
        return this.folders.size();
    }

    @Override
    public void reset() {
        this.folders.clear();
        this.updateLabel();
    }

    @Override
    public boolean merge(ItemFilter<?> filter) {
        DeckFolderFilter formatFilter = (DeckFolderFilter)filter;
        this.folders.addAll(formatFilter.folders);
        return true;
    }
}
