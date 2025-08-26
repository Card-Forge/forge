package forge.screens.quest;

import java.util.HashMap;
import java.util.Map;

import forge.deck.CardPool;
import forge.deck.DeckProxy;
import forge.deck.FDeckEditor;
import forge.gamemodes.quest.QuestSpellShop;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;

public class QuestDeckEditor extends FDeckEditor {
    public QuestDeckEditor(boolean commander) {
        super(commander ? FDeckEditor.EditorConfigQuestCommander : FDeckEditor.EditorConfigQuest, "");
    }
    public QuestDeckEditor(DeckProxy editDeck, boolean commander) {
        super(commander ? FDeckEditor.EditorConfigQuestCommander : FDeckEditor.EditorConfigQuest, editDeck);
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

    @Override
    protected void devAddCards(CardPool cards) {
        FModel.getQuest().getCards().getCardpool().addAll(cards);
        getCatalogPage().scheduleRefresh();
    }
}
