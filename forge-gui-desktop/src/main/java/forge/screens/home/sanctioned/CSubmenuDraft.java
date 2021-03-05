package forge.screens.home.sanctioned;

import forge.GuiBase;
import forge.Singletons;
import forge.UiCommand;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckProxy;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPoolType;
import forge.gamemodes.match.HostedMatch;
import forge.gui.GuiChoose;
import forge.gui.SOverlayUtils;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import com.google.common.collect.Lists;

/**
 * Controls the draft submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
@SuppressWarnings("serial")
public enum CSubmenuDraft implements ICDoc {
    SINGLETON_INSTANCE;

    private final UiCommand cmdDeckSelect = new UiCommand() {
        @Override
        public void run() {
            VSubmenuDraft.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);
            fillOpponentComboBox();
        }
    };

    private final ActionListener radioAction = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            fillOpponentComboBox();
        }
    };

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        final VSubmenuDraft view = VSubmenuDraft.SINGLETON_INSTANCE;

        view.getLstDecks().setSelectCommand(cmdDeckSelect);

        view.getBtnBuildDeck().setCommand(new UiCommand() {
            @Override
            public void run() {
                setupDraft();
            }
        });

        view.getBtnStart().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                startGame(GameType.Draft);
            }
        });

        view.getRadSingle().addActionListener(radioAction);

        view.getRadAll().addActionListener(radioAction);
        view.getRadMultiple().addActionListener(radioAction);
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
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

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (btnStart.isEnabled()) {
                    view.getBtnStart().requestFocusInWindow();
                } else {
                    view.getBtnBuildDeck().requestFocusInWindow();
                }
            }
        });
    }

    private void startGame(final GameType gameType) {
        final Localizer localizer = Localizer.getInstance();
        final boolean gauntlet = VSubmenuDraft.SINGLETON_INSTANCE.isGauntlet();
        final DeckProxy humanDeck = VSubmenuDraft.SINGLETON_INSTANCE.getLstDecks().getSelectedItem();

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

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        List<Deck> aiDecks = Lists.newArrayList();
        if (VSubmenuDraft.SINGLETON_INSTANCE.isSingleSelected()) {
            // Restore Zero Indexing
            final int aiIndex = Integer.parseInt(duelType)-1;
            final Deck aiDeck = opponentDecks.getAiDecks().get(aiIndex);
            if (aiDeck == null) {
                throw new IllegalStateException("Draft: Computer deck is null!");
            }
            aiDecks.add(aiDeck);
        } else {
            final int numOpponents = Integer.parseInt(duelType);

            List<Deck> randomOpponents = Lists.newArrayList(opponentDecks.getAiDecks());
            Collections.shuffle(randomOpponents);
            aiDecks = randomOpponents.subList(0, numOpponents);
            for(Deck d : aiDecks) {
                if (d == null) {
                    throw new IllegalStateException("Draft: Computer deck is null!");
                }
            }
        }

        final List<RegisteredPlayer> starter = new ArrayList<>();
        final RegisteredPlayer human = new RegisteredPlayer(humanDeck.getDeck()).setPlayer(GamePlayerUtil.getGuiPlayer());
        starter.add(human);
        for(Deck aiDeck : aiDecks) {
            starter.add(new RegisteredPlayer(aiDeck).setPlayer(GamePlayerUtil.createAiPlayer()));
        }
        for (final RegisteredPlayer pl : starter) {
            pl.assignConspiracies();
        }

        final HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
        hostedMatch.startMatch(GameType.Draft, null, starter, human, GuiBase.getInterface().getNewGuiGame());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.hideOverlay();
            }
        });
    }

    /** */
    private void setupDraft() {
        final Localizer localizer = Localizer.getInstance();
        // Determine what kind of booster draft to run
        final LimitedPoolType poolType = GuiChoose.oneOrNone(localizer.getMessage("lblChooseDraftFormat"), LimitedPoolType.values());
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

        if (VSubmenuDraft.SINGLETON_INSTANCE.isSingleSelected()) {
            // Single opponent
            final DeckGroup opponentDecks = FModel.getDecks().getDraft().get(humanDeck.getName());
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
            combo.addItem("2");
            combo.addItem("3");
            combo.addItem("4");
            combo.addItem("5");
        }
    }

}
