package forge.gui.home.sanctioned;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import forge.Command;
import forge.FThreads;
import forge.Singletons;
import forge.game.GameType;
import forge.game.Match;
import forge.game.RegisteredPlayer;
import forge.game.player.LobbyPlayer;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.gui.menubar.IMenuProvider;
import forge.gui.menubar.MenuUtil;
import forge.gui.toolbox.FComboBox;
import forge.net.FServer;
import forge.net.Lobby;
import forge.properties.ForgePreferences;
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
    private final ForgePreferences prefs = Singletons.getModel().getPreferences();

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {

        MenuUtil.setupMenuBar(this);

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { view.getBtnStart().requestFocusInWindow(); }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void initialize() {

        initializeGamePlayersComboBox();

        view.getDcLeft().initialize();
        view.getDcRight().initialize();

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
        RegisteredPlayer pscLeft = view.getDcLeft().getPlayer();
        RegisteredPlayer pscRight = view.getDcRight().getPlayer();

        if (pscLeft == null || pscRight == null) {
            JOptionPane.showMessageDialog(null, "Please specify a Human and Computer deck first.");
            return;
        }

        String leftDeckErrorMessage = gameType.getDecksFormat().getDeckConformanceProblem(pscLeft.getOriginalDeck());
        if (null != leftDeckErrorMessage) {
            JOptionPane.showMessageDialog(null, "Left-side deck " + leftDeckErrorMessage, "Invalid deck", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String rightDeckErrorMessage = gameType.getDecksFormat().getDeckConformanceProblem(pscRight.getOriginalDeck());
        if (null != rightDeckErrorMessage) {
            JOptionPane.showMessageDialog(null, "Right-side deck " + rightDeckErrorMessage, "Invalid deck", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Lobby lobby = FServer.instance.getLobby();
        LobbyPlayer leftPlayer = view.isLeftPlayerAi() ? lobby.getAiPlayer() : lobby.getGuiPlayer();
        LobbyPlayer rightPlayer = view.isRightPlayerAi() ? lobby.getAiPlayer() : lobby.getGuiPlayer();

        List<RegisteredPlayer> players = new ArrayList<RegisteredPlayer>();
        players.add(pscLeft.setPlayer(leftPlayer));
        players.add(pscRight.setPlayer(rightPlayer));
        final Match mc = new Match(gameType, players);

        SOverlayUtils.startGameOverlay();
        SOverlayUtils.showOverlay();

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

    /* (non-Javadoc)
     * @see forge.gui.menubar.IMenuProvider#getMenus()
     */
    @Override
    public List<JMenu> getMenus() {
        List<JMenu> menus = new ArrayList<JMenu>();
        menus.add(ConstructedGameMenu.getMenu());
        return menus;
    }

    private void initializeGamePlayersComboBox() {
        final FComboBox<GamePlayers> comboBox = this.view.getGamePlayersComboBox();
        comboBox.setModel(new DefaultComboBoxModel<GamePlayers>(GamePlayers.values()));
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setGamePlayers((GamePlayers)comboBox.getSelectedItem());
            }
        });
        comboBox.setSelectedItem(getGamePlayersSetting());
    }

    private void setGamePlayers(GamePlayers p) {
        boolean isPlayerOneHuman = (p == GamePlayers.HUMAN_VS_AI) || (p == GamePlayers.HUMAN_VS_HUMAN);
        boolean isPlayerTwoHuman = (p == GamePlayers.HUMAN_VS_HUMAN);
        view.getDcLeft().setIsAiDeck(isPlayerOneHuman ? false : true);
        view.getDcRight().setIsAiDeck(isPlayerTwoHuman ? false : true);
        prefs.setPref(FPref.CONSTRUCTED_GAMEPLAYERS, p.name());
        prefs.save();
    }

    private GamePlayers getGamePlayersSetting() {
        try {
            GamePlayers players = GamePlayers.valueOf(prefs.getPref(FPref.CONSTRUCTED_GAMEPLAYERS));
            return players;
        } catch (IllegalArgumentException e) {
            return GamePlayers.HUMAN_VS_AI;
        }
    }

}
