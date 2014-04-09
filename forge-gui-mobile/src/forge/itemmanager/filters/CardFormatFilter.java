package forge.itemmanager.filters;

import com.google.common.base.Predicate;

import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;


public class CardFormatFilter extends FormatFilter<PaperCard> {
    public CardFormatFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }
    public CardFormatFilter(ItemManager<? super PaperCard> itemManager0, GameFormat format0) {
        super(itemManager0, format0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        CardFormatFilter copy = new CardFormatFilter(itemManager);
        copy.formats.addAll(this.formats);
        return copy;
    }

    @Override
    protected final Predicate<PaperCard> buildPredicate() {
        return SFilterUtil.buildFormatFilter(this.formats, this.allowReprints);
    }
}
