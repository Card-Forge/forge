package forge.screens.home.sanctioned;

import forge.GuiBase;
import forge.UiCommand;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gui.GuiChoose;
import forge.gui.SOverlayUtils;
import forge.deck.DeckProxy;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.itemmanager.ItemManagerConfig;
import forge.limited.BoosterDraft;
import forge.limited.LimitedPoolType;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorDraftingProcess;
import forge.toolbox.FOptionPane;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/** 
 * Controls the draft submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuDraft implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final UiCommand cmdDeckSelect = new UiCommand() {
        @Override
        public void run() {
            VSubmenuDraft.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);
        }
    };

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
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final VSubmenuDraft view = VSubmenuDraft.SINGLETON_INSTANCE;
        final JButton btnStart = view.getBtnStart();

        view.getLstDecks().setPool(DeckProxy.getDraftDecks(FModel.getDecks().getDraft()));
        view.getLstDecks().setup(ItemManagerConfig.DRAFT_DECKS);

        if (!view.getLstDecks().getPool().isEmpty()) {
            btnStart.setEnabled(true);
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
        final boolean gauntlet = !VSubmenuDraft.SINGLETON_INSTANCE.isSingleSelected();
        final DeckProxy humanDeck = VSubmenuDraft.SINGLETON_INSTANCE.getLstDecks().getSelectedItem();
        final int aiIndex = (int) Math.floor(Math.random() * 7);

        if (humanDeck == null) {
            FOptionPane.showErrorDialog("No deck selected for human.\n(You may need to build a new deck)", "No Deck");
            return;
        }

        if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            String errorMessage = gameType.getDeckFormat().getDeckConformanceProblem(humanDeck.getDeck());
            if (null != errorMessage) {
                FOptionPane.showErrorDialog("Your deck " + errorMessage + " Please edit or choose a different deck.", "Invalid Deck");
                return;
            }
        }

        FModel.getGauntletMini().resetGauntletDraft();

        if (gauntlet) {
            int rounds = FModel.getDecks().getDraft().get(humanDeck.getName()).getAiDecks().size();
            FModel.getGauntletMini().launch(rounds, humanDeck.getDeck(), gameType);
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        DeckGroup opponentDecks = FModel.getDecks().getDraft().get(humanDeck.getName());
        Deck aiDeck = opponentDecks.getAiDecks().get(aiIndex);
        if (aiDeck == null) {
            throw new IllegalStateException("Draft: Computer deck is null!");
        }

        List<RegisteredPlayer> starter = new ArrayList<RegisteredPlayer>();
        starter.add(new RegisteredPlayer(humanDeck.getDeck()).setPlayer(GuiBase.getInterface().getGuiPlayer()));
        starter.add(new RegisteredPlayer(aiDeck).setPlayer(GuiBase.getInterface().createAiPlayer()));
        for (RegisteredPlayer pl : starter) {
            pl.assignConspiracies();
        }

        GuiBase.getInterface().startMatch(GameType.Draft, starter);
    }

    /** */
    private void setupDraft() {
        // Determine what kind of booster draft to run
        final LimitedPoolType poolType = GuiChoose.oneOrNone("Choose Draft Format", LimitedPoolType.values());
        if (poolType == null) { return; }

        BoosterDraft draft = BoosterDraft.createDraft(poolType);
        if (draft == null) { return; }

        final CEditorDraftingProcess draftController = new CEditorDraftingProcess();
        draftController.showGui(draft);

        Singletons.getControl().setCurrentScreen(FScreen.DRAFTING_PROCESS);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(draftController);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }
}
