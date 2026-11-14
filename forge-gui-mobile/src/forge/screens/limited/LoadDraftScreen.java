package forge.screens.limited;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.assets.FSkinFont;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.FDeckChooser;
import forge.deck.FDeckEditor;
import forge.deck.io.DeckPreferences;
import forge.game.GameType;
import forge.gamemodes.limited.CommanderDraftUtil;
import forge.gamemodes.limited.LimitedMatch;
import forge.gamemodes.net.EventFormat;
import forge.gui.FThreads;
import forge.gui.util.SGuiChoose;
import forge.toolbox.Frost;
import forge.util.GuiPrefBinders;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.filters.ItemFilter;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.screens.home.LoadGameMenu;
import forge.toolbox.FComboBox;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.util.Utils;

public class LoadDraftScreen extends LaunchScreen {
    private final DeckManager lstDecks = add(new DeckManager(GameType.Draft));
    private final FLabel lblTip = add(new FLabel.Builder()
        .text(Forge.getLocalizer().getMessage("lblDoubleTapToEditDeck"))
        .textColor(FLabel.getInlineLabelColor())
        .align(Align.center).font(FSkinFont.get(12)).build());

    private final FSkinFont GAME_MODE_FONT= FSkinFont.get(12);
    private final FLabel lblMode = add(new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblMode")).font(GAME_MODE_FONT).build());
    private final FComboBox<String> cbMode = add(new FComboBox<>());

    // Max games in a match frame and variables
    private final FLabel lblGamesInMatch = add(new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblMatch") + ":").font(GAME_MODE_FONT).build());
    private final FComboBox<String> cbGamesInMatch = add(new FComboBox<>());
    private final GuiPrefBinders.ComboBox cbGamesInMatchBinder = new GuiPrefBinders.ComboBox(
        FPref.UI_MATCHES_PER_GAME, cbGamesInMatch);

    public LoadDraftScreen() {
        super(null, LoadGameMenu.getMenu());

        cbMode.setFont(GAME_MODE_FONT);
        cbMode.addItem(Forge.getLocalizer().getMessage("lblGauntlet"));
        cbMode.addItem(Forge.getLocalizer().getMessage("lblSingleMatch"));

        lstDecks.setup(ItemManagerConfig.DRAFT_DECKS);
        lstDecks.setItemActivateHandler(event -> editSelectedDeck());

        cbGamesInMatch.setFont(GAME_MODE_FONT);
        cbGamesInMatch.addItem("1");
        cbGamesInMatch.addItem("3");
        cbGamesInMatch.addItem("5");
        cbGamesInMatchBinder.bind();
    }

    @Override
    public void onActivate() {
        List<DeckProxy> combined = new ArrayList<>(DeckProxy.getAllDraftDecks());
        for (DeckProxy p : DeckProxy.getAllNetworkEventDecks()) {
            if (EventFormat.BOOSTER_DRAFT.name().equals(DeckProxy.getEventTag(p.getDeck(), "eventFormat"))) {
                combined.add(p);
            }
        }
        lstDecks.setPool(combined);
        lstDecks.setSelectedString(DeckPreferences.getDraftDeck());
        cbGamesInMatchBinder.load();
    }

    private void editSelectedDeck() {
        final DeckProxy deck = lstDecks.getSelectedItem();
        if (deck == null) { return; }

        DeckPreferences.setDraftDeck(deck.getName());
        Forge.openScreen(new FDeckEditor(CommanderDraftUtil.isCommanderDraftDeck(deck.getDeck())
                ? FDeckEditor.EditorConfigCommanderDraft : FDeckEditor.EditorConfigDraft, deck));
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float x = ItemFilter.PADDING;
        float y = startY;
        float w = width - 2 * x;
        float labelHeight = lblTip.getAutoSizeBounds().height;
        float listHeight = height - labelHeight - y - FDeckChooser.PADDING;
        float comboBoxHeight = cbMode.getHeight();

        float x2 = x;
        float w1 = lblMode.getAutoSizeBounds().width;
        float w2 = lblGamesInMatch.getAutoSizeBounds().width;
        lblMode.setBounds(x2, y, w1 + FDeckChooser.PADDING / 2, comboBoxHeight);
        x2 += lblMode.getWidth();
        cbMode.setBounds(x2, y, w - x2 - w2 - Utils.AVG_FINGER_WIDTH, comboBoxHeight);
        x2 += cbMode.getWidth();
        lblGamesInMatch.setBounds(x2, y, w2 + FDeckChooser.PADDING / 2, comboBoxHeight);
        x2 += lblGamesInMatch.getWidth();
        cbGamesInMatch.setBounds(x2, y, Utils.AVG_FINGER_WIDTH, comboBoxHeight);
        y += comboBoxHeight + FDeckChooser.PADDING;
        lstDecks.setBounds(x, y, w, listHeight);
        y += listHeight + FDeckChooser.PADDING;
        lblTip.setBounds(x, y, w, labelHeight);
        lblTip.setVisible(!Frost.isOn());
        y += labelHeight + FDeckChooser.PADDING;
    }

    @Override
    protected void startMatch() {
        FThreads.invokeInBackgroundThread(() -> {
            final DeckProxy humanDeck = lstDecks.getSelectedItem();
            if (humanDeck == null) {
                FThreads.invokeInEdtLater(() -> { //should be in EDT...
                    FOptionPane.showErrorDialog(Forge.getLocalizer().getMessage("lblYouMustSelectExistingDeck"),
                        Forge.getLocalizer().getMessage("lblNoDeck"));
                });
                return;
            }

            final LimitedMatch limitedMatch = FModel.getLimitedMatch();
            final List<Deck> aiDecks = limitedMatch.getAiDecks(GameType.Draft, humanDeck.getDeck());
            // Event decks lack a DeckGroup for gauntlet/AI play
            if (aiDecks.isEmpty()) {
                FThreads.invokeInEdtLater(() ->
                    FOptionPane.showErrorDialog(
                        Forge.getLocalizer().getMessage("lblEventDeckEditOnly"),
                        Forge.getLocalizer().getMessage("lblNoOpponentsForEventDeck")));
                return;
            }

            // TODO: if booster draft tournaments are supported in the future, add the possibility to choose them here
            final boolean gauntlet = cbMode.getSelectedItem().equals(Forge.getLocalizer().getMessage("lblGauntlet"));
            final Integer choice = SGuiChoose.getInteger(Forge.getLocalizer().getMessage(
                    gauntlet ? "lblHowManyOpponents" : "lblWhichOpponentWouldYouLikeToFace"), 1, aiDecks.size());
            if (choice == null) {
                return; // Cancel was pressed
            }

            FThreads.invokeInEdtLater(() -> {
                if (!checkDeckLegality(humanDeck)) {
                    return;
                }

                LoadingOverlay.show(Forge.getLocalizer().getMessage("lblLoadingNewGame"), true, () -> {
                    if (gauntlet) {
                        limitedMatch.launchGauntlet(GameType.Draft, humanDeck.getDeck(), choice);
                    } else {
                        limitedMatch.startMatch(GameType.Draft, humanDeck.getDeck(), Collections.singletonList(aiDecks.get(choice - 1)));
                    }
                });
            });
        });
    }

    private boolean checkDeckLegality(DeckProxy humanDeck) {
        String errorMessage = FModel.getLimitedMatch().getDeckProblem(GameType.Draft, humanDeck.getDeck());
        if (errorMessage != null) {
            FOptionPane.showErrorDialog(Forge.getLocalizer().getMessage("lblInvalidDeckDesc").replace("%n", errorMessage), Forge.getLocalizer().getMessage("lblInvalidDeck"));
            return false;
        }
        return true;
    }
}
