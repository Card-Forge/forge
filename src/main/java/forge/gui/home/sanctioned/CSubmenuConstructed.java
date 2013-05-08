package forge.gui.home.sanctioned;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import forge.Command;
import forge.FThreads;
import forge.Singletons;
import forge.control.Lobby;
import forge.deck.Deck;
import forge.game.GameType;
import forge.game.MatchController;
import forge.game.MatchStartHelper;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

/** 
 * Controls the constructed submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuConstructed implements ICDoc {
    /** */
    SINGLETON_INSTANCE;
    private final VSubmenuConstructed view = VSubmenuConstructed.SINGLETON_INSTANCE;


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
        final ForgePreferences prefs = Singletons.getModel().getPreferences();
        view.getDcAi().initialize();
        view.getDcHuman().initialize();

        // Checkbox event handling
        view.getBtnStart().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                startGame(GameType.Constructed);
            }
        });

        // Checkbox event handling
        view.getCbSingletons().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setPref(FPref.DECKGEN_SINGLETONS,
                        String.valueOf(view.getCbSingletons().isSelected()));
                prefs.save();
            }
        });

        view.getCbArtifacts().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setPref(
                        FPref.DECKGEN_ARTIFACTS, String.valueOf(view.getCbArtifacts().isSelected()));
                prefs.save();
            }
        });

        view.getCbRemoveSmall().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setPref(
                        FPref.DECKGEN_NOSMALL, String.valueOf(view.getCbRemoveSmall().isSelected()));
                prefs.save();
            }
        });

        // Pre-select checkboxes
        view.getCbSingletons().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_SINGLETONS));
        view.getCbArtifacts().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
        view.getCbRemoveSmall().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_NOSMALL));
    }

    /**
     *
     * @param gameType
     */
    private void startGame(final GameType gameType) {
        Deck humanDeck = VSubmenuConstructed.SINGLETON_INSTANCE.getDcHuman().getDeck();
        String humanDeckErrorMessage = gameType.getDecksFormat().getDeckConformanceProblem(humanDeck);
        if (null != humanDeckErrorMessage) {
            JOptionPane.showMessageDialog(null, "Your deck " + humanDeckErrorMessage, "Invalid deck", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Deck aiDeck = VSubmenuConstructed.SINGLETON_INSTANCE.getDcAi().getDeck();
        String aiDeckErrorMessage = gameType.getDecksFormat().getDeckConformanceProblem(aiDeck);
        if (null != aiDeckErrorMessage) {
            JOptionPane.showMessageDialog(null, "AI deck " + aiDeckErrorMessage, "Invalid deck", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SOverlayUtils.startGameOverlay();
        SOverlayUtils.showOverlay();

        final MatchStartHelper starter = new MatchStartHelper();
        Lobby lobby = Singletons.getControl().getLobby();
        starter.addPlayer(lobby.getGuiPlayer(), humanDeck);
        starter.addPlayer(lobby.getAiPlayer(), aiDeck);
        final MatchController mc = new MatchController(gameType, starter.getPlayerMap());
        
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
        return null;
    }
}
