package forge.screens.sealed;

import forge.FThreads;
import forge.Forge;
import forge.GuiBase;
import forge.assets.FSkinFont;
import forge.deck.DeckGroup;
import forge.deck.DeckProxy;
import forge.deck.FDeckChooser;
import forge.deck.FDeckEditor;
import forge.deck.FDeckEditor.EditorType;
import forge.deck.io.DeckPreferences;
import forge.game.GameType;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.filters.ItemFilter;
import forge.limited.SealedCardPoolGenerator;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.LaunchScreen;
import forge.toolbox.FButton;
import forge.toolbox.FEvent;
import forge.toolbox.FOptionPane;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.ThreadUtil;

public class SealedScreen extends LaunchScreen {
    private final DeckManager lstDecks = add(new DeckManager(GameType.Draft));
    private final FButton btnNewDeck = add(new FButton("New Deck"));
    private final FButton btnEditDeck = add(new FButton("Edit Deck"));

    public SealedScreen() {
        super("Sealed Deck");

        lstDecks.setup(ItemManagerConfig.SEALED_DECKS);
        lstDecks.setItemActivateHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                editSelectedDeck();
            }
        });

        btnNewDeck.setFont(FSkinFont.get(16));
        btnNewDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() { //must run in game thread to prevent blocking UI thread
                    @Override
                    public void run() {
                        final DeckGroup sealed = SealedCardPoolGenerator.generateSealedDeck(GuiBase.getInterface(), false);
                        if (sealed == null) { return; }

                        FThreads.invokeInEdtLater(GuiBase.getInterface(), new Runnable() {
                            @Override
                            public void run() {
                                DeckPreferences.setSealedDeck(sealed.getName());
                                Forge.openScreen(new FDeckEditor(EditorType.Sealed, sealed.getName(), false));
                            }
                        });
                    }
                });
            }
        });
        btnEditDeck.setFont(btnNewDeck.getFont());
        btnEditDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                editSelectedDeck();
            }
        });
    }

    @Override
    public void onActivate() {
        lstDecks.setPool(DeckProxy.getAllSealedDecks(FModel.getDecks().getSealed()));
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
        float buttonWidth = (w - FDeckChooser.PADDING) / 2;
        float buttonHeight = btnNewDeck.getAutoSizeBounds().height * 1.2f;
        float listHeight = height - buttonHeight - y - FDeckChooser.PADDING;

        lstDecks.setBounds(x, y, w, listHeight);
        y += listHeight + FDeckChooser.PADDING;
        btnNewDeck.setBounds(x, y, buttonWidth, buttonHeight);
        btnEditDeck.setBounds(x + buttonWidth + FDeckChooser.PADDING, y, buttonWidth, buttonHeight);
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
        final DeckProxy human = lstDecks.getSelectedItem();
        if (human == null) {
            FOptionPane.showErrorDialog("You must select an existing deck or build a deck from a new sealed pool.", "No Deck");
            return false;
        }

        if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            String errorMessage = GameType.Sealed.getDeckFormat().getDeckConformanceProblem(human.getDeck());
            if (errorMessage != null) {
                FOptionPane.showErrorDialog("Your deck " + errorMessage + "\nPlease edit or choose a different deck.", "Invalid Deck");
                return false;
            }
        }

        int matches = FModel.getDecks().getSealed().get(human.getName()).getAiDecks().size();
        FModel.getGauntletMini().launch(matches, human.getDeck(), GameType.Sealed);
        return false; //prevent launching via launch screen since gauntlet handles it
    }
}
