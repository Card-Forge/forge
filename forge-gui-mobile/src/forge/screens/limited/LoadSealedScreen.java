package forge.screens.limited;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
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
import forge.properties.ForgePreferences.FPref;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.screens.home.LoadGameMenu;
import forge.toolbox.FEvent;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FEvent.FEventHandler;

public class LoadSealedScreen extends LaunchScreen {
    private final DeckManager lstDecks = add(new DeckManager(GameType.Draft));
    private final FLabel lblTip = add(new FLabel.Builder()
        .text("Double-tap to edit deck (Long-press to view)")
        .textColor(FLabel.INLINE_LABEL_COLOR)
        .align(HAlignment.CENTER).font(FSkinFont.get(12)).build());

    public LoadSealedScreen() {
        super(null, LoadGameMenu.getMenu());

        lstDecks.setup(ItemManagerConfig.SEALED_DECKS);
        lstDecks.setItemActivateHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                editSelectedDeck();
            }
        });
    }

    @Override
    public void onActivate() {
        lstDecks.setPool(DeckProxy.getAllSealedDecks());
        lstDecks.setSelectedString(DeckPreferences.getSealedDeck());
    }

    private void editSelectedDeck() {
        final DeckProxy deck = lstDecks.getSelectedItem();
        if (deck == null) { return; }

        DeckPreferences.setSealedDeck(deck.getName());
        Forge.openScreen(new FDeckEditor(EditorType.Sealed, deck, true));
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
        final DeckProxy human = lstDecks.getSelectedItem();
        if (human == null) {
            FOptionPane.showErrorDialog("You must select an existing deck or build a deck from a new sealed pool.", "No Deck");
            return;
        }

        if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            String errorMessage = GameType.Sealed.getDeckFormat().getDeckConformanceProblem(human.getDeck());
            if (errorMessage != null) {
                FOptionPane.showErrorDialog("Your deck " + errorMessage + "\nPlease edit or choose a different deck.", "Invalid Deck");
                return;
            }
        }

        LoadingOverlay.show("Loading new game...", new Runnable() {
            @Override
            public void run() {
                final int matches = FModel.getDecks().getSealed().get(human.getName()).getAiDecks().size();
                FModel.getGauntletMini().launch(matches, human.getDeck(), GameType.Sealed);
            }
        });
    }
}
