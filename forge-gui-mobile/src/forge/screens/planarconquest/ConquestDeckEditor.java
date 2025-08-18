package forge.screens.planarconquest;

import java.util.Map;

import forge.Forge;
import forge.deck.CardPool;
import forge.deck.DeckProxy;
import forge.deck.FDeckEditor;
import forge.game.GameType;
import forge.gamemodes.planarconquest.ConquestCommander;
import forge.gamemodes.planarconquest.ConquestData;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;

public class ConquestDeckEditor extends FDeckEditor {
    public ConquestDeckEditor(final ConquestCommander commander) {
        super(FDeckEditor.EditorConfigPlanarConquest,
                new DeckProxy(commander.getDeck(), Forge.getLocalizer().getMessage("lblConquestCommander"), GameType.PlanarConquest, FModel.getConquest().getDecks())
        );

        setSaveHandler(e -> {
            commander.reloadDeck(); //ensure commander receives deck changes
        });
    }

    @Override
    protected boolean allowRename() {
        return false;
    }
    @Override
    protected boolean allowDelete() {
        return false;
    }

    @Override
    protected Map<ColumnDef, ItemColumn> getColOverrides(ItemManagerConfig config) {
        return ConquestData.getColOverrides(config);
    }

    @Override
    protected void devAddCards(CardPool cards) {
        FModel.getConquest().getModel().unlockCards(cards.toFlatList());
        getCatalogPage().scheduleRefresh();
    }
}
