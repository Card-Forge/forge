package forge.itemmanager.filters;

import java.util.List;

import com.google.common.base.Predicate;

import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.screens.home.quest.DialogChooseFormats;

public class CardFormatFilter extends FormatFilter<PaperCard> {
    public CardFormatFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    public CardFormatFilter(ItemManager<? super PaperCard> itemManager0, GameFormat format0) {
        super(itemManager0, format0);
    }

    public CardFormatFilter(ItemManager<? super PaperCard> itemManager0, List<GameFormat> formats0,boolean allowReprints0) {
        super(itemManager0);
        this.formats.addAll(formats0);
        this.allowReprints = allowReprints0;
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

    public void edit(final ItemManager<? super PaperCard> itemManager) {
        final DialogChooseFormats dialog = new DialogChooseFormats(this.formats);
        final CardFormatFilter itemFilter = this;
        dialog.setWantReprintsCB(allowReprints);

        dialog.setOkCallback(new Runnable() {
            @Override
            public void run() {
                formats.clear();
                formats.addAll(dialog.getSelectedFormats());
                allowReprints = dialog.getWantReprints();
                itemManager.addFilter(itemFilter); // this adds/updates the current filter...
            }
        });
    }
}
