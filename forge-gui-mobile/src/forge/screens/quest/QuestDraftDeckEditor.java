package forge.screens.quest;

import forge.deck.Deck;
import forge.deck.FDeckEditor;

public class QuestDraftDeckEditor extends FDeckEditor {
    public QuestDraftDeckEditor(String existingDeckName) {
        super(EditorType.QuestDraft, existingDeckName, true);
    }
    public QuestDraftDeckEditor(Deck newDeck) {
        super(EditorType.QuestDraft, newDeck, false);
    }

    @Override
    protected boolean allowRename() {
        return false;
    }
    @Override
    protected boolean allowDelete() {
        return false;
    }
}
