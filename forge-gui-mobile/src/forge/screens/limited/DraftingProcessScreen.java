package forge.screens.limited;

import org.apache.commons.lang3.StringUtils;

import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.FDeckEditor;
import forge.deck.io.DeckPreferences;
import forge.limited.BoosterDraft;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.home.LoadGameMenu.LoadGameScreen;
import forge.toolbox.FOptionPane;
import forge.util.Callback;

public class DraftingProcessScreen extends FDeckEditor {
    private boolean isDraftSaved;
    private final BoosterDraft draft;

    public DraftingProcessScreen(BoosterDraft draft0, EditorType editorType0) {
        super(editorType0, "", false);
        draft = draft0;
        getCatalogPage().refresh(); //must refresh after draft set
    }

    @Override
    protected BoosterDraft getDraft() {
        return draft;
    }

    @Override
    protected void save(final Callback<Boolean> callback) {
        if (isDraftSaved) { //if draft itself is saved, let base class handle saving deck changes
            super.save(callback);
            return;
        }

        FOptionPane.showInputDialog("Save this draft as?", new Callback<String>() {
            @Override
            public void run(final String name) {
                if (StringUtils.isEmpty(name)) {
                    save(callback); //re-prompt if user doesn't pick a name
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
                                        if (callback != null) {
                                            callback.run(true);
                                        }
                                    }
                                    else {
                                        save(callback); //If no overwrite, recurse
                                    }
                                }
                            });
                        return;
                    }
                }

                finishSave(name);
                if (callback != null) {
                    callback.run(true);
                }
            }
        });
    }

    private void finishSave(String name) {
        isDraftSaved = true;

        // Construct computer's decks and save draft
        final Deck[] computer = draft.getDecks();

        final DeckGroup finishedDraft = new DeckGroup(name);
        finishedDraft.setHumanDeck((Deck) getDeck().copyTo(name));
        finishedDraft.addAiDecks(computer);

        FModel.getDecks().getDraft().add(finishedDraft);
        getEditorType().getController().load("", name);
        DeckPreferences.setDraftDeck(name);

        LoadGameScreen.BoosterDraft.setAsBackScreen(false); //set load draft screen to be opened when user done editing deck

        //show header for main deck and sideboard when finished drafting
        deckHeader.setVisible(true);
        revalidate();
    }

    @Override
    public void onClose(Callback<Boolean> canCloseCallback) {
        if (isDraftSaved || canCloseCallback == null) {
            super.onClose(canCloseCallback); //can skip prompt if draft saved
            return;
        }
        FOptionPane.showConfirmDialog("This will end the current draft and you will not be able to resume.\n\n" +
                "Leave anyway?", "Leave Draft?", "Leave", "Cancel", false, canCloseCallback);
    }

    @Override
    public FScreen getLandscapeBackdropScreen() {
        return null;
    }
}
