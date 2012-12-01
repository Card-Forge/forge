package forge.gui.home.variant;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import forge.Card;
import forge.Command;
import forge.Singletons;
import forge.control.FControl;
import forge.control.Lobby;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.game.GameType;
import forge.game.MatchController;
import forge.game.MatchStartHelper;
import forge.game.PlayerStartConditions;
import forge.game.player.PlayerType;
import forge.gui.SOverlayUtils;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorConstructed;
import forge.gui.deckeditor.controllers.CEditorScheme;
import forge.gui.framework.ICDoc;
import forge.gui.home.VHomeUI;
import forge.item.CardDb;
import forge.item.CardPrinted;

/** 
 * Controls the deck editor submenu option in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuArchenemy implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final VSubmenuArchenemy view = VSubmenuArchenemy.SINGLETON_INSTANCE;

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
        
        VSubmenuArchenemy.SINGLETON_INSTANCE.getLblEditor().setCommand(new Command() {
            @Override
            public void execute() {
                CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(new CEditorScheme());
                FControl.SINGLETON_INSTANCE.changeState(FControl.DECK_EDITOR_CONSTRUCTED);
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

                //Debugging
                List<CardPrinted> schemes = new ArrayList<CardPrinted>();
                schemes.add(CardDb.instance().getCard("A Display of My Dark Power"));
                schemes.add(CardDb.instance().getCard("Behold the Power of Destruction"));
                schemes.add(CardDb.instance().getCard("I Know All, I See All"));
                schemes.add(CardDb.instance().getCard("Look Skyward and Despair"));
                schemes.add(CardDb.instance().getCard("My Genius Knows No Bounds"));
                
                starter.addArchenemy(lobby.findLocalPlayer(PlayerType.HUMAN), humanDeck, schemes);
                
                for (int i = 0; i < numFields; i++) {
                    starter.addPlayer(lobby.findLocalPlayer(PlayerType.COMPUTER), DeckgenUtil.getRandomColorDeck(PlayerType.COMPUTER));
                }

                MatchController mc = Singletons.getModel().getMatch();
                mc.initMatch(GameType.Archenemy, starter.getPlayerMap());
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
