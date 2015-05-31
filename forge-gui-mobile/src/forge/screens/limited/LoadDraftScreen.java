package forge.screens.limited;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.FThreads;
import forge.Forge;
import forge.properties.ForgePreferences.FPref;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.screens.home.LoadGameMenu;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.assets.FSkinFont;
import forge.deck.DeckProxy;
import forge.deck.FDeckChooser;
import forge.deck.FDeckEditor;
import forge.deck.FDeckEditor.EditorType;
import forge.deck.io.DeckPreferences;
import forge.game.GameType;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.filters.ItemFilter;
import forge.model.FModel;
import forge.util.gui.SGuiChoose;

public class LoadDraftScreen extends LaunchScreen {
    private final DeckManager lstDecks = add(new DeckManager(GameType.Draft));
    private final FLabel lblTip = add(new FLabel.Builder()
        .text("Double-tap to edit deck (Long-press to view)")
        .textColor(FLabel.INLINE_LABEL_COLOR)
        .align(HAlignment.CENTER).font(FSkinFont.get(12)).build());

    public LoadDraftScreen() {
        super(null, LoadGameMenu.getMenu());

        lstDecks.setup(ItemManagerConfig.DRAFT_DECKS);
        lstDecks.setItemActivateHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                editSelectedDeck();
            }
        });
    }

    @Override
    public void onActivate() {
        lstDecks.setPool(DeckProxy.getAllDraftDecks());
        lstDecks.setSelectedString(DeckPreferences.getDraftDeck());
    }

    private void editSelectedDeck() {
        final DeckProxy deck = lstDecks.getSelectedItem();
        if (deck == null) { return; }

        DeckPreferences.setDraftDeck(deck.getName());
        Forge.openScreen(new FDeckEditor(EditorType.Draft, deck, true));
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float x = ItemFilter.PADDING;
        float y = startY;
        float w = width - 2 * x;
        float labelHeight = lblTip.getAutoSizeBounds().height;
        float listHeight = height - labelHeight - y - FDeckChooser.PADDING;

        lstDecks.setBounds(x, y, w, listHeight);
        y += listHeight + FDeckChooser.PADDING;
        lblTip.setBounds(x, y, w, labelHeight);
    }

    @Override
    protected void startMatch() {
        FThreads.invokeInBackgroundThread(new Runnable() {
            @Override
            public void run() {
                final DeckProxy humanDeck = lstDecks.getSelectedItem();
                if (humanDeck == null) {
                    FOptionPane.showErrorDialog("You must select an existing deck or build a deck from a new booster draft game.", "No Deck");
                    return;
                }

                if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
                    String errorMessage = GameType.Draft.getDeckFormat().getDeckConformanceProblem(humanDeck.getDeck());
                    if (errorMessage != null) {
                        FOptionPane.showErrorDialog("Your deck " + errorMessage + "\nPlease edit or choose a different deck.", "Invalid Deck");
                        return;
                    }
                }

                final Integer rounds = SGuiChoose.getInteger("How many opponents are you willing to face?",
                        1, FModel.getDecks().getDraft().get(humanDeck.getName()).getAiDecks().size());
                if (rounds == null) {
                    return;
                }

                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        LoadingOverlay.show("Loading new game...", new Runnable() {
                            @Override
                            public void run() {
                                FModel.getGauntletMini().resetGauntletDraft();
                                FModel.getGauntletMini().launch(rounds, humanDeck.getDeck(), GameType.Draft);
                            }
                        });
                    }
                });
            }
        });
    }
}
