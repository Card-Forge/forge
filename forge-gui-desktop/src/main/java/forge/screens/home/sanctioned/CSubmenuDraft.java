package forge.screens.home.sanctioned;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckProxy;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPoolType;
import forge.gamemodes.match.HostedMatch;
import forge.gui.GuiBase;
import forge.gui.GuiChoose;
import forge.gui.SOverlayUtils;
import forge.gui.UiCommand;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.itemmanager.ItemManagerConfig;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorDraftingProcess;
import forge.screens.deckeditor.views.VProbabilities;
import forge.screens.deckeditor.views.VStatistics;
import forge.toolbox.FOptionPane;
import forge.util.Localizer;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

        if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            final String errorMessage = gameType.getDeckFormat().getDeckConformanceProblem(humanDeck.getDeck());
            if (null != errorMessage) {
                FOptionPane.showErrorDialog("Your deck " + errorMessage + " Please edit or choose a different deck.", "Invalid Deck");
                return;
            }
        }

        FModel.getGauntletMini().resetGauntletDraft();
        String duelType = (String)VSubmenuDraft.SINGLETON_INSTANCE.getCbOpponent().getSelectedItem();

        if (duelType == null) {
            FOptionPane.showErrorDialog("Please select duel types for the draft match.", "Missing opponent items");
            return;
        }

        final DeckGroup opponentDecks = FModel.getDecks().getDraft().get(humanDeck.getName());
        if (gauntlet) {
            if ("Gauntlet".equals(duelType)) {
                final int rounds = opponentDecks.getAiDecks().size();
                FModel.getGauntletMini().launch(rounds, humanDeck.getDeck(), gameType);
            } else if ("Tournament".equals(duelType)) {
                // TODO Allow for tournament style draft, instead of always a gauntlet
            }
            return;
        }

        SwingUtilities.invokeLater(() -> {
            SOverlayUtils.startGameOverlay();
            SOverlayUtils.showOverlay();
        });

        Map<Integer, Deck> aiMap = Maps.newHashMap();
        if (VSubmenuDraft.SINGLETON_INSTANCE.isSingleSelected()) {
            // Restore Zero Indexing
            final int aiIndex = Integer.parseInt(duelType)-1;
            final Deck aiDeck = opponentDecks.getAiDecks().get(aiIndex);
            if (aiDeck == null) {
                throw new IllegalStateException("Draft: Computer deck is null!");
            }

            aiMap.put(aiIndex + 1, aiDeck);
        } else {
            final int numOpponents = Integer.parseInt(duelType);

            int maxDecks = opponentDecks.getAiDecks().size();
            if (numOpponents > maxDecks) {
                throw new IllegalStateException("Draft: Not enough decks for the number of opponents!");
            }

            List<Integer> aiIndices = Lists.newArrayList();
            for(int i = 0; i < maxDecks; i++) {
                aiIndices.add(i);
            }
            Collections.shuffle(aiIndices);
            aiIndices = aiIndices.subList(0, numOpponents);

            for(int i : aiIndices) {
                final Deck aiDeck = opponentDecks.getAiDecks().get(i);
                if (aiDeck == null) {
                    throw new IllegalStateException("Draft: Computer deck is null!");
                }

                aiMap.put(i + 1, aiDeck);
            }
        }

        final List<RegisteredPlayer> starter = new ArrayList<>();
        // Human is 0
        final RegisteredPlayer human = new RegisteredPlayer(humanDeck.getDeck()).setPlayer(GamePlayerUtil.getGuiPlayer());
        starter.add(human);
        human.setId(0);
        human.assignConspiracies();
        for(Map.Entry<Integer, Deck> aiDeck : aiMap.entrySet()) {
            RegisteredPlayer aiPlayer = new RegisteredPlayer(aiDeck.getValue()).setPlayer(GamePlayerUtil.createAiPlayer());
            aiPlayer.setId(aiDeck.getKey());
            starter.add(aiPlayer);
            aiPlayer.assignConspiracies();
        }

        final HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
        hostedMatch.startMatch(GameType.Draft, null, starter, human, GuiBase.getInterface().getNewGuiGame());

        SwingUtilities.invokeLater(SOverlayUtils::hideOverlay);
    }

    /** */
    private void setupDraft() {
        final Localizer localizer = Localizer.getInstance();
        // Determine what kind of booster draft to run
        final LimitedPoolType poolType = GuiChoose.oneOrNone(localizer.getMessage("lblChooseDraftFormat"), LimitedPoolType.values(true));
        if (poolType == null) { return; }

        final BoosterDraft draft = BoosterDraft.createDraft(poolType);
        if (draft == null) { return; }

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
