/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.control;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.eventbus.Subscribe;

import forge.Card;
import forge.FThreads;
import forge.Constant.Preferences;
import forge.Singletons;
import forge.control.KeyboardShortcuts.Shortcut;
import forge.game.GameState;
import forge.game.ai.AiProfileUtil;
import forge.game.event.CardsAntedEvent;
import forge.game.event.DuelFinishedEvent;
import forge.game.event.Event;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.gui.GuiDialog;
import forge.gui.SOverlayUtils;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.VDeckEditorUI;
import forge.gui.framework.EDocID;
import forge.gui.framework.SDisplayUtil;
import forge.gui.framework.SOverflowUtil;
import forge.gui.framework.SResizingUtil;
import forge.gui.home.CHomeUI;
import forge.gui.home.VHomeUI;
import forge.gui.match.CMatchUI;
import forge.gui.match.VMatchUI;
import forge.gui.match.ViewWinLose;
import forge.gui.match.controllers.CCombat;
import forge.gui.match.controllers.CDock;
import forge.gui.match.controllers.CLog;
import forge.gui.match.controllers.CMessage;
import forge.gui.match.controllers.CStack;
import forge.gui.match.nonsingleton.VField;
import forge.gui.match.views.VAntes;
import forge.gui.toolbox.CardFaceSymbols;
import forge.gui.toolbox.FSkin;
import forge.net.NetServer;
import forge.properties.NewConstants;
import forge.quest.data.QuestPreferences.QPref;
import forge.quest.io.QuestDataIO;
import forge.sound.SoundSystem;
import forge.view.FView;

/**
 * <p>
 * FControl.
 * </p>
 * Controls all Forge UI functionality inside one JFrame. This class switches
 * between various display states in that JFrame. Controllers are instantiated
 * separately by each state's top level view class.
 */
public enum FControl {
    /** */
    SINGLETON_INSTANCE;

    private List<Shortcut> shortcuts;
    private JLayeredPane display;
    private Screens state = Screens.UNKNOWN;

    private WindowListener waDefault, waConcede, waLeaveBazaar, waLeaveEditor;

    public static enum Screens {
        UNKNOWN,
        HOME_SCREEN,
        MATCH_SCREEN,
        DECK_EDITOR_CONSTRUCTED,
        QUEST_BAZAAR,
        DECK_EDITOR_LIMITED,
        DECK_EDITOR_QUEST,
        QUEST_CARD_SHOP,
        DRAFTING_PROCESS
    }

    private final SoundSystem soundSystem = new SoundSystem();

