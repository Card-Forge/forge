package forge.screens.home.sanctioned;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import forge.Singletons;
import forge.deck.DeckProxy;
import forge.game.GameType;
import forge.gamemodes.limited.LimitedMatch;
import forge.gamemodes.limited.LimitedPoolType;
import forge.gamemodes.limited.WinstonDraft;
import forge.gui.GuiChoose;
import forge.gui.SOverlayUtils;
import forge.gui.UiCommand;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorWinstonProcess;
import forge.toolbox.FOptionPane;

/**
 * Controls the draft submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuWinston implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final UiCommand cmdDeckSelect = () -> VSubmenuWinston.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        final VSubmenuWinston view = VSubmenuWinston.SINGLETON_INSTANCE;

        view.getLstDecks().setSelectCommand(cmdDeckSelect);

        view.getBtnBuildDeck().setCommand((UiCommand) this::setupDraft);

        view.getBtnStart().addActionListener(e -> startGame(GameType.Winston));
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final VSubmenuWinston view = VSubmenuWinston.SINGLETON_INSTANCE;
        final JButton btnStart = view.getBtnStart();

        view.getLstDecks().setPool(DeckProxy.getWinstonDecks(FModel.getDecks().getWinston()));
        view.getLstDecks().setup(ItemManagerConfig.WINSTON_DECKS);

        if (!view.getLstDecks().getPool().isEmpty()) {
            btnStart.setEnabled(true);
        }

        SwingUtilities.invokeLater(() -> {
            if (btnStart.isEnabled()) {
                view.getBtnStart().requestFocusInWindow();
            } else {
                view.getBtnBuildDeck().requestFocusInWindow();
            }
        });
    }

    private void startGame(final GameType gameType) {
        final DeckProxy humanDeck = VSubmenuWinston.SINGLETON_INSTANCE.getLstDecks().getSelectedItem();

        if (humanDeck == null) {
            FOptionPane.showErrorDialog("No deck selected for human.\n(You may need to build a new deck)", "No Deck");
            return;
        }

        final LimitedMatch limitedMatch = FModel.getLimitedMatch();
        final String errorMessage = limitedMatch.getDeckProblem(gameType, humanDeck.getDeck());
        if (null != errorMessage) {
            FOptionPane.showErrorDialog("Your deck " + errorMessage + " Please edit or choose a different deck.", "Invalid Deck");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            SOverlayUtils.startGameOverlay();
            SOverlayUtils.showOverlay();
        });

        limitedMatch.startMatch(gameType, humanDeck.getDeck(), limitedMatch.getAiDecks(gameType, humanDeck.getDeck()).subList(0, 1));

        SwingUtilities.invokeLater(SOverlayUtils::hideOverlay);
    }

    /** */
    private void setupDraft() {
        // Determine what kind of booster draft to run
        final LimitedPoolType poolType = GuiChoose.oneOrNone("Choose Draft Format", LimitedPoolType.values());
        if (poolType == null) { return; }

        final WinstonDraft draft = WinstonDraft.createDraft(poolType);
        if (draft == null) { return; }

        final CEditorWinstonProcess draftController = new CEditorWinstonProcess(CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());
        draftController.showGui(draft);

        Singletons.getControl().setCurrentScreen(FScreen.DRAFTING_PROCESS);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(draftController);
    }

}
