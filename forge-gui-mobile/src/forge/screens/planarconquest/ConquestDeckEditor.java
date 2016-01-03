package forge.screens.planarconquest;

import java.util.Map;

import forge.deck.DeckProxy;
import forge.deck.FDeckEditor;
import forge.game.GameType;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.planarconquest.ConquestCommander;

public class ConquestDeckEditor extends FDeckEditor {
    public ConquestDeckEditor(ConquestCommander commander) {
        super(EditorType.PlanarConquest, new DeckProxy(commander.getDeck(), "Conquest Commander",
                GameType.PlanarConquest, FModel.getConquest().getDecks()), true);
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
        return FModel.getConquest().getModel().getColOverrides(config);
    }
}
