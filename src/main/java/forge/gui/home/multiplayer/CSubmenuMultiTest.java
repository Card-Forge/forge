package forge.gui.home.multiplayer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import forge.AllZone;
import forge.Command;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.game.GameNew;
import forge.game.GameType;
import forge.game.PlayerStartsGame;
import forge.game.player.PlayerType;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.gui.match.CMatchUI;

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
        final int numFields;
        final int numHands;

        for (JRadioButton rad : view.getFieldRadios()) {
            if (rad.isSelected()) { radTemp = rad; break; } }

        numFields = (radTemp == null ? 2 : Integer.valueOf(radTemp.getText()));

        radTemp = null;
        for (JRadioButton rad : view.getHandRadios()) {
            if (rad.isSelected()) { radTemp = rad; break; } }

        numHands = (radTemp == null ? 1 : Integer.valueOf(radTemp.getText()));

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
                Deck humanDeck = DeckgenUtil.getRandomColorDeck(PlayerType.HUMAN);
                Deck aiDeck = DeckgenUtil.getRandomColorDeck(PlayerType.COMPUTER);

                CMatchUI.SINGLETON_INSTANCE.initMatch(numFields, numHands);
                Singletons.getModel().getMatchState().setGameType(GameType.Constructed);

                if (humanDeck != null && aiDeck != null) {
                    GameNew.newGame(new PlayerStartsGame(AllZone.getHumanPlayer(), humanDeck),
                                     new PlayerStartsGame(AllZone.getComputerPlayer(), aiDeck));
                }
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
