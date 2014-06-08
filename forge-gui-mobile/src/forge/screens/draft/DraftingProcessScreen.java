package forge.screens.draft;

import org.apache.commons.lang3.StringUtils;

import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.FDeckEditor;
import forge.limited.BoosterDraft;
import forge.model.FModel;
import forge.toolbox.FOptionPane;
import forge.util.Callback;

public class DraftingProcessScreen extends FDeckEditor {
    private boolean saved;
    private final BoosterDraft draft;

    public DraftingProcessScreen(BoosterDraft draft0) {
        super(EditorType.Draft);
        draft = draft0;
        getCatalogPage().refresh(); //must refresh after draft set
    }

    @Override
    protected BoosterDraft getDraft() {
        return draft;
    }

    @Override
    protected void save() {
        if (saved) { return; }

        FOptionPane.showInputDialog("Save this draft as:", "Save Draft", FOptionPane.QUESTION_ICON, new Callback<String>() {
            @Override
            public void run(final String name) {
                if (StringUtils.isEmpty(name)) {
                    save(); //re-prompt if user doesn't pick a name
                    return;
                }

                // Check for overwrite case
                for (DeckGroup d : FModel.getDecks().getDraft()) {
                    if (name.equalsIgnoreCase(d.getName())) {
                        FOptionPane.showConfirmDialog(
                            "There is already a deck named '" + name + "'. Overwrite?",
                            "Overwrite Deck?", false, new Callback<Boolean>() {
                                @Override
                                public void run(Boolean result) {
                                    if (result) {
                                        finishSave(name);
                                    }
                                    else {
                                        save(); //If no overwrite, recurse
                                    }
                                }
                            });
                        return;
                    }
                }

                finishSave(name);
            }
        });
    }

    private void finishSave(String name) {
        saved = true;

        // Construct computer's decks and save draft
        final Deck[] computer = draft.getDecks();

        final DeckGroup finishedDraft = new DeckGroup(name);
        finishedDraft.setHumanDeck((Deck) getDeck().copyTo(name));
        finishedDraft.addAiDecks(computer);

        FModel.getDecks().getDraft().add(finishedDraft);
    }

    @Override
    public void onClose(Callback<Boolean> canCloseCallback) {
        if (saved || canCloseCallback == null) {
            super.onClose(canCloseCallback); //can skip prompt if draft saved
            return;
        }
        FOptionPane.showConfirmDialog("This will end the current draft and you will not be able to resume.\n\n" +
                "Leave anyway?", "Leave Draft?", "Leave", "Cancel", false, canCloseCallback);
    }
}
