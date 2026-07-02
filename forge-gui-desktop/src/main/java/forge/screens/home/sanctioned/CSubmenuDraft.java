package forge.screens.home.sanctioned;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;

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
        view.getRadGauntlet().addActionListener(radioAction);
        view.getBtnGauntletOptions().addActionListener(e -> {
            final CGauntletOptionsDialog.GauntletOptions opts = CGauntletOptionsDialog.SINGLETON_INSTANCE.showDialog();
            if (opts != null) {
                // Persist preferred games-per-match
                FModel.getPreferences().setPref(FPref.UI_MATCHES_PER_GAME, String.valueOf(opts.gamesPerMatch));
                // Update UI binder
                view.getGamesInMatchBinder().load();
            }
        });
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
        final boolean gauntlet = view.isGauntlet() || view.isLimitedGauntletSelected();
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
        final String duelType = (String)VSubmenuDraft.SINGLETON_INSTANCE.getCbOpponent().getSelectedItem();

        if (duelType == null) {
            FOptionPane.showErrorDialog("Please select duel types for the draft match.", "Missing opponent items");
            return;
        }

        final DeckGroup opponentDecks = FModel.getDecks().getDraft().get(humanDeck.getName());
        if (gauntlet) {
            startGauntlet(view, opponentDecks, humanDeck, gameType, duelType);
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

    private void startGauntlet(final VSubmenuDraft view, final DeckGroup opponentDecks,
            final DeckProxy humanDeck, final GameType gameType, final String duelType) {
        final int maxRounds = opponentDecks.getAiDecks().size();
        final int configuredRounds = getConfiguredGauntletRounds(maxRounds);
        final int rounds = view.isLimitedGauntletSelected()
            ? Math.min(parseGauntletRounds(duelType, opponentDecks), configuredRounds)
            : configuredRounds;
        FModel.getGauntletMini().launch(rounds, humanDeck.getDeck(), gameType);
    }

    private int getConfiguredGauntletRounds(final int maxRounds) {
        final String savedRounds = FModel.getPreferences().getPref(FPref.UI_GAUNTLET_ROUNDS);
        try {
            final int parsedRounds = Integer.parseInt(savedRounds);
            return Math.max(1, Math.min(parsedRounds, maxRounds));
        } catch (final NumberFormatException e) {
            return Math.min(4, maxRounds);
        }
    }

    private int parseGauntletRounds(final String duelType, final DeckGroup opponentDecks) {
        final int rounds = Integer.parseInt(duelType);
        final int maxRounds = opponentDecks.getAiDecks().size();
        if (rounds < 1 || rounds > maxRounds) {
            throw new IllegalStateException("Draft: Invalid gauntlet round count!");
        }
        return rounds;
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
        final Localizer localizer = Localizer.getInstance();

        final DeckProxy humanDeck = view.getLstDecks().getSelectedItem();
        final String placeholder = localizer.getMessage("lblSelectOpponentPlaceholder");
        // Set a renderer that displays placeholder text in gray when appropriate
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                final JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                    lbl.setText(placeholder);
                    lbl.setForeground(Color.GRAY);
                } else if (!combo.isEnabled()) {
                    lbl.setForeground(Color.GRAY);
                } else {
                    lbl.setForeground(null);
                }
                return lbl;
            }
        });

        if (humanDeck == null) {
            combo.addItem("");
            combo.setEnabled(false);
            combo.setToolTipText(placeholder);
            return;
        }

        final DeckGroup opponentDecks = FModel.getDecks().getDraft().get(humanDeck.getName());
        if (opponentDecks == null || opponentDecks.getAiDecks().isEmpty()) {
            combo.addItem("");
            combo.setEnabled(false);
            combo.setToolTipText(localizer.getMessage("lblNoDeck"));
            return;
        }
        if (VSubmenuDraft.SINGLETON_INSTANCE.isSingleSelected()) {
            fillSingleOpponentChoices(combo, opponentDecks);
            return;
        }

        if (VSubmenuDraft.SINGLETON_INSTANCE.isLimitedGauntletSelected()) {
            fillLimitedGauntletChoices(combo, opponentDecks);
            return;
        }

        if (VSubmenuDraft.SINGLETON_INSTANCE.isGauntlet()) {
            fillFullGauntletChoices(combo);
            return;
        }

        fillMultipleOpponentChoices(combo, opponentDecks);
        // ensure combo enabled and select a sensible default when items were added
        if (combo.getItemCount() > 0) {
            combo.setEnabled(true);
            if (combo.getSelectedItem() == null) {
                combo.setSelectedIndex(0);
            }
            combo.setToolTipText(null);
        } else {
            combo.setEnabled(false);
            combo.setToolTipText(placeholder);
        }
    }

    private void fillSingleOpponentChoices(final JComboBox<String> combo, final DeckGroup opponentDecks) {
        int index = 0;
        for (@SuppressWarnings("unused") Deck deck : opponentDecks.getAiDecks()) {
            index++;
            combo.addItem(String.valueOf(index));
        }
    }

    private void fillLimitedGauntletChoices(final JComboBox<String> combo, final DeckGroup opponentDecks) {
        final int rounds = opponentDecks.getAiDecks().size();
        for (int round = 1; round <= rounds; round++) {
            combo.addItem(String.valueOf(round));
        }

        final int preferredRounds = Math.min(4, rounds);
        combo.setSelectedItem(String.valueOf(preferredRounds));
    }

    private void fillFullGauntletChoices(final JComboBox<String> combo) {
        combo.addItem("Gauntlet");
        // combo.addItem("Tournament");
    }

    private void fillMultipleOpponentChoices(final JComboBox<String> combo, final DeckGroup opponentDecks) {
        final int size = opponentDecks.getAiDecks().size();
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
