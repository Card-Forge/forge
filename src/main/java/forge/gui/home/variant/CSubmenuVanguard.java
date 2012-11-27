package forge.gui.home.variant;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.google.common.collect.Iterables;

import forge.Command;
import forge.GameActionUtil;
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
import forge.gui.toolbox.FDeckChooser;
import forge.gui.toolbox.FList;
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
        for(FDeckChooser fdc : view.getDeckChoosers())
        {
            fdc.initialize();
        }

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
                Random rnd = new Random();
                
                List<Deck> playerDecks = new ArrayList<Deck>();
                for(int i=0;i<view.getNumPlayers();i++)
                {
                    Deck d = view.getDeckChoosers().get(i).getDeck();
                    
                    if(d == null)
                    {
                        //ERROR!
                        GameActionUtil.showInfoDialg("No deck selected for player " + (i+1));
                        return null;
                    }
                    playerDecks.add(d);
                }
                
                List<CardPrinted> playerAvatars = new ArrayList<CardPrinted>();
                for(int i=0;i<view.getNumPlayers();i++)
                {
                    CardPrinted avatar = null;
                    Object obj = view.getAvatarLists().get(i).getSelectedValue();
                    if(obj instanceof String)
                    {
                        //Random is the only string in the list so grab a random avatar.
                        if(i == 0)
                        {
                            //HUMAN
                            avatar = Iterables.get(view.getAllAvatars(),rnd.nextInt(Iterables.size(view.getAllAvatars())));
                        }
                        else
                        {
                            //AI
                            avatar = Iterables.get(view.getAllAiAvatars(),rnd.nextInt(Iterables.size(view.getAllAiAvatars())));
                        }
                    }
                    else
                    {
                        avatar = (CardPrinted) obj;
                    }
                    if(avatar == null)
                    {
                        //ERROR!
                        GameActionUtil.showInfoDialg("No avatar selected for player " + (i+1));
                        return null;
                    }
                    playerAvatars.add(avatar);
                }
                
                Lobby lobby = Singletons.getControl().getLobby();
                MatchStartHelper helper = new MatchStartHelper();
                for(int i=0;i<view.getNumPlayers();i++)
                {
                    LobbyPlayer player = lobby.findLocalPlayer(i == 0 ? PlayerType.HUMAN : PlayerType.COMPUTER);
                    
                    helper.addVanguardPlayer(player, playerDecks.get(i), playerAvatars.get(i));
                }
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
