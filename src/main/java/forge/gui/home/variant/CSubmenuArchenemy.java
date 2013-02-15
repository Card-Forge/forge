package forge.gui.home.variant;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.google.common.base.Predicate;

import forge.Command;
import forge.Singletons;
import forge.control.FControl;
import forge.control.Lobby;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.DeckgenUtil;
import forge.game.GameType;
import forge.game.MatchController;
import forge.game.MatchStartHelper;
import forge.game.player.LobbyPlayer;
import forge.game.player.PlayerType;
import forge.gui.GuiDialog;
import forge.gui.SOverlayUtils;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorVariant;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.FDeckChooser;
import forge.item.CardPrinted;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.util.Aggregates;

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
            private static final long serialVersionUID = -4548064747843903896L;

            @Override
            public void execute() {
                
                Predicate<CardPrinted> predSchemes = new Predicate<CardPrinted>() {

                    @Override
                    public boolean apply(CardPrinted arg0) {
                        if(arg0.getCard().getType().isScheme())
                        {
                            return true;
                        }
                        
                        return false;
                    }
                    
                };
                
                FControl.SINGLETON_INSTANCE.changeState(FControl.Screens.DECK_EDITOR_CONSTRUCTED);
                CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(
                        new CEditorVariant(Singletons.getModel().getDecks().getScheme(),predSchemes,EDocID.HOME_ARCHENEMY));
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
                boolean usedDefaults = false;

                List<Deck> playerDecks = new ArrayList<Deck>();
                for (int i = 0; i < view.getNumPlayers(); i++) {
                    Deck d = view.getDeckChoosers().get(i).getDeck();

                    if (d == null) {
                        //ERROR!
                        GuiDialog.message("No deck selected for player " + (i + 1));
                        return null;
                    }
                    playerDecks.add(d);
                }

                List<CardPrinted> schemes = null;
                Object obj = view.getArchenemySchemes().getSelectedValue();

                boolean useDefault = VSubmenuArchenemy.SINGLETON_INSTANCE.getCbUseDefaultSchemes().isSelected();
                useDefault &= playerDecks.get(0).has(DeckSection.Schemes);

                System.out.println(useDefault);
                if (useDefault) {
                    schemes = playerDecks.get(0).get(DeckSection.Schemes).toFlatList();
                    System.out.println(schemes.toString());
                    usedDefaults = true;
                } else {
                    if (obj instanceof String) {
                        String sel = (String) obj;
                        if (sel.equals("Random")) {
                            if (view.getAllSchemeDecks().isEmpty()) {
                                //Generate if no constructed scheme decks are available
                                System.out.println("Generating scheme deck - no others available");
                                schemes = DeckgenUtil.generateSchemeDeck().toFlatList();
                            } else {
                                System.out.println("Using scheme deck: " + Aggregates.random(view.getAllSchemeDecks()).getName());
                                schemes = Aggregates.random(view.getAllSchemeDecks()).get(DeckSection.Schemes).toFlatList();
                            }
                        } else {
                            //Generate
                            schemes = DeckgenUtil.generateSchemeDeck().toFlatList();
                        }
                    } else {
                        schemes = ((Deck) obj).get(DeckSection.Schemes).toFlatList();
                    }
                }
                if (schemes == null) {
                    //ERROR!
                    GuiDialog.message("No scheme deck selected!");
                    return null;
                }

                if (usedDefaults) {

                    GuiDialog.message("Using default scheme deck.");
                }

                Lobby lobby = Singletons.getControl().getLobby();
                MatchStartHelper helper = new MatchStartHelper();
                for (int i = 0; i < view.getNumPlayers(); i++) {
                    LobbyPlayer player = lobby.findLocalPlayer(i == 0 ? PlayerType.HUMAN : PlayerType.COMPUTER);

                    if (i == 0) {

                        helper.addArchenemy(player, playerDecks.get(i), schemes);
                        helper.getPlayerMap().get(player).setStartingLife(10 + (10 * (view.getNumPlayers() - 1)));
                    } else {

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
