package forge.gui.home.sanctioned;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import forge.Command;
import forge.Singletons;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gui.deckchooser.DecksComboBox.DeckType;
import forge.gui.framework.ICDoc;
import forge.gui.menus.IMenuProvider;
import forge.gui.menus.MenuUtil;
import forge.gui.toolbox.FOptionPane;
import forge.net.FServer;
import forge.net.Lobby;
import forge.properties.ForgePreferences.FPref;

/**
 * Controls the constructed submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuConstructed implements ICDoc, IMenuProvider {
    /** */
    SINGLETON_INSTANCE;

    protected enum GamePlayers {
        HUMAN_VS_AI ("Human v Computer"),
        AI_VS_AI ("Computer v Computer"),
        HUMAN_VS_HUMAN ("Human v Human");
        private String value;
        private GamePlayers(String value) { this.value = value; }
        @Override
        public String toString() { return value; }
        public static GamePlayers fromString(String value){
            for (final GamePlayers t : GamePlayers.values()) {
                if (t.toString().equalsIgnoreCase(value)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("No Enum specified for this string");
        }
    };

    private final VSubmenuConstructed view = VSubmenuConstructed.SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {

        MenuUtil.setMenuProvider(this);

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { view.getBtnStart().requestFocusInWindow(); }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void initialize() {
    	view.getDeckChooser(0).initialize(FPref.CONSTRUCTED_P1_DECK_STATE, DeckType.PRECONSTRUCTED_DECK);
    	view.getDeckChooser(1).initialize(FPref.CONSTRUCTED_P2_DECK_STATE, DeckType.COLOR_DECK);
    	view.getDeckChooser(2).initialize(FPref.CONSTRUCTED_P3_DECK_STATE, DeckType.COLOR_DECK);
    	view.getDeckChooser(3).initialize(FPref.CONSTRUCTED_P4_DECK_STATE, DeckType.COLOR_DECK);
    	view.getDeckChooser(4).initialize(FPref.CONSTRUCTED_P5_DECK_STATE, DeckType.COLOR_DECK);
    	view.getDeckChooser(5).initialize(FPref.CONSTRUCTED_P6_DECK_STATE, DeckType.COLOR_DECK);
    	view.getDeckChooser(6).initialize(FPref.CONSTRUCTED_P7_DECK_STATE, DeckType.COLOR_DECK);
    	view.getDeckChooser(7).initialize(FPref.CONSTRUCTED_P8_DECK_STATE, DeckType.COLOR_DECK);

        // Checkbox event handling
        view.getBtnStart().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                startGame(GameType.Constructed);
            }
        });

    }

    /**
     *
     * @param gameType
     */
    private void startGame(final GameType gameType) {
        for (final int i : view.getParticipants()) {
        	if (view.getDeckChooser(i).getPlayer() == null) {
                FOptionPane.showMessageDialog("Please specify a deck for each player first.");
                return;
            }
        }        

        if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            for (final int i : view.getParticipants()) {
            	String name = view.getPlayerName(i);
            	String errMsg = gameType.getDecksFormat().getDeckConformanceProblem(view.getDeckChooser(i).getPlayer().getDeck());
                if (null != errMsg) {
                    FOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Deck");
                    return;
                }
            }
        }

        Lobby lobby = FServer.instance.getLobby();
        List<RegisteredPlayer> players = new ArrayList<RegisteredPlayer>();
        for (final int i : view.getParticipants()) {
        	RegisteredPlayer rp = view.getDeckChooser(i).getPlayer();
        	players.add(rp.setPlayer(view.isPlayerAI(i) ? lobby.getAiPlayer() : lobby.getGuiPlayer()));
        	view.getDeckChooser(i).saveState();
        }
        
        Singletons.getControl().startMatch(gameType, players);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.menubar.IMenuProvider#getMenus()
     */
    @Override
    public List<JMenu> getMenus() {
        List<JMenu> menus = new ArrayList<JMenu>();
        menus.add(ConstructedGameMenu.getMenu());
        return menus;
    }

}
