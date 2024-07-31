package forge.screens.home.gauntlet;

import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import forge.deck.Deck;
import forge.deck.DeckType;
import forge.deckchooser.FDeckChooser;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.gauntlet.GauntletData;
import forge.gamemodes.gauntlet.GauntletIO;
import forge.gui.SOverlayUtils;
import forge.gui.UiCommand;
import forge.gui.framework.ICDoc;
import forge.model.FModel;
import forge.player.GamePlayerUtil;

/**
 * Controls the "quick gauntlet" submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuGauntletLoad implements ICDoc {
    SINGLETON_INSTANCE;

    private final ActionListener actStartGame = arg0 -> startGame();

    private final VSubmenuGauntletLoad view = VSubmenuGauntletLoad.SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {
        updateData();
        enableStartButton();

        view.getGauntletLister().setSelectedIndex(0);

        SwingUtilities.invokeLater(() -> {
            final JButton btnStart = view.getBtnStart();
            if (btnStart.isEnabled()) {
                view.getBtnStart().requestFocusInWindow();
            }
        });
    }

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        view.getBtnStart().addActionListener(actStartGame);

        view.getGauntletLister().setCmdDelete((UiCommand) this::enableStartButton);
        view.getGauntletLister().setCmdSelect((UiCommand) this::enableStartButton);
        view.getGauntletLister().setCmdActivate((UiCommand) this::startGame);
    }

    private void updateData() {
        final File[] files = GauntletIO.getGauntletFilesUnlocked(null);
        final List<GauntletData> data = new ArrayList<>();

        for (final File f : files) {
            final GauntletData gd = GauntletIO.loadGauntlet(f);
            if (gd != null) {
                data.add(gd);
            }
        }

        view.getGauntletLister().setGauntlets(data);
    }

    private void enableStartButton() {
        if (view.getGauntletLister().getSelectedGauntletFile() == null) {
            view.getBtnStart().setEnabled(false);
        }
        else {
            view.getBtnStart().setEnabled(true);
        }
    }

    private void startGame() {
        final GauntletData gd = GauntletIO.loadGauntlet(view.getGauntletLister().getSelectedGauntletFile());
        if (gd == null) { return; }

        FModel.setGauntletData(gd);
        final Deck aiDeck = gd.getDecks().get(gd.getCompleted());
        Deck userDeck = gd.getUserDeck();
        if (userDeck == null) {
            //give user a chance to select a deck if none saved with gauntlet
            userDeck = FDeckChooser.promptForDeck(null, "Select a deck to play for this gauntlet", gd.isCommanderGauntlet()
                    ? DeckType.COMMANDER_DECK : DeckType.CUSTOM_DECK, false);
            if (userDeck == null) { return; } //prevent crash if user doesn't select a deck
            gd.setUserDeck(userDeck);
            GauntletIO.saveGauntlet(gd);
            view.getGauntletLister().refresh();
        }

        // Start game
        SwingUtilities.invokeLater(() -> {
            SOverlayUtils.startGameOverlay();
            SOverlayUtils.showOverlay();
        });

        final List<RegisteredPlayer> starter = new ArrayList<>();
        final RegisteredPlayer human = gd.isCommanderGauntlet()
                ? RegisteredPlayer.forCommander(userDeck).setPlayer(GamePlayerUtil.getGuiPlayer())
                : new RegisteredPlayer(userDeck).setPlayer(GamePlayerUtil.getGuiPlayer());
        starter.add(human);
        starter.add(gd.isCommanderGauntlet()
                ? RegisteredPlayer.forCommander(aiDeck).setPlayer(GamePlayerUtil.createAiPlayer())
                : new RegisteredPlayer(aiDeck).setPlayer(GamePlayerUtil.createAiPlayer()));

        gd.startRound(starter, human);

        SwingUtilities.invokeLater(SOverlayUtils::hideOverlay);
    }

}
