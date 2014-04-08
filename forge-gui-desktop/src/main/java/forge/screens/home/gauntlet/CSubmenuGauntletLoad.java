package forge.screens.home.gauntlet;

import forge.UiCommand;
import forge.Singletons;
import forge.deck.Deck;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.model.FModel;
import forge.net.FServer;
import forge.net.Lobby;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 
 * Controls the "quick gauntlet" submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuGauntletLoad implements ICDoc {
    SINGLETON_INSTANCE;

    private final ActionListener actStartGame = new ActionListener() { @Override
        public void actionPerformed(ActionEvent arg0) { startGame(); } };

    private final VSubmenuGauntletLoad view = VSubmenuGauntletLoad.SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {
        updateData();
        enableStartButton();

        view.getGauntletLister().setSelectedIndex(0);
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                JButton btnStart = view.getBtnStart();
                if (btnStart.isEnabled()) {
                    view.getBtnStart().requestFocusInWindow();
                }
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        view.getBtnStart().addActionListener(actStartGame);

        view.getGauntletLister().setCmdDelete(new UiCommand() { @Override
            public void run() { enableStartButton(); } });
        view.getGauntletLister().setCmdSelect(new UiCommand() { @Override
            public void run() { enableStartButton(); } });
    }

    private void updateData() {
        final File[] files = GauntletIO.getGauntletFilesUnlocked();
        final List<GauntletData> data = new ArrayList<GauntletData>();

        for (final File f : files) {
            data.add(GauntletIO.loadGauntlet(f));
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
        FModel.setGauntletData(
                GauntletIO.loadGauntlet(view.getGauntletLister().getSelectedGauntletFile()));

        // Start game
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        final GauntletData gd = FModel.getGauntletData();
        final Deck aiDeck = gd.getDecks().get(gd.getCompleted());

        List<RegisteredPlayer> starter = new ArrayList<RegisteredPlayer>();
        Lobby lobby = FServer.getLobby();
        starter.add(new RegisteredPlayer(gd.getUserDeck()).setPlayer(lobby.getGuiPlayer()));
        starter.add(new RegisteredPlayer(aiDeck).setPlayer(lobby.getAiPlayer()));
        
        Singletons.getControl().startMatch(GameType.Gauntlet, starter);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @SuppressWarnings("serial")
    @Override
    public UiCommand getCommandOnSelect() {
        return new UiCommand() {
            @Override
            public void run() {
                updateData();
            }
        };
    }
}
