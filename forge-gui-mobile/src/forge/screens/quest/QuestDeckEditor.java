package forge.screens.quest;

import java.util.HashMap;
import java.util.Map;
import forge.deck.DeckProxy;
import forge.deck.FDeckEditor;
import forge.gamemodes.quest.QuestSpellShop;
import forge.item.PaperCard;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;

public class QuestDeckEditor extends FDeckEditor {
    public QuestDeckEditor(boolean commander) {
        super(commander ? EditorType.QuestCommander: EditorType.Quest, "", false);
    }
    public QuestDeckEditor(DeckProxy editDeck, boolean commander) {
        super(commander ? EditorType.QuestCommander: EditorType.Quest, editDeck, true);
    }

    @Override
    public void onActivate() {
        super.onActivate();
        QuestSpellShop.updateDecksForEachCard();
    }

    @Override
    protected Map<ColumnDef, ItemColumn> getColOverrides(ItemManagerConfig config) {
        Map<ColumnDef, ItemColumn> colOverrides = new HashMap<>();
        switch (config) {
        case QUEST_EDITOR_POOL:
            ItemColumn.addColOverride(config, colOverrides, ColumnDef.NEW, FModel.getQuest().getCards().getFnNewCompare(), FModel.getQuest().getCards().getFnNewGet());
            break;
        case QUEST_DECK_EDITOR:
            ItemColumn.addColOverride(config, colOverrides, ColumnDef.NEW, FModel.getQuest().getCards().getFnNewCompare(), FModel.getQuest().getCards().getFnNewGet());
            ItemColumn.addColOverride(config, colOverrides, ColumnDef.DECKS, QuestSpellShop.fnDeckCompare, QuestSpellShop.fnDeckGet);
            break;
        default:
            colOverrides = null; //shouldn't happen
            break;
        }
        return colOverrides;
    }

    /**
     * Adds any card to the catalog and data pool.
     * 
     * @param card {@link forge.item.PaperCard}
     */
    public void addCheatCard(final PaperCard card, int qty) {
        getCatalogPage().addCard(card, qty);
        FModel.getQuest().getCards().getCardpool().add(card, qty);
    }
}
