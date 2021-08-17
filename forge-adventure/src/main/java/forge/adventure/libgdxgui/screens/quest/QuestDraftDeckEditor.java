package forge.adventure.libgdxgui.screens.quest;

import forge.adventure.libgdxgui.deck.FDeckEditor;
import forge.deck.Deck;

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
