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
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.commons.lang3.StringUtils;

import forge.Constant.Preferences;
import forge.FThreads;
import forge.ImageCache;
import forge.Singletons;
import forge.control.KeyboardShortcuts.Shortcut;
import forge.game.Game;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.player.LobbyPlayer;
import forge.game.player.LobbyPlayerAi;
import forge.gui.player.LobbyPlayerHuman;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gui.GuiDialog;
import forge.gui.SOverlayUtils;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.InvalidLayoutFileException;
import forge.gui.framework.SDisplayUtil;
import forge.gui.framework.SLayoutIO;
import forge.gui.framework.SOverflowUtil;
import forge.gui.framework.SResizingUtil;
import forge.gui.home.settings.GamePlayerUtil;
import forge.gui.match.CMatchUI;
import forge.gui.match.VMatchUI;
import forge.gui.match.controllers.CDock;
import forge.gui.match.controllers.CLog;
import forge.gui.match.controllers.CPrompt;
import forge.gui.match.controllers.CStack;
import forge.gui.match.views.VAntes;
import forge.gui.match.views.VField;
import forge.gui.menus.ForgeMenu;
import forge.gui.toolbox.FOptionPane;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.itemmanager.SItemManagerIO;
import forge.net.FServer;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.properties.NewConstants;
import forge.quest.QuestController;
import forge.quest.data.QuestPreferences.QPref;
import forge.quest.io.QuestDataIO;
import forge.sound.SoundSystem;
import forge.view.FFrame;
import forge.view.FView;

/**
 * <p>
 * FControl.
 * </p>
 * Controls all Forge UI functionality inside one JFrame. This class switches
 * between various display screens in that JFrame. Controllers are instantiated
 * separately by each screen's top level view class.
 */
public enum FControl implements KeyEventDispatcher {
    instance;

    private ForgeMenu forgeMenu;
    private List<Shortcut> shortcuts;
    private JLayeredPane display;
    private FScreen currentScreen;
    private boolean altKeyLastDown;
    private CloseAction closeAction;

    public static enum CloseAction {
        NONE,
        CLOSE_SCREEN,
        EXIT_FORGE
    }

    private final SoundSystem soundSystem = new SoundSystem();

