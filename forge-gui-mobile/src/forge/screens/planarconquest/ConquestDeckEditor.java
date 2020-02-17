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
import forge.planarconquest.ConquestData;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Localizer;

public class ConquestDeckEditor extends FDeckEditor {
    public ConquestDeckEditor(final ConquestCommander commander) {
        super(EditorType.PlanarConquest, new DeckProxy(commander.getDeck(), Localizer.getInstance().getMessage("lblConquestCommander"),
                GameType.PlanarConquest, FModel.getConquest().getDecks()), true);

        setSaveHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                commander.reloadDeck(); //ensure commander receives deck changes
            }
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
}
