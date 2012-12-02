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
import forge.control.FControl;
import forge.control.Lobby;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.game.GameType;
import forge.game.MatchController;
import forge.game.MatchStartHelper;
import forge.game.player.LobbyPlayer;
import forge.game.player.PlayerType;
import forge.gui.SOverlayUtils;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorScheme;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.FDeckChooser;
import forge.item.CardPrinted;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

/** 
 * Controls the constructed submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuArchenemy implements ICDoc {
    /** */
    SINGLETON_INSTANCE;
    private final VSubmenuArchenemy view = VSubmenuArchenemy.SINGLETON_INSTANCE;


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
        
        VSubmenuArchenemy.SINGLETON_INSTANCE.getLblEditor().setCommand(new Command() {
            @Override
            public void execute() {
                CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(new CEditorScheme());
                FControl.SINGLETON_INSTANCE.changeState(FControl.DECK_EDITOR_CONSTRUCTED);
            }
        });
        
        final ForgePreferences prefs = Singletons.getModel().getPreferences();
        for (FDeckChooser fdc : view.getDeckChoosers()) {
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
                String nl = System.getProperty("line.separator");
                boolean usedDefaults = false;

                List<Deck> playerDecks = new ArrayList<Deck>();
                for (int i = 0; i < view.getNumPlayers(); i++) {
                    Deck d = view.getDeckChoosers().get(i).getDeck();

                    if (d == null) {
                        //ERROR!
                        GameActionUtil.showInfoDialg("No deck selected for player " + (i + 1));
                        return null;
                    }
                    playerDecks.add(d);
                }

                List<CardPrinted> schemes = null;
                Object obj = view.getArchenemySchemes().getSelectedValue();

                boolean useDefault = VSubmenuArchenemy.SINGLETON_INSTANCE.getCbUseDefaultSchemes().isSelected();
                useDefault &= !playerDecks.get(0).getSchemes().isEmpty();

                System.out.println(useDefault);
                if (useDefault) {

                    schemes = playerDecks.get(0).getSchemes().toFlatList();
                    System.out.println(schemes.toString());
                    usedDefaults = true;
                    
                } else {

                    if (obj instanceof String) {
                        String sel = (String)obj;
                        if(sel.equals("Random"))
                        {
                            schemes = Iterables.get(view.getAllSchemeDecks(), rnd.nextInt(Iterables.size(view.getAllSchemeDecks()))).getSchemes().toFlatList();
                        }
                        else
                        {
                            //Generate
                            schemes = DeckgenUtil.generateSchemeDeck().getSchemes().toFlatList();
                        }
                    } else {
                        schemes = ((Deck)obj).getSchemes().toFlatList();
                    }
                }
                if (schemes == null) {
                    //ERROR!
                    GameActionUtil.showInfoDialg("No scheme deck selected!");
                    return null;
                }

                if (usedDefaults) {

                    GameActionUtil.showInfoDialg("Using default scheme deck.");
                }

                Lobby lobby = Singletons.getControl().getLobby();
                MatchStartHelper helper = new MatchStartHelper();
                for (int i = 0; i < view.getNumPlayers(); i++) {
                    LobbyPlayer player = lobby.findLocalPlayer(i == 0 ? PlayerType.HUMAN : PlayerType.COMPUTER);

                    if(i == 0)
                    {
                        helper.addArchenemy(player, playerDecks.get(i), schemes);
                    }
                    else
                    {
                        helper.addPlayer(player, playerDecks.get(i));
                    }
                }
                MatchController mc = Singletons.getModel().getMatch();
                mc.initMatch(GameType.Archenemy, helper.getPlayerMap());
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
