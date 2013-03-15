package forge.gui.home.gauntlet;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import forge.Command;
import forge.Singletons;
import forge.control.Lobby;
import forge.deck.Deck;
import forge.game.GameType;
import forge.game.MatchController;
import forge.game.MatchStartHelper;
import forge.game.player.PlayerType;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.gui.home.VHomeUI;
import forge.model.FModel;

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
                } else {
                    VHomeUI.SINGLETON_INSTANCE.getLblEditor().requestFocusInWindow();
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

        view.getGauntletLister().setCmdDelete(new Command() { @Override
            public void execute() { enableStartButton(); } });
        view.getGauntletLister().setCmdSelect(new Command() { @Override
            public void execute() { enableStartButton(); } });
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
        FModel.SINGLETON_INSTANCE.setGauntletData(
                GauntletIO.loadGauntlet(VSubmenuGauntletQuick.SINGLETON_INSTANCE.getGauntletLister().getSelectedGauntletFile()));

        // Start game
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        final SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {
            @Override
            public Object doInBackground() {
                final GauntletData gd = FModel.SINGLETON_INSTANCE.getGauntletData();
                final Deck aiDeck = gd.getDecks().get(gd.getCompleted());

                MatchStartHelper starter = new MatchStartHelper();
                Lobby lobby = Singletons.getControl().getLobby();
                starter.addPlayer(lobby.findLocalPlayer(PlayerType.HUMAN), gd.getUserDeck());
                starter.addPlayer(lobby.findLocalPlayer(PlayerType.COMPUTER), aiDeck);

                MatchController mc = Singletons.getModel().getMatch();
                mc.initMatch(GameType.Gauntlet, starter.getPlayerMap());
                mc.startRound();
                return null;
            }

            @Override
            public void done() {
                SOverlayUtils.hideOverlay();
            }
        };
        worker.execute();
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @SuppressWarnings("serial")
    @Override
    public Command getCommandOnSelect() {
        return new Command() {
            @Override
            public void execute() {
                updateData();
            }
        };
    }
}
