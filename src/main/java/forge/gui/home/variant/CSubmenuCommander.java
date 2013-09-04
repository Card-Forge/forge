package forge.gui.home.variant;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import forge.Command;
import forge.FThreads;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.Deck;
import forge.game.GameType;
import forge.game.Match;
import forge.game.RegisteredPlayer;
import forge.game.player.LobbyPlayer;
import forge.gui.GuiDialog;
import forge.gui.SOverlayUtils;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorCommander;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.FList;
import forge.net.FServer;
import forge.net.Lobby;
import forge.util.MyRandom;

/** 
 * Controls the commander submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuCommander implements ICDoc {
    /** */
    SINGLETON_INSTANCE;
    private final VSubmenuCommander view = VSubmenuCommander.SINGLETON_INSTANCE;


    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {
     // reinit deck lists and restore last selections (if any)
        for (FList<Object> deckList : view.getDeckLists()) {
            Vector<Object> listData = new Vector<Object>();            
            
            if(Singletons.getModel().getDecks().getCommander().size() != 0) {
                listData.add("Random");
                for (Deck commanderDeck : Singletons.getModel().getDecks().getCommander()) {
                    listData.add(commanderDeck);
                }                
            }
            
            Object val = deckList.getSelectedValue();
            deckList.setListData(listData);
            if (null != val) {
                deckList.setSelectedValue(val, true);
            }
            
            if (-1 == deckList.getSelectedIndex() && listData.size() != 0) {
                deckList.setSelectedIndex(0);
            }
            
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { view.getBtnStart().requestFocusInWindow(); }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void initialize() {

        // Checkbox event handling
        view.getBtnStart().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {

                startGame();
            }
        });

        VSubmenuCommander.SINGLETON_INSTANCE.getLblEditor().setCommand(new Command() {
            private static final long serialVersionUID = -4548064747843903896L;

            @Override
            public void run() {                
                Singletons.getControl().changeStateAutoFixLayout(FControl.Screens.DECK_EDITOR_CONSTRUCTED, "deck editor");
                CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(
                        new CEditorCommander());
            }
        });
    }


    /** @param lists0 &emsp; {@link java.util.List}<{@link javax.swing.JList}> */
    private void startGame() {
        Random rnd = MyRandom.getRandom();
        List<Deck> playerDecks = new ArrayList<Deck>();
        List<Deck> problemDecks = new ArrayList<Deck>();
        
        for (int i = 0; i < view.getNumPlayers(); i++) {
            
            Object o = view.getDeckLists().get(i).getSelectedValue();
            Deck d = null;
            if(o instanceof String)
            {
                d = view.getAllCommanderDecks().get(rnd.nextInt(view.getAllCommanderDecks().size()));
            }
            else
            {
                d = (Deck)o;
            }

            if (d == null) {
                //ERROR!
                GuiDialog.message("No deck selected for player " + (i + 1));
                return;
            }
            String errorMessage = GameType.Commander.getDecksFormat().getDeckConformanceProblem(d);
            if (null != errorMessage) {
                if(!problemDecks.contains(d)) 
                {
                    JOptionPane.showMessageDialog(null, "The deck "  + d.getName() + " " + errorMessage +  " Please edit or choose a different deck.", "Invalid deck", JOptionPane.ERROR_MESSAGE);
                    problemDecks.add(d);
                }
            }
            playerDecks.add(d);
        }
        if(problemDecks.size() != 0)
            return;
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });
        
        Lobby lobby = FServer.instance.getLobby();
        List<RegisteredPlayer> helper = new ArrayList<RegisteredPlayer>();
        for (int i = 0; i < view.getNumPlayers(); i++) {
            LobbyPlayer player = i == 0 ? lobby.getGuiPlayer() : lobby.getAiPlayer();
            helper.add(RegisteredPlayer.forCommander(playerDecks.get(i)).setPlayer(player));
        }
        final Match mc = new Match(GameType.Commander, helper);
        FThreads.invokeInEdtLater(new Runnable(){
            @Override
            public void run() {
                Singletons.getControl().startGameWithUi(mc);
                SOverlayUtils.hideOverlay();
            }
        });
    }


    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
