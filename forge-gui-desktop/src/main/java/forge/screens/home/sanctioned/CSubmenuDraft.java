package forge.screens.home.sanctioned;

import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckProxy;
import forge.game.GameType;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedMatch;
import forge.gamemodes.limited.LimitedPoolType;
import forge.gui.GuiChoose;
import forge.gui.SOverlayUtils;
import forge.gui.UiCommand;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorDraftingProcess;
import forge.screens.deckeditor.views.VProbabilities;
import forge.screens.deckeditor.views.VStatistics;
import forge.screens.home.DraftPicksSelector;
import forge.toolbox.FOptionPane;
import forge.util.Localizer;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controls the draft submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
@SuppressWarnings("serial")
public enum CSubmenuDraft implements ICDoc {
    SINGLETON_INSTANCE;

    private final UiCommand cmdDeckSelect = () -> {
        VSubmenuDraft.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);
        fillOpponentComboBox();
    };

    private final ActionListener radioAction = e -> fillOpponentComboBox();

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        final VSubmenuDraft view = VSubmenuDraft.SINGLETON_INSTANCE;

        view.getLstDecks().setSelectCommand(cmdDeckSelect);

        view.getBtnBuildDeck().setCommand((UiCommand) this::setupDraft);

        view.getBtnStart().addActionListener(e -> startGame(GameType.Draft));

        view.getRadSingle().addActionListener(radioAction);

        view.getRadAll().addActionListener(radioAction);
        view.getRadMultiple().addActionListener(radioAction);
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final VSubmenuDraft view = VSubmenuDraft.SINGLETON_INSTANCE;
        final JButton btnStart = view.getBtnStart();

        view.getLstDecks().setPool(DeckProxy.getAllDraftDecks());
        view.getLstDecks().setup(ItemManagerConfig.DRAFT_DECKS);

        if (!view.getLstDecks().getPool().isEmpty()) {
            btnStart.setEnabled(true);
            fillOpponentComboBox();
        }

        SwingUtilities.invokeLater(() -> {
            if (btnStart.isEnabled()) {
                view.getBtnStart().requestFocusInWindow();
            } else {
                view.getBtnBuildDeck().requestFocusInWindow();
            }
        });

        view.getGamesInMatchBinder().load();
    }

    private void startGame(final GameType gameType) {
        final Localizer localizer = Localizer.getInstance();
        final VSubmenuDraft view = VSubmenuDraft.SINGLETON_INSTANCE;
        final boolean gauntlet = view.isGauntlet();
        final DeckProxy humanDeck = view.getLstDecks().getSelectedItem();

        if (humanDeck == null) {
            FOptionPane.showErrorDialog(localizer.getMessage("lblNoDeckSelected"), localizer.getMessage("lblNoDeck"));
            return;
        }

        final LimitedMatch limitedMatch = FModel.getLimitedMatch();
        final String errorMessage = limitedMatch.getDeckProblem(gameType, humanDeck.getDeck());
        if (null != errorMessage) {
            FOptionPane.showErrorDialog("Your deck " + errorMessage + " Please edit or choose a different deck.", "Invalid Deck");
            return;
        }

        String duelType = (String)VSubmenuDraft.SINGLETON_INSTANCE.getCbOpponent().getSelectedItem();

        if (duelType == null) {
            FOptionPane.showErrorDialog("Please select duel types for the draft match.", "Missing opponent items");
            return;
        }

        final List<Deck> aiDecks = limitedMatch.getAiDecks(gameType, humanDeck.getDeck());
        if (gauntlet) {
            if ("Gauntlet".equals(duelType)) {
                limitedMatch.launchGauntlet(gameType, humanDeck.getDeck(), aiDecks.size());
            } else if ("Tournament".equals(duelType)) {
                // TODO Allow for tournament style draft, instead of always a gauntlet
                // Most of the time this will be a single elimination bracket
                // But for Commander and Conspiracy its two 4 player matches, into a head to head finals
            }
            return;
        }

        SwingUtilities.invokeLater(() -> {
            SOverlayUtils.startGameOverlay();
            SOverlayUtils.showOverlay();
        });

        final List<Deck> opponents;
        if (VSubmenuDraft.SINGLETON_INSTANCE.isSingleSelected()) {
            opponents = List.of(aiDecks.get(Integer.parseInt(duelType) - 1));
        } else {
            final List<Deck> shuffled = new ArrayList<>(aiDecks);
            Collections.shuffle(shuffled);
            opponents = shuffled.subList(0, Integer.parseInt(duelType));
        }
        limitedMatch.startMatch(gameType, humanDeck.getDeck(), opponents);

        SwingUtilities.invokeLater(SOverlayUtils::hideOverlay);
    }

    /** */
    private void setupDraft() {
        final Localizer localizer = Localizer.getInstance();
        // Determine what kind of booster draft to run
        final LimitedPoolType poolType = GuiChoose.oneOrNone(localizer.getMessage("lblChooseDraftFormat"), LimitedPoolType.values(true));
        if (poolType == null) { return; }

        final BoosterDraft draft = BoosterDraft.createDraft(poolType);
        if (draft == null || !DraftPicksSelector.showDialog(draft)) { return; }

        final CEditorDraftingProcess draftController = new CEditorDraftingProcess(CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());
        draftController.showGui(draft);

        Singletons.getControl().setCurrentScreen(FScreen.DRAFTING_PROCESS);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(draftController);
        VProbabilities.SINGLETON_INSTANCE.getLayoutControl().update();
        VStatistics.SINGLETON_INSTANCE.getLayoutControl().update();

    }

    private void fillOpponentComboBox() {
        final VSubmenuDraft view = VSubmenuDraft.SINGLETON_INSTANCE;
        JComboBox<String> combo = view.getCbOpponent();
        combo.removeAllItems();

        final DeckProxy humanDeck = view.getLstDecks().getSelectedItem();
        if (humanDeck == null) {
            return;
        }

        final DeckGroup opponentDecks = FModel.getDecks().getDraft().get(humanDeck.getName());
        if (VSubmenuDraft.SINGLETON_INSTANCE.isSingleSelected()) {
            // Single opponent
            int indx = 0;
            for (@SuppressWarnings("unused") Deck d : opponentDecks.getAiDecks()) {
                indx++;
                // 1-7 instead of 0-6
                combo.addItem(String.valueOf(indx));
            }
        } else if (VSubmenuDraft.SINGLETON_INSTANCE.isGauntlet()) {
            // Gauntlet/Tournament
            combo.addItem("Gauntlet");
            //combo.addItem("Tournament");
        } else {
            int size = opponentDecks.getAiDecks().size();
            combo.addItem("2");
            if (size > 2) {
                combo.addItem("3");
            }

            if (size >= 4) {
                combo.addItem("4");
                combo.addItem("5");
            }
        }
    }
}
