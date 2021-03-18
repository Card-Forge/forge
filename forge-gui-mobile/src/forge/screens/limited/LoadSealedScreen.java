package forge.screens.limited;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
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
import forge.gamemodes.match.HostedMatch;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.util.SGuiChoose;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.filters.ItemFilter;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.screens.home.LoadGameMenu;
import forge.toolbox.FComboBox;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.util.Localizer;

public class LoadSealedScreen extends LaunchScreen {
    private final DeckManager lstDecks = add(new DeckManager(GameType.Draft));
    private final FLabel lblTip = add(new FLabel.Builder()
        .text(Localizer.getInstance().getMessage("lblDoubleTapToEditDeck"))
        .textColor(FLabel.INLINE_LABEL_COLOR)
        .align(Align.center).font(FSkinFont.get(12)).build());

    private final FSkinFont GAME_MODE_FONT= FSkinFont.get(12);
    private final FLabel lblMode = add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblMode")).font(GAME_MODE_FONT).build());
    private final FComboBox<String> cbMode = add(new FComboBox<>());

    public LoadSealedScreen() {
        super(null, LoadGameMenu.getMenu());

        cbMode.setFont(GAME_MODE_FONT);
        cbMode.addItem(Localizer.getInstance().getMessage("lblGauntlet"));
        cbMode.addItem(Localizer.getInstance().getMessage("lblSingleMatch"));

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
        float comboBoxHeight = cbMode.getHeight();

        lblMode.setBounds(x, y, lblMode.getAutoSizeBounds().width + FDeckChooser.PADDING / 2, comboBoxHeight);
        cbMode.setBounds(x + lblMode.getWidth(), y, w - lblMode.getWidth(), comboBoxHeight);
        y += comboBoxHeight + FDeckChooser.PADDING;
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
                    FOptionPane.showErrorDialog(Localizer.getInstance().getMessage("lblYouMustSelectExistingSealedPool"), Localizer.getInstance().getMessage("lblNoDeck"));
                    return;
                }

                final boolean gauntlet = cbMode.getSelectedItem().equals(Localizer.getInstance().getMessage("lblGauntlet"));

                if (gauntlet) {
                    FThreads.invokeInEdtLater(new Runnable() {
                        @Override
                        public void run() {
                            if (!checkDeckLegality(humanDeck)) {
                                return;
                            }

                            LoadingOverlay.show(Localizer.getInstance().getMessage("lblLoadingNewGame"), new Runnable() {
                                @Override
                                public void run() {
                                    final int matches = FModel.getDecks().getSealed().get(humanDeck.getName()).getAiDecks().size();
                                    FModel.getGauntletMini().launch(matches, humanDeck.getDeck(), GameType.Sealed);
                                }
                            });
                        }
                    });
                } else {

                    final Integer aiIndex = SGuiChoose.getInteger(Localizer.getInstance().getMessage("lblWhichOpponentWouldYouLikeToFace"),
                            1, FModel.getDecks().getSealed().get(humanDeck.getName()).getAiDecks().size());
                    if (aiIndex == null) {
                        return; // Cancel was pressed
                    }

                    final DeckGroup opponentDecks = FModel.getDecks().getSealed().get(humanDeck.getName());
                    final Deck aiDeck = opponentDecks.getAiDecks().get(aiIndex - 1);
                    if (aiDeck == null) {
                        throw new IllegalStateException("Draft: Computer deck is null!");
                    }

                    FThreads.invokeInEdtLater(new Runnable() {
                        @Override
                        public void run() {
                            if (!checkDeckLegality(humanDeck)) {
                                return;
                            }

                            LoadingOverlay.show(Localizer.getInstance().getMessage("lblLoadingNewGame"), new Runnable() {
                                @Override
                                public void run() {
                                    final List<RegisteredPlayer> starter = new ArrayList<>();
                                    final RegisteredPlayer human = new RegisteredPlayer(humanDeck.getDeck()).setPlayer(GamePlayerUtil.getGuiPlayer());
                                    starter.add(human);
                                    starter.add(new RegisteredPlayer(aiDeck).setPlayer(GamePlayerUtil.createAiPlayer()));
                                    for (final RegisteredPlayer pl : starter) {
                                        pl.assignConspiracies();
                                    }

                                    FModel.getGauntletMini().resetGauntletDraft();
                                    final HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
                                    hostedMatch.startMatch(GameType.Sealed, null, starter, human, GuiBase.getInterface().getNewGuiGame());
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private boolean checkDeckLegality(DeckProxy humanDeck) {
        if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            String errorMessage = GameType.Sealed.getDeckFormat().getDeckConformanceProblem(humanDeck.getDeck());
            if (errorMessage != null) {
                FOptionPane.showErrorDialog(Localizer.getInstance().getMessage("lblInvalidDeckDesc").replace("%n", errorMessage), Localizer.getInstance().getMessage("lblInvalidDeck"));
                return false;
            }
        }
        return true;
    }
}
