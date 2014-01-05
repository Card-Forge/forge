package forge.gui.toolbox.itemmanager.filters;

import com.google.common.base.Predicate;
import forge.game.GameFormat;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SFilterUtil;
import forge.item.DeckBox;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckFormatFilter extends FormatFilter<DeckBox> {
    public DeckFormatFilter(ItemManager<? super DeckBox> itemManager0) {
        super(itemManager0);
    }
    public DeckFormatFilter(ItemManager<? super DeckBox> itemManager0, GameFormat format0) {
        super(itemManager0, format0);
    }

    @Override
    public ItemFilter<DeckBox> createCopy() {
        DeckFormatFilter copy = new DeckFormatFilter(itemManager);
        copy.formats.addAll(this.formats);
        return copy;
    }

    @Override
    protected final Predicate<DeckBox> buildPredicate() {
        return DeckBox.createPredicate(SFilterUtil.buildFormatFilter(this.formats, this.allowReprints));
    }
}
