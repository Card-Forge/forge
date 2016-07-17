package forge.screens.limited;

import org.apache.commons.lang3.StringUtils;

import forge.FThreads;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.FDeckEditor;
import forge.deck.io.DeckPreferences;
import forge.limited.BoosterDraft;
import forge.model.FModel;
import forge.quest.QuestEventDraft;
import forge.quest.QuestTournamentController;
import forge.screens.FScreen;
import forge.screens.home.LoadGameMenu.LoadGameScreen;
import forge.toolbox.FOptionPane;
import forge.util.Callback;

public class DraftingProcessScreen extends FDeckEditor {
    private boolean isDraftSaved;
    private final BoosterDraft draft;
    private final QuestTournamentController questDraftController;

    public DraftingProcessScreen(BoosterDraft draft0, EditorType editorType0, QuestTournamentController questDraftController0) {
        super(editorType0, "", false);
        draft = draft0;
        questDraftController = questDraftController0;
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

        if (getEditorType() == EditorType.QuestDraft) {
            finishSave(QuestEventDraft.DECK_NAME);
            if (callback != null) {
                callback.run(true);
            }
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

        switch (getEditorType()) {
        case Draft:
            FModel.getDecks().getDraft().add(finishedDraft);
            getEditorType().getController().load("", name);
            DeckPreferences.setDraftDeck(name);

            LoadGameScreen.BoosterDraft.setAsBackScreen(false); //set load draft screen to be opened when user done editing deck
            break;
        case QuestDraft:
            FModel.getQuest().getDraftDecks().add(finishedDraft);
            getEditorType().getController().load("", name);
            break;
        default:
            break;
        }

        //show header for main deck and sideboard when finished drafting
        deckHeader.setVisible(true);
        revalidate();
    }

    @Override
    public void onClose(final Callback<Boolean> canCloseCallback) {
        if (isDraftSaved || canCloseCallback == null) {
            super.onClose(canCloseCallback); //can skip prompt if draft saved
            return;
        }

        if (getEditorType() == EditorType.QuestDraft) {
            FThreads.invokeInBackgroundThread(new Runnable() {
                @Override
                public void run() {
                    if (questDraftController.cancelDraft()) {
                        FThreads.invokeInEdtLater(new Runnable() {
                            @Override
                            public void run() {
                                canCloseCallback.run(true);
                            }
                        });
                    }
                }
            });
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