    /**
     * <p>
     * FControl.
     * </p>
     * Controls all Forge UI functionality inside one JFrame. This class
     * switches between various display screens in that JFrame. Controllers are
     * instantiated separately by each screen's top level view class.
     */
    private FControl() {
        Singletons.getView().getFrame().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                switch (closeAction) {
                case NONE: //prompt user for close action if not previously specified
                    String[] options = {"Close Screen", "Exit Forge", "Cancel"};
                    int reply = FOptionPane.showOptionDialog(
                            "Forge now supports navigation tabs which allow closing and switching between different screens with ease. "
                            + "As a result, you no longer need to use the X button in the upper right to close the current screen and go back."
                            + "\n\n"
                            + "Please select what you want to happen when clicking the X button in the upper right. This choice will be used "
                            + "going forward and you will not see this message again. You can change this behavior at any time in Preferences.",
                            "Select Your Close Action",
                            FOptionPane.INFORMATION_ICON,
                            options,
                            2);
                    switch (reply) {
                    case 0: //Close Screen
                        setCloseAction(CloseAction.CLOSE_SCREEN);
                        windowClosing(e); //call again to apply chosen close action
                        return;
                    case 1: //Exit Forge
                        setCloseAction(CloseAction.EXIT_FORGE);
                        windowClosing(e); //call again to apply chosen close action
                        return;
                    }
                    break;
                case CLOSE_SCREEN:
                    Singletons.getView().getNavigationBar().closeSelectedTab();
                    break;
                case EXIT_FORGE:
                    if (exitForge()) { return; }
                    break;
                }
                //prevent closing Forge if we reached this point
                Singletons.getView().getFrame().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            }
        });
    }

    public CloseAction getCloseAction() {
        return this.closeAction;
    }

    public void setCloseAction(CloseAction closeAction0) {
        if (this.closeAction == closeAction0) { return; }
        this.closeAction = closeAction0;
        Singletons.getView().getNavigationBar().updateBtnCloseTooltip();

        final ForgePreferences prefs = Singletons.getModel().getPreferences();
        prefs.setPref(FPref.UI_CLOSE_ACTION, closeAction0.toString());
        prefs.save();
    }

    public boolean canExitForge(boolean forRestart) {
        String action = (forRestart ? "Restart" : "Exit");
        String userPrompt = "Are you sure you wish to " + (forRestart ? "restart" : "exit") + " Forge?";
        if (this.game != null) {
            userPrompt = "A game is currently active. " + userPrompt;
        }
        if (!FOptionPane.showConfirmDialog(userPrompt, action + " Forge", action, "Cancel", this.game == null)) { //default Yes if no game active
            return false;
        }
        if (!CDeckEditorUI.SINGLETON_INSTANCE.canSwitchAway(true)) {
            return false;
        }
        return true;
    }

    public boolean exitForge() {
        if (!canExitForge(false)) {
            return false;
        }
        Singletons.getView().getFrame().setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        System.exit(0);
        return true;
    }

    /** After view and model have been initialized, control can start.
     * @param isHeadlessMode */
    public void initialize() {
        // Preloads skin components (using progress bar).
        FSkin.loadFull(true);

        SItemManagerIO.loadPreferences();

        this.shortcuts = KeyboardShortcuts.attachKeyboardShortcuts();
        this.display = FView.SINGLETON_INSTANCE.getLpnDocument();

        final ForgePreferences prefs = Singletons.getModel().getPreferences();

        this.closeAction = CloseAction.valueOf(prefs.getPref(FPref.UI_CLOSE_ACTION));

        FView.SINGLETON_INSTANCE.setSplashProgessBarMessage("About to load current quest.");
        // Preload quest data if present
        final File dirQuests = new File(NewConstants.QUEST_SAVE_DIR);
        final String questname = Singletons.getModel().getQuestPreferences().getPref(QPref.CURRENT_QUEST);
        final File data = new File(dirQuests.getPath(), questname);
        if (data.exists()) {
            Singletons.getModel().getQuest().load(QuestDataIO.loadData(data));
        }

        // Handles resizing in null layouts of layers in JLayeredPane as well as saving window layout
        final FFrame window = Singletons.getView().getFrame();
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                sizeChildren();
                window.updateNormalBounds();
                SLayoutIO.saveWindowLayout();
            }

            @Override
            public void componentMoved(final ComponentEvent e) {
                window.updateNormalBounds();
                SLayoutIO.saveWindowLayout();
            }
        });

        FView.SINGLETON_INSTANCE.getLpnDocument().addMouseListener(SOverflowUtil.getHideOverflowListener());
        FView.SINGLETON_INSTANCE.getLpnDocument().addComponentListener(SResizingUtil.getWindowResizeListener());

        setGlobalKeyboardHandler();

        FView.SINGLETON_INSTANCE.setSplashProgessBarMessage("Opening main window...");
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Singletons.getView().initialize();
            }
        });
    }

    private void setGlobalKeyboardHandler() {
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(this);
    }

    public ForgeMenu getForgeMenu() {
        if (this.forgeMenu == null) {
            this.forgeMenu = new ForgeMenu();
        }
        return this.forgeMenu;
    }

    public FScreen getCurrentScreen() {
        return this.currentScreen;
    }

    /**
     * Switches between display screens in top level JFrame.
     */
    public boolean setCurrentScreen(FScreen screen) {
        return setCurrentScreen(screen, false);
    }
    public boolean setCurrentScreen(FScreen screen, boolean previousScreenClosed) {
        //TODO: Uncomment the line below if this function stops being used to refresh
        //the current screen in some places (such as Continue and Restart in the match screen)
        //if (this.currentScreen == screen) { return; }

        //give previous screen a chance to perform special switch handling and/or cancel switching away from screen
        if (this.currentScreen != screen && !Singletons.getView().getNavigationBar().canSwitch(screen)) {
            return false;
        }

        if (this.currentScreen == FScreen.MATCH_SCREEN) { //hide targeting overlay and reset image if was on match screen
            SOverlayUtils.hideTargetingOverlay();
            if (isMatchBackgroundImageVisible()) {
                FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(new ImageIcon());
            }
        }

        clearChildren(JLayeredPane.DEFAULT_LAYER);
        SOverlayUtils.hideOverlay();
        ImageCache.clear(); //reduce memory usage by clearing image cache when switching screens

        this.currentScreen = screen;

        //load layout for new current screen
        try {
            SLayoutIO.loadLayout(null);
        } catch (InvalidLayoutFileException ex) {
            GuiDialog.message("Your " + screen.getTabCaption() + " layout file could not be read. It will be deleted after you press OK.\nThe game will proceed with default layout.");
            if (screen.deleteLayoutFile()) {
                SLayoutIO.loadLayout(null); //try again
            }
        }

        screen.getView().populate();
        screen.getController().initialize();

        if (screen == FScreen.MATCH_SCREEN) {
            if (isMatchBackgroundImageVisible()) {
                FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(FSkin.getIcon(FSkin.Backgrounds.BG_MATCH));
            }
            SOverlayUtils.showTargetingOverlay();
        }

        Singletons.getView().getNavigationBar().updateSelectedTab();
        return true;
    }

    private boolean isMatchBackgroundImageVisible() {
        return Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_MATCH_IMAGE_VISIBLE);
    }

    public boolean ensureScreenActive(FScreen screen) {
        if (this.currentScreen == screen) { return true; }

        return setCurrentScreen(screen);
    }

    /** @return List<Shortcut> A list of attached keyboard shortcut descriptions and properties. */
    public List<Shortcut> getShortcuts() {
        return this.shortcuts;
    }

    /** Remove all children from a specified layer. */
    private void clearChildren(final int layer0) {
        final Component[] children = FView.SINGLETON_INSTANCE.getLpnDocument().getComponentsInLayer(layer0);

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

    public Player getCurrentPlayer() {
        // try current priority
        Player currentPriority = game.getPhaseHandler().getPriorityPlayer();
        if (null != currentPriority && currentPriority.getLobbyPlayer() == FServer.instance.getLobby().getGuiPlayer()) {
            return currentPriority;
        }

        // otherwise find just any player, belonging to this lobbyplayer
        for (Player p : game.getPlayers()) {
            if (p.getLobbyPlayer() == FServer.instance.getLobby().getGuiPlayer()) {
                return p;
            }
        }

        return null;
    }

    public boolean mayShowCard(Card c) {
        return game == null || !gameHasHumanPlayer || c.canBeShownTo(getCurrentPlayer());
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public SoundSystem getSoundSystem() {
        return soundSystem;
    }

    private Game game;
    private boolean gameHasHumanPlayer;

    public Game getObservedGame() {
        return game;
    }

    public final void stopGame() {
        List<Player> pp = new ArrayList<Player>();
        for (Player p : game.getPlayers()) {
            if (p.getOriginalLobbyPlayer() == FServer.instance.getLobby().getGuiPlayer()) {
                pp.add(p);
            }
        }
        boolean hasHuman = !pp.isEmpty();

        if (pp.isEmpty()) {
            pp.addAll(game.getPlayers()); // no human? then all players surrender!
        }

        for (Player p: pp) {
            p.concede();
        }

        Player priorityPlayer = game.getPhaseHandler().getPriorityPlayer();
        boolean humanHasPriority = priorityPlayer == null || priorityPlayer.getLobbyPlayer() == FServer.instance.getLobby().getGuiPlayer();

        if (hasHuman && humanHasPriority) {
            game.getAction().checkGameOverCondition();
        }
        else {
            game.isGameOver(); // this is synchronized method - it's used to make Game-0 thread see changes made here
            inputQueue.onGameOver(false); //release any waiting input, effectively passing priority
        }

        playbackControl.onGameStopRequested();
    }

    private InputQueue inputQueue;
    public InputQueue getInputQueue() {
        return inputQueue;
    }

    public final void startGameWithUi(Match match) {
        if (this.game != null) {
            this.setCurrentScreen(FScreen.MATCH_SCREEN);
            SOverlayUtils.hideOverlay();
            FOptionPane.showMessageDialog("Cannot start a new game while another game is already in progress.");
            return; //TODO: See if it's possible to run multiple games at once without crashing
        }
        setPlayerName(match.getPlayers());
        Game newGame = match.createGame();
        attachToGame(newGame);
        match.startGame(newGame, null);
    }

    public final void endCurrentGame() {
        if (this.game == null) { return; }

        Singletons.getView().getNavigationBar().closeTab(FScreen.MATCH_SCREEN);
        this.game = null;
    }

    private final FControlGameEventHandler fcVisitor = new FControlGameEventHandler(this);
    private final FControlGamePlayback playbackControl = new FControlGamePlayback(this);
    private void attachToGame(Game game0) {
        if (game0.getType() == GameType.Quest) {
            QuestController qc = Singletons.getModel().getQuest();
            // Reset new list when the Match round starts, not when each game starts
            if (game0.getMatch().getPlayedGames().isEmpty()) {
                qc.getCards().resetNewList();
            }
            game0.subscribeToEvents(qc); // this one listens to player's mulligans ATM
        }

        inputQueue = new InputQueue();

        this.game = game0;
        game.subscribeToEvents(Singletons.getControl().getSoundSystem());

        LobbyPlayer humanLobbyPlayer = FServer.instance.getLobby().getGuiPlayer();
        // The UI controls should use these game data as models
        CMatchUI.SINGLETON_INSTANCE.initMatch(game.getRegisteredPlayers(), humanLobbyPlayer);
        CDock.SINGLETON_INSTANCE.setModel(game, humanLobbyPlayer);
        CStack.SINGLETON_INSTANCE.setModel(game.getStack(), humanLobbyPlayer);
        CLog.SINGLETON_INSTANCE.setModel(game.getGameLog());

        Singletons.getModel().getPreferences().actuateMatchPreferences();

        setCurrentScreen(FScreen.MATCH_SCREEN);
        SDisplayUtil.showTab(EDocID.REPORT_LOG.getDoc());

        CPrompt.SINGLETON_INSTANCE.getInputControl().setGame(game);

        // Listen to DuelOutcome event to show ViewWinLose
        game.subscribeToEvents(fcVisitor);

        // Add playback controls to match if needed
        gameHasHumanPlayer = false;
        for (Player p :  game.getPlayers()) {
            if (p.getController().getLobbyPlayer() == FServer.instance.getLobby().getGuiPlayer())
                gameHasHumanPlayer = true;
        }

        if (!gameHasHumanPlayer) {
            game.subscribeToEvents(playbackControl);
        }

        VAntes.SINGLETON_INSTANCE.setModel(game.getRegisteredPlayers());

        for (final VField field : VMatchUI.SINGLETON_INSTANCE.getFieldViews()) {
            field.getDetailsPanel().getLblLibrary().setHoverable(Preferences.DEV_MODE);
        }

        // per player observers were set in CMatchUI.SINGLETON_INSTANCE.initMatch
        //Set Field shown to current player.
        VField nextField = CMatchUI.SINGLETON_INSTANCE.getFieldViewFor(game.getPlayers().get(0));
        SDisplayUtil.showTab(nextField);
    }

    /* (non-Javadoc)
     * @see java.awt.KeyEventDispatcher#dispatchKeyEvent(java.awt.event.KeyEvent)
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        // Show Forge menu if Alt key pressed without modifiers and released without pressing any other keys in between
        if (e.getKeyCode() == KeyEvent.VK_ALT) {
            if (e.getID() == KeyEvent.KEY_RELEASED) {
                if (altKeyLastDown) {
                    forgeMenu.show(true);
                    return true;
                }
            }
            else if (e.getID() == KeyEvent.KEY_PRESSED && e.getModifiers() == KeyEvent.ALT_MASK) {
                altKeyLastDown = true;
            }
        }
        else {
            altKeyLastDown = false;
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (forgeMenu.handleKeyEvent(e)) { //give Forge menu the chance to handle the key event
                    return true;
                }
            }
            else if (e.getID() == KeyEvent.KEY_RELEASED) {
                if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU) {
                    forgeMenu.show();
                }
            }
        }
        //Allow the event to be redispatched
        return false;
    }

    /**
     * Prompts user for a name that will be used instead of "Human" during gameplay.
     * <p>
     * This is a one time only event that is triggered when starting a game and the
     * PLAYER_NAME setting is blank. Does not apply to a hotseat game.
     */
    private void setPlayerName(List<RegisteredPlayer> players) {
        final ForgePreferences prefs = Singletons.getModel().getPreferences();
        if (StringUtils.isBlank(prefs.getPref(FPref.PLAYER_NAME))) {
            boolean isPlayerOneHuman = players.get(0).getPlayer() instanceof LobbyPlayerHuman;
            boolean isPlayerTwoComputer = players.get(1).getPlayer() instanceof LobbyPlayerAi;
            if (isPlayerOneHuman && isPlayerTwoComputer) {
                GamePlayerUtil.setPlayerName();
            }
        }
    }

    public void startMatch(GameType gameType, List<RegisteredPlayer> starter) {
        boolean useAnte = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ANTE);
        final Match mc = new Match(gameType, starter, useAnte);
        SOverlayUtils.startGameOverlay();
        SOverlayUtils.showOverlay();
        FThreads.invokeInEdtLater(new Runnable(){
            @Override
            public void run() {
                startGameWithUi(mc);
                SOverlayUtils.hideOverlay();
            }
        });
    }
}