    /**
     * <p>
     * FControl.
     * </p>
     * Controls all Forge UI functionality inside one JFrame. This class
     * switches between various display states in that JFrame. Controllers are
     * instantiated separately by each state's top level view class.
     */
    private FControl() {
        this.waDefault = new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                Singletons.getView().getFrame().setDefaultCloseOperation(
                        WindowConstants.EXIT_ON_CLOSE);

                System.exit(0);
            }
        };
        
        

        // "Close" button override during match
        this.waConcede = new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                Singletons.getView().getFrame().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

                if (!FControl.this.game.isGameOver())
                    CDock.SINGLETON_INSTANCE.concede();
                else {
                    Singletons.getControl().changeState(FControl.Screens.HOME_SCREEN);
                    SOverlayUtils.hideOverlay();
                }
            }
        };

        // "Close" button override while inside bazaar (will probably be used later for other things)
        this.waLeaveBazaar = new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                Singletons.getView().getFrame().setDefaultCloseOperation(
                        WindowConstants.DO_NOTHING_ON_CLOSE);

                changeState(Screens.HOME_SCREEN);
            }
        };

         this.waLeaveEditor = new WindowAdapter() {
             @Override
             public void windowClosing(final WindowEvent ev) {
                 Singletons.getView().getFrame().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

                 if (CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().exit()) {
                     changeState(Screens.HOME_SCREEN);
                 }
             }
         };
    }

    /** After view and model have been initialized, control can start. */
    public void initialize() {
        // Preloads skin components (using progress bar).
        FSkin.loadFull();

        //This must be done here or at least between the skin being loaded and any FTabbedPanes being created.
        //Why,Swing? Why is this not a property of JTabbbedPane?
        UIManager.put("TabbedPane.selected", FSkin.getColor(FSkin.Colors.CLR_ACTIVE));
        UIManager.put("TabbedPane.contentOpaque", FSkin.getColor(FSkin.Colors.CLR_THEME));
        UIManager.put("TabbedPane.unselectedBackground", FSkin.getColor(FSkin.Colors.CLR_THEME2));

        // Does not use progress bar, due to be deprecated with battlefield refactoring.
        CardFaceSymbols.loadImages();

        this.shortcuts = KeyboardShortcuts.attachKeyboardShortcuts();
        this.display = FView.SINGLETON_INSTANCE.getLpnDocument();
        
        FSkin.setProgessBarMessage("About to load current quest.");
        // Preload quest data if present
        final File dirQuests = new File(NewConstants.QUEST_SAVE_DIR);
        final String questname = Singletons.getModel().getQuestPreferences().getPref(QPref.CURRENT_QUEST);
        final File data = new File(dirQuests.getPath(), questname);
        if (data.exists()) {
            Singletons.getModel().getQuest().load(QuestDataIO.loadData(data));
        }

        FSkin.setProgessBarMessage("Will load AI profiles now.");
        // Preload AI profiles
        AiProfileUtil.loadAllProfiles();

        // Handles resizing in null layouts of layers in JLayeredPane.
        Singletons.getView().getFrame().addComponentListener(new ComponentAdapter() {
           @Override
           public void componentResized(final ComponentEvent e) {
               sizeChildren();
           }
        });

        FView.SINGLETON_INSTANCE.getLpnDocument().addMouseListener(SOverflowUtil.getHideOverflowListener());
        FView.SINGLETON_INSTANCE.getLpnDocument().addComponentListener(SResizingUtil.getWindowResizeListener());

        FSkin.setProgessBarMessage("Opening main window...");
        SwingUtilities.invokeLater(new Runnable() { @Override
            public void run() { Singletons.getView().initialize(); } });
    }

    /**
     * Switches between display states in top level JFrame.
     */
    public void changeState(Screens screen) {
        clearChildren(JLayeredPane.DEFAULT_LAYER);
        this.state = screen;

        Singletons.getView().getFrame().removeWindowListener(waDefault);
        Singletons.getView().getFrame().removeWindowListener(waConcede);
        Singletons.getView().getFrame().removeWindowListener(waLeaveBazaar);
        Singletons.getView().getFrame().removeWindowListener(waLeaveEditor);

        // Fire up new state
        switch (screen) {
            case HOME_SCREEN:
                SOverlayUtils.hideTargetingOverlay();
                VHomeUI.SINGLETON_INSTANCE.populate();
                CHomeUI.SINGLETON_INSTANCE.initialize();
                FView.SINGLETON_INSTANCE.getPnlInsets().setVisible(true);
                FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(new ImageIcon());
                Singletons.getView().getFrame().addWindowListener(waDefault);
                break;

            case MATCH_SCREEN:
                VMatchUI.SINGLETON_INSTANCE.populate();
                FView.SINGLETON_INSTANCE.getPnlInsets().setVisible(true);
                FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(FSkin.getIcon(FSkin.Backgrounds.BG_MATCH));
                Singletons.getView().getFrame().addWindowListener(waConcede);
                SOverlayUtils.showTargetingOverlay();
                break;

            case DECK_EDITOR_CONSTRUCTED:
            case DECK_EDITOR_LIMITED:
            case DECK_EDITOR_QUEST:
            case QUEST_CARD_SHOP:
            case DRAFTING_PROCESS:
                SOverlayUtils.hideTargetingOverlay();
                VDeckEditorUI.SINGLETON_INSTANCE.populate();
                FView.SINGLETON_INSTANCE.getPnlInsets().setVisible(true);
                FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(new ImageIcon());
                Singletons.getView().getFrame().addWindowListener(waLeaveEditor);
                break;

            case QUEST_BAZAAR:
                SOverlayUtils.hideTargetingOverlay();
                display.add(Singletons.getView().getViewBazaar(), JLayeredPane.DEFAULT_LAYER);
                FView.SINGLETON_INSTANCE.getPnlInsets().setVisible(false);
                sizeChildren();
                Singletons.getView().getFrame().addWindowListener(waLeaveBazaar);
                break;

            default:
                throw new RuntimeException("unhandled screen: " + screen);
        }
    }

    /** 
     * Returns the int reflecting the current state of the top level frame
     * (see field definitions and class methods for details).
     * 
     * @return {@link java.lang.Integer}
     * */
    public Screens getState() {
        return this.state;
    }

    /** @return List<Shortcut> A list of attached keyboard shortcut descriptions and properties. */
    public List<Shortcut> getShortcuts() {
        return this.shortcuts;
    }

    /** Remove all children from a specified layer. */
    private void clearChildren(final int layer0) {
        final Component[] children = FView.SINGLETON_INSTANCE.getLpnDocument()
                .getComponentsInLayer(layer0);

        for (final Component c : children) {
            display.remove(c);
        }
    }

    /** Sizes children of JLayeredPane to fully fit their layers. */
    private void sizeChildren() {
        Component[] children = display.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER);
        if (children.length != 0) { children[0].setSize(display.getSize()); }

        children = display.getComponentsInLayer(FView.TARGETING_LAYER);
        if (children.length != 0) { children[0].setSize(display.getSize()); }

        children = display.getComponentsInLayer(JLayeredPane.MODAL_LAYER);
        if (children.length != 0) { children[0].setSize(display.getSize()); }
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    private Lobby lobby = null;
    public Lobby getLobby() {
        if (lobby == null) {
            lobby = new Lobby();
        }
        return lobby;
    }
    
    public boolean mayShowCard(Card c) {
        if ( game == null ) return true;
        Player p = game.getPhaseHandler().getPriorityPlayer();
        return c.canBeShownTo(p);
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public SoundSystem getSoundSystem() {
        return soundSystem;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    private final NetServer server = new NetServer();
    public NetServer getServer() {
        // TODO Auto-generated method stub
        return server;
    }

    private GameState game;
    public GameState getObservedGame() {
        return game;
    }

    public void attachToGame(GameState game0) {
        // TODO: Detach from other game we might be looking at
        
        this.game = game0;
        game.getEvents().register(Singletons.getControl().getSoundSystem());
        
        LobbyPlayer humanLobbyPlayer = getLobby().getGuiPlayer();
        // The UI controls should use these game data as models
        CMatchUI.SINGLETON_INSTANCE.initMatch(game.getRegisteredPlayers(), humanLobbyPlayer);
        CDock.SINGLETON_INSTANCE.setModel(game, humanLobbyPlayer);
        CStack.SINGLETON_INSTANCE.setModel(game.getStack(), humanLobbyPlayer);
        CLog.SINGLETON_INSTANCE.setModel(game.getGameLog());
        CCombat.SINGLETON_INSTANCE.setModel(game);
        CMessage.SINGLETON_INSTANCE.setModel(game);
    
    
        Singletons.getModel().getPreferences().actuateMatchPreferences();
        Singletons.getControl().changeState(Screens.MATCH_SCREEN);
        SDisplayUtil.showTab(EDocID.REPORT_LOG.getDoc());
    
        CMessage.SINGLETON_INSTANCE.getInputControl().setGame(game);
    
        // models shall notify controllers of changes
        
        game.getGameLog().addObserver(CLog.SINGLETON_INSTANCE);
        // some observers were set in CMatchUI.initMatch
    
        // Listen to DuelOutcome event to show ViewWinLose
        game.getEvents().register(this);
        
        
        VAntes.SINGLETON_INSTANCE.setModel(game.getRegisteredPlayers());
    
        for (final VField field : VMatchUI.SINGLETON_INSTANCE.getFieldViews()) {
            field.getLblLibrary().setHoverable(Preferences.DEV_MODE);
        }
    
        // per player observers were set in CMatchUI.SINGLETON_INSTANCE.initMatch
        //Set Field shown to current player.
        VField nextField = CMatchUI.SINGLETON_INSTANCE.getFieldViewFor(game.getPlayers().get(0));
        SDisplayUtil.showTab(nextField);
    }
    
    @Subscribe
    public void receiveGameEvent(Event ev) { 

        if( ev instanceof DuelFinishedEvent ) {
            FThreads.invokeInEdtNowOrLater(new Runnable() { @Override public void run() {
                new ViewWinLose(game.getMatch());
                SOverlayUtils.showOverlay();
            } });
        } else if ( ev instanceof CardsAntedEvent ) {
            // Require EDT here? 
            final String nl = System.getProperty("line.separator");
            final StringBuilder msg = new StringBuilder();
            for (final Pair<Player, Card> kv : ((CardsAntedEvent) ev).cards) {
                msg.append(kv.getKey().getName()).append(" ante: ").append(kv.getValue()).append(nl);
            }
            GuiDialog.message(msg.toString(), "Ante");
        }
    }
}
