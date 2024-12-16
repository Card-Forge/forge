package forge.itemmanager.filters;

import forge.deck.DeckProxy;
import forge.game.GameFormat;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.screens.home.quest.DialogChooseFormats;

import java.util.function.Predicate;


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
    protected Predicate<DeckProxy> buildPredicate() {
        return DeckProxy.createPredicate(SFilterUtil.buildFormatFilter(this.formats, this.allowReprints));
    }

    public void edit() {
        final DialogChooseFormats dialog = new DialogChooseFormats(this.formats);
        dialog.setOkCallback(() -> {
            allowReprints = dialog.getWantReprints();
            formats.clear();
            formats.addAll(dialog.getSelectedFormats());
        });
    }
}
