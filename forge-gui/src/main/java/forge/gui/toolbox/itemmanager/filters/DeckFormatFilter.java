package forge.gui.toolbox.itemmanager.filters;

import com.google.common.base.Predicate;

import forge.deck.Deck;
import forge.game.GameFormat;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SFilterUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckFormatFilter extends FormatFilter<Deck> {
    public DeckFormatFilter(ItemManager<? super Deck> itemManager0) {
        super(itemManager0);
    }
    public DeckFormatFilter(ItemManager<? super Deck> itemManager0, GameFormat format0) {
        super(itemManager0, format0);
    }

    @Override
    public ItemFilter<Deck> createCopy() {
        DeckFormatFilter copy = new DeckFormatFilter(itemManager);
        copy.formats.addAll(this.formats);
        return copy;
    }

    @Override
    protected final Predicate<Deck> buildPredicate() {
        return Deck.createPredicate(SFilterUtil.buildFormatFilter(this.formats, this.allowReprints));
    }
}
