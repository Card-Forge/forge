package forge.screens.draft;

import org.apache.commons.lang3.StringUtils;

import forge.card.MagicColor;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.limited.BoosterDraft;
import forge.limited.IBoosterDraft;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.TabPageScreen;
import forge.toolbox.FOptionPane;
import forge.util.Callback;
import forge.util.MyRandom;

public class DraftingProcessScreen extends TabPageScreen<DraftingProcessScreen> {
    private boolean saved;
    private final BoosterDraft draft;
    private final Deck deck;

    @SuppressWarnings("unchecked")
    public DraftingProcessScreen(BoosterDraft draft0) {
        super(new TabPage[] {
                new DraftPackPage(),
                new DraftMainPage(),
                new DraftSideboardPage()
        });
        draft = draft0;

        //create starting point for draft deck with lands in sideboard
        deck = new Deck();
        deck.getOrCreate(DeckSection.Sideboard);
/*
        final String landSet = IBoosterDraft.LAND_SET_CODE[0].getCode();
        final boolean isZendikarSet = landSet.equals("ZEN"); // we want to generate one kind of Zendikar lands at a time only
        final boolean zendikarSetMode = MyRandom.getRandom().nextBoolean();

        final int landsCount = 10;

        for (String landName : MagicColor.Constant.BASIC_LANDS) {
            int numArt = FModel.getMagicDb().getCommonCards().getArtCount(landName, landSet);
            int minArtIndex = isZendikarSet ? (zendikarSetMode ? 1 : 5) : 1;
            int maxArtIndex = isZendikarSet ? minArtIndex + 3 : numArt;

            if (FModel.getPreferences().getPrefBoolean(FPref.UI_RANDOM_ART_IN_POOLS)) {
                for (int i = minArtIndex; i <= maxArtIndex; i++) {
                    deck.get(DeckSection.Sideboard).add(landName, landSet, i, numArt > 1 ? landsCount : 30);
                }
            }
            else {
                deck.get(DeckSection.Sideboard).add(landName, landSet, 30);
            }
        }*/

        //show initial cards in card managers
        getPackPage().showChoices();
        getMainPage().refresh();
        getSideboardPage().refresh();
    }

    public DraftPackPage getPackPage() {
        return (DraftPackPage)tabPages[0];
    }

    public DraftMainPage getMainPage() {
        return (DraftMainPage)tabPages[1];
    }

    public DraftSideboardPage getSideboardPage() {
        return (DraftSideboardPage)tabPages[2];
    }

    public BoosterDraft getDraft() {
        return draft;
    }

    public Deck getDeck() {
        return deck;
    }

    public void saveDraft() {
        if (saved) { return; }

        FOptionPane.showInputDialog("Save this draft as:", "Save Draft", FOptionPane.QUESTION_ICON, new Callback<String>() {
            @Override
            public void run(final String name) {
                if (StringUtils.isEmpty(name)) {
                    saveDraft(); //re-prompt if user doesn't pick a name
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
                                        saveDraft(); //If no overwrite, recurse
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
        finishedDraft.setHumanDeck((Deck) deck.copyTo(name));
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
