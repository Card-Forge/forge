package forge.screens.home.sanctioned;

import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckProxy;
import forge.game.GameType;
import forge.gamemodes.limited.LimitedMatch;
import forge.gamemodes.limited.SealedCardPoolGenerator;
import forge.gui.SOverlayUtils;
import forge.gui.UiCommand;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorLimited;
import forge.toolbox.FOptionPane;

/**
 * Controls the sealed submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuSealed implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final UiCommand cmdDeckSelect = () -> {
        VSubmenuSealed.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);
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
        final VSubmenuSealed view = VSubmenuSealed.SINGLETON_INSTANCE;

        view.getLstDecks().setSelectCommand(cmdDeckSelect);

        view.getBtnBuildDeck().setCommand((UiCommand) this::setupSealed);

        view.getBtnStart().addActionListener(e -> startGame(GameType.Sealed));

        view.getBtnDirections().setCommand((UiCommand) view::showDirections);

        view.getRadSingle().addActionListener(radioAction);

        view.getRadAll().addActionListener(radioAction);
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final VSubmenuSealed view = VSubmenuSealed.SINGLETON_INSTANCE;
        view.getLstDecks().setPool(DeckProxy.getAllSealedDecks());
        view.getLstDecks().setup(ItemManagerConfig.SEALED_DECKS);

        SwingUtilities.invokeLater(() -> {
            final JButton btnStart = view.getBtnStart();
            if (btnStart.isEnabled()) {
                view.getBtnStart().requestFocusInWindow();
            } else {
                view.getBtnBuildDeck().requestFocusInWindow();
            }
        });
    }

    private void startGame(final GameType gameType) {
        final boolean gauntlet = !VSubmenuSealed.SINGLETON_INSTANCE.isSingleSelected();
        final DeckProxy humanDeck = VSubmenuSealed.SINGLETON_INSTANCE.getLstDecks().getSelectedItem();

        if (humanDeck == null) {
            FOptionPane.showErrorDialog("Please build and/or select a deck for yourself.", "No Deck");
            return;
        }

        final LimitedMatch limitedMatch = FModel.getLimitedMatch();
        final String errorMessage = limitedMatch.getDeckProblem(gameType, humanDeck.getDeck());
        if (null != errorMessage) {
            FOptionPane.showErrorDialog("Your deck " + errorMessage + " Please edit or choose a different deck.", "Invalid Deck");
            return;
        }

        String duelType = (String)VSubmenuSealed.SINGLETON_INSTANCE.getCbOpponent().getSelectedItem();
        final List<Deck> aiDecks = limitedMatch.getAiDecks(gameType, humanDeck.getDeck());
        if (gauntlet) {
            if ("Gauntlet".equals(duelType)) {
                limitedMatch.launchGauntlet(gameType, humanDeck.getDeck(), aiDecks.size());
            } else if ("Tournament".equals(duelType)) {
                // TODO Allow for tournament style sealed, instead of always a gauntlet
            }
            return;
        }

        SwingUtilities.invokeLater(() -> {
            SOverlayUtils.startGameOverlay();
            SOverlayUtils.showOverlay();
        });

        limitedMatch.startMatch(gameType, humanDeck.getDeck(), List.of(aiDecks.get(Integer.parseInt(duelType) - 1)));

        SwingUtilities.invokeLater(SOverlayUtils::hideOverlay);
    }

    private void setupSealed() {
        final DeckGroup sealed = SealedCardPoolGenerator.generateSealedDeck(false);
        if (sealed == null) { return; }

        final CEditorLimited<DeckGroup> editor = new CEditorLimited<>(
                FModel.getDecks().getSealed(), DeckGroup::new, FScreen.DECK_EDITOR_SEALED, CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());

        Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_SEALED);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(editor);
        editor.getDeckController().setModel(sealed);
    }

    private void fillOpponentComboBox() {
        final VSubmenuSealed view = VSubmenuSealed.SINGLETON_INSTANCE;
        JComboBox<String> combo = view.getCbOpponent();
        combo.removeAllItems();

        final DeckProxy humanDeck = view.getLstDecks().getSelectedItem();

        if (humanDeck == null) {
            return;
        }

        if (VSubmenuSealed.SINGLETON_INSTANCE.isSingleSelected()) {
            // Single opponent
            final DeckGroup opponentDecks = FModel.getDecks().getSealed().get(humanDeck.getName());
            int indx = 0;
            for (@SuppressWarnings("unused") Deck d : opponentDecks.getAiDecks()) {
                indx++;
                // 1-7 instead of 0-6
                combo.addItem(String.valueOf(indx));
            }
        } else {
            // Gauntlet/Tournament
            combo.addItem("Gauntlet");
            //combo.addItem("Tournament");
        }
    }

}
