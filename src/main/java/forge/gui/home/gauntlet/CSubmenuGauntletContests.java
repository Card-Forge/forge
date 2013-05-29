package forge.gui.home.gauntlet;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JList;
import javax.swing.SwingUtilities;

import forge.Command;
import forge.FThreads;
import forge.Singletons;
import forge.control.Lobby;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.game.GameType;
import forge.game.Match;
import forge.game.RegisteredPlayer;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.model.FModel;

/** 
 * Controls the "gauntlet contests" submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */

@SuppressWarnings("serial")
public enum CSubmenuGauntletContests implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final VSubmenuGauntletContests view = VSubmenuGauntletContests.SINGLETON_INSTANCE;

    private final MouseAdapter madDecklist = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.getClickCount() == 2) {
                DeckgenUtil.showDecklist(((JList) e.getSource())); }
        }
    };

    private final ActionListener actStartGame = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            startGame();
        }
    };

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { view.getBtnStart().requestFocusInWindow(); }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void initialize() {
        view.getBtnStart().addActionListener(actStartGame);
        view.getLstDecks().addMouseListener(madDecklist);

        view.getLstDecks().initialize();
        updateData();

        view.getGauntletLister().setSelectedIndex(0);
    }


    private void updateData() {
        final File[] files = GauntletIO.getGauntletFilesLocked();
        final List<GauntletData> data = new ArrayList<GauntletData>();
        for (final File f : files) {
            data.add(GauntletIO.loadGauntlet(f));
        }

        view.getGauntletLister().setGauntlets(data);
        view.getGauntletLister().setSelectedIndex(0);
    }

    /** */
    private void startGame() {
        final GauntletData gd = view.getGauntletLister().getSelectedGauntlet();
        final Deck userDeck;

        if (gd.getUserDeck() != null) {
            userDeck = gd.getUserDeck();
        }
        else {
            userDeck = view.getLstDecks().getDeck().getOriginalDeck();
            gd.setUserDeck(userDeck);
        }

        gd.stamp();
        FModel.SINGLETON_INSTANCE.setGauntletData(gd);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        Deck aiDeck = gd.getDecks().get(gd.getCompleted());

        List<RegisteredPlayer> starter = new ArrayList<RegisteredPlayer>();
        Lobby lobby = Singletons.getControl().getLobby();

        starter.add(RegisteredPlayer.fromDeck(gd.getUserDeck()).setPlayer(lobby.getGuiPlayer()));
        starter.add(RegisteredPlayer.fromDeck(aiDeck).setPlayer(lobby.getAiPlayer()));

        final Match mc = new Match(GameType.Gauntlet, starter);
        FThreads.invokeInEdtLater(new Runnable(){
            @Override
            public void run() {
                mc.startRound();
                SOverlayUtils.hideOverlay();
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return new Command() {
            @Override
            public void run() {
                updateData();
            }
        };
    }
}
