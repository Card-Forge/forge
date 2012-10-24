package forge.gui.home.multiplayer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import forge.Command;
import forge.Singletons;
import forge.control.Lobby;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.game.GameType;
import forge.game.MatchController;
import forge.game.MatchStartHelper;
import forge.game.player.PlayerType;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;

/** 
 * Controls the deck editor submenu option in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuMultiTest implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final VSubmenuMultiTest view = VSubmenuMultiTest.SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        view.getDcHuman().initialize();
        // Checkbox event handling
        view.getBtnStart().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                start();
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() { }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
       return null;
    }

    private void start() {
        // Retrieve selections
        JRadioButton radTemp = null;

        for (JRadioButton rad : view.getFieldRadios()) {
            if (rad.isSelected()) { radTemp = rad; break; } }

        final int numFields = (radTemp == null ? 2 : Integer.valueOf(radTemp.getText()));

        // Boilerplate start game code
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
                Deck humanDeck = view.getDcHuman().getDeck();

                MatchStartHelper starter = new MatchStartHelper();
                Lobby lobby = Singletons.getControl().getLobby();
                starter.addPlayer(lobby.findLocalPlayer(PlayerType.HUMAN), humanDeck);
                for( int i = 0; i < numFields; i++ )
                    starter.addPlayer(lobby.findLocalPlayer(PlayerType.COMPUTER), DeckgenUtil.getRandomColorDeck(PlayerType.COMPUTER));
                
                MatchController mc = Singletons.getModel().getMatch(); 
                mc.initMatch(GameType.Constructed, starter.getPlayerMap());
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
}
