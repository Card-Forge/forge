package forge.screens.quest;

import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.deck.FDeckEditor;

public class QuestDraftDeckEditor extends FDeckEditor {
    public QuestDraftDeckEditor(String existingDeckName) {
        super(FDeckEditor.EditorConfigQuestDraft, existingDeckName);
    }
    public QuestDraftDeckEditor(DeckGroup newDecks) {
        super(FDeckEditor.EditorConfigQuestDraft, newDecks);
        setSelectedSection(DeckSection.Sideboard);
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
