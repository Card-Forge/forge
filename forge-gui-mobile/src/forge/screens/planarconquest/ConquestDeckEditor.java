package forge.screens.planarconquest;

import java.util.HashMap;
import java.util.Map;

import forge.deck.DeckProxy;
import forge.deck.FDeckEditor;
import forge.game.GameType;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.planarconquest.ConquestCommander;
import forge.planarconquest.ConquestData;

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
    public void onActivate() {
        super.onActivate();
        FModel.getConquest().getModel().updateDecksForEachCard();
    }

    @Override
    protected Map<ColumnDef, ItemColumn> getColOverrides(ItemManagerConfig config) {
        ConquestData model = FModel.getConquest().getModel();
        Map<ColumnDef, ItemColumn> colOverrides = new HashMap<ColumnDef, ItemColumn>();
        ItemColumn.addColOverride(config, colOverrides, ColumnDef.NEW, model.fnNewCompare, model.fnNewGet);
        ItemColumn.addColOverride(config, colOverrides, ColumnDef.DECKS, model.fnDeckCompare, model.fnDeckGet);
        return colOverrides;
    }
}
