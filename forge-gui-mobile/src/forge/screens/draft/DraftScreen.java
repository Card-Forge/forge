package forge.screens.draft;

import forge.FThreads;
import forge.Forge;
import forge.GuiBase;
import forge.properties.ForgePreferences.FPref;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.toolbox.FButton;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FOptionPane;
import forge.util.ThreadUtil;
import forge.assets.FSkinFont;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckProxy;
import forge.deck.FDeckChooser;
import forge.deck.FDeckEditor;
import forge.deck.FDeckEditor.EditorType;
import forge.deck.io.DeckPreferences;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.filters.ItemFilter;
import forge.limited.BoosterDraft;
import forge.limited.LimitedPoolType;
import forge.model.FModel;
import forge.util.gui.SGuiChoose;

public class DraftScreen extends LaunchScreen {
    private final DeckManager lstDecks = add(new DeckManager(GameType.Draft));
    private final FButton btnNewDraft = add(new FButton("New Draft"));
    private final FButton btnEditDeck = add(new FButton("Edit Deck"));

    public DraftScreen() {
        super("Booster Draft");

        lstDecks.setup(ItemManagerConfig.DRAFT_DECKS);
        lstDecks.setItemActivateHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                editSelectedDeck();
            }
        });

        btnNewDraft.setFont(FSkinFont.get(16));
        btnNewDraft.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() { //must run in game thread to prevent blocking UI thread
                    @Override
                    public void run() {
                        final LimitedPoolType poolType = SGuiChoose.oneOrNone("Choose Draft Format", LimitedPoolType.values());
                        if (poolType == null) { return; }

                        final BoosterDraft draft = BoosterDraft.createDraft(poolType);
                        if (draft == null) { return; }

                        FThreads.invokeInEdtLater(new Runnable() {
                            @Override
                            public void run() {
                                LoadingOverlay.show("Loading new draft...", new Runnable() {
                                    @Override
                                    public void run() {
                                        Forge.openScreen(new DraftingProcessScreen(draft));
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
        btnEditDeck.setFont(btnNewDraft.getFont());
        btnEditDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                editSelectedDeck();
            }
        });
    }

    @Override
    public void onActivate() {
        lstDecks.setPool(DeckProxy.getDraftDecks(FModel.getDecks().getDraft()));
        lstDecks.setSelectedString(DeckPreferences.getDraftDeck());
    }

    private void editSelectedDeck() {
        final DeckProxy deck = lstDecks.getSelectedItem();
        if (deck == null) { return; }

        DeckPreferences.setDraftDeck(deck.getName());
        Forge.openScreen(new FDeckEditor(EditorType.Draft, deck));
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float x = ItemFilter.PADDING;
        float y = startY;
        float w = width - 2 * x;
        float buttonWidth = (w - FDeckChooser.PADDING) / 2;
        float buttonHeight = btnNewDraft.getAutoSizeBounds().height * 1.2f;
        float listHeight = height - buttonHeight - y - FDeckChooser.PADDING;

        lstDecks.setBounds(x, y, w, listHeight);
        y += listHeight + FDeckChooser.PADDING;
        btnNewDraft.setBounds(x, y, buttonWidth, buttonHeight);
        btnEditDeck.setBounds(x + buttonWidth + FDeckChooser.PADDING, y, buttonWidth, buttonHeight);
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
        final DeckProxy humanDeck = lstDecks.getSelectedItem();
        if (humanDeck == null) {
            FOptionPane.showErrorDialog("You must select an existing deck or build a deck from a new booster draft game.", "No Deck");
            return false;
        }

        if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            String errorMessage = GameType.Draft.getDecksFormat().getDeckConformanceProblem(humanDeck.getDeck());
            if (errorMessage != null) {
                FOptionPane.showErrorDialog("Your deck " + errorMessage + "\nPlease edit or choose a different deck.", "Invalid Deck");
                return false;
            }
        }

        FModel.getGauntletMini().resetGauntletDraft();

        /*if (radAll.isSelected()) {
            int rounds = FModel.getDecks().getDraft().get(humanDeck.getName()).getAiDecks().size();
            FModel.getGauntletMini().launch(rounds, humanDeck.getDeck(), GameType.Draft);
            return false;
        }*/

        final int aiIndex = (int) Math.floor(Math.random() * 7);
        DeckGroup deckGroup = FModel.getDecks().getDraft().get(humanDeck.getName());
        Deck aiDeck = deckGroup.getAiDecks().get(aiIndex);
        if (aiDeck == null) {
            throw new IllegalStateException("Draft: Computer deck is null!");
        }

        launchParams.gameType = GameType.Draft;
        launchParams.players.add(new RegisteredPlayer(humanDeck.getDeck()).setPlayer(GuiBase.getInterface().getGuiPlayer()));
        launchParams.players.add(new RegisteredPlayer(aiDeck).setPlayer(GuiBase.getInterface().createAiPlayer()));

        return true;
    }
}
