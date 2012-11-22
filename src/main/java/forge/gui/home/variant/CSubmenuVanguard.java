package forge.gui.home.variant;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.google.common.collect.Iterables;

import forge.Command;
import forge.Singletons;
import forge.control.Lobby;
import forge.deck.Deck;
import forge.game.GameType;
import forge.game.MatchController;
import forge.game.MatchStartHelper;
import forge.game.PlayerStartConditions;
import forge.game.player.LobbyPlayer;
import forge.game.player.PlayerType;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.item.CardPrinted;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

/** 
 * Controls the constructed submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuVanguard implements ICDoc {
    /** */
    SINGLETON_INSTANCE;
    private final VSubmenuVanguard view = VSubmenuVanguard.SINGLETON_INSTANCE;


    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {
        // Nothing to see here...
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
                startGame();
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

    

    /** @param lists0 &emsp; {@link java.util.List}<{@link javax.swing.JList}> */
    private void startGame() {
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
                Deck humanDeck = VSubmenuVanguard.SINGLETON_INSTANCE.getDcHuman().getDeck();
                Deck aiDeck = VSubmenuVanguard.SINGLETON_INSTANCE.getDcAi().getDeck();
                Object selAiAv = VSubmenuVanguard.SINGLETON_INSTANCE.getAvAi().getSelectedValue();
                Object selHumanAv = VSubmenuVanguard.SINGLETON_INSTANCE.getAvHuman().getSelectedValue();

                Lobby lobby = Singletons.getControl().getLobby();
                LobbyPlayer humanPlayer = lobby.findLocalPlayer(PlayerType.HUMAN);
                LobbyPlayer aiPlayer = lobby.findLocalPlayer(PlayerType.COMPUTER);

                MatchStartHelper helper = new MatchStartHelper();

                final CardPrinted aiVanguard,humanVanguard;
                Iterable<CardPrinted> all = VSubmenuVanguard.SINGLETON_INSTANCE.getAllAvatars();
                Iterable<CardPrinted> aiAll = VSubmenuVanguard.SINGLETON_INSTANCE.getAllAiAvatars();
                if(selAiAv instanceof String)
                {
                    //Random is the only string in the list so grab a random avatar.
                    Random r = new Random();
                    aiVanguard = Iterables.get(aiAll,r.nextInt(Iterables.size(all)));
                }
                else
                {
                    aiVanguard = (CardPrinted)selAiAv;
                }
                if(selHumanAv instanceof String)
                {
                    //Random is the only string in the list so grab a random avatar.
                    Random r = new Random();
                    humanVanguard = Iterables.get(all,r.nextInt(Iterables.size(all)));
                }
                else
                {
                    humanVanguard = (CardPrinted)selHumanAv;
                }
                helper.addVanguardPlayer(humanPlayer, humanDeck, humanVanguard);
                helper.addVanguardPlayer(aiPlayer, aiDeck, aiVanguard);
                
                MatchController mc = Singletons.getModel().getMatch(); 
                mc.initMatch(GameType.Vanguard, helper.getPlayerMap());
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
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
