package forge.screens.deckeditor.controllers;

import java.util.EnumSet;

import forge.deck.CardPool;
import forge.deck.DeckBase;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.gui.framework.FScreen;
import forge.item.PaperCard;
import forge.screens.match.controllers.CDetailPicture;

public abstract class CDeckEditor<TModel extends DeckBase> extends ACEditorBase<PaperCard, TModel> {
    protected CDeckEditor(FScreen screen0, CDetailPicture cDetailPicture0, GameType gameType0) {
        super(screen0, cDetailPicture0, gameType0);
    }

    /**
     * While user edits the deck, the catalog content changes.
     * In order to keep deck loading logic simple we need an initial card pool state to pick cards from.
     *
     * The method should only be used when the catalog is non infinite.
     */
    protected CardPool getInitialCatalog() {
        if (getCatalogManager().isInfinite()) {
            throw new UnsupportedOperationException("Currently used catalog is infinite");
        }

        CardPool result = new CardPool();
        result.addAll(getCatalogManager().getPool());

        for (DeckSection section: EnumSet.allOf(DeckSection.class)) {
            if (isSectionPickableFromCatalog(section)) {
                result.addAll(getHumanDeck().getOrCreate(section));
            }
        }

        return result;
    }

    public Boolean isSectionImportable(DeckSection section) {
        return section == DeckSection.Main;
    }

    /**
     * Can user pick a card from the catalog into specific section
     *
     * The returned value is only used when the catalog is non infinite.
     * When it is infinite, the implementation may safely return null.
     */
    protected Boolean isSectionPickableFromCatalog(DeckSection section) {
        return isSectionImportable(section);
    }
}
