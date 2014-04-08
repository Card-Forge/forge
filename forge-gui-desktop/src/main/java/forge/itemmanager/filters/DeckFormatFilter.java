package forge.itemmanager.filters;

import com.google.common.base.Predicate;

import forge.game.GameFormat;
import forge.deck.DeckProxy;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;


public class DeckFormatFilter extends FormatFilter<DeckProxy> {
    public DeckFormatFilter(ItemManager<? super DeckProxy> itemManager0) {
        super(itemManager0);
    }
    public DeckFormatFilter(ItemManager<? super DeckProxy> itemManager0, GameFormat format0) {
        super(itemManager0, format0);
    }

    @Override
    public ItemFilter<DeckProxy> createCopy() {
        DeckFormatFilter copy = new DeckFormatFilter(itemManager);
        copy.formats.addAll(this.formats);
        return copy;
    }

    @Override
    protected final Predicate<DeckProxy> buildPredicate() {
        return DeckProxy.createPredicate(SFilterUtil.buildFormatFilter(this.formats, this.allowReprints));
    }
}
