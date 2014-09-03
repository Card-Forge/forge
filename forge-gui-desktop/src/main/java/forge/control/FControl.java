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
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.commons.lang3.StringUtils;

import forge.FThreads;
import forge.GuiBase;
import forge.ImageCache;
import forge.LobbyPlayer;
import forge.Singletons;
import forge.ai.AiProfileUtil;
import forge.ai.LobbyPlayerAi;
import forge.assets.FSkinProp;
import forge.control.KeyboardShortcuts.Shortcut;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gui.GuiDialog;
import forge.gui.SOverlayUtils;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.InvalidLayoutFileException;
import forge.gui.framework.SDisplayUtil;
import forge.gui.framework.SLayoutIO;
import forge.gui.framework.SOverflowUtil;
import forge.gui.framework.SResizingUtil;
import forge.match.input.InputQueue;
import forge.menus.ForgeMenu;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.player.LobbyPlayerHuman;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestController;
import forge.quest.data.QuestPreferences.QPref;
import forge.quest.io.QuestDataIO;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.match.CMatchUI;
import forge.screens.match.VMatchUI;
import forge.screens.match.controllers.CDock;
import forge.screens.match.controllers.CLog;
import forge.screens.match.controllers.CPlayers;
import forge.screens.match.controllers.CPrompt;
import forge.screens.match.controllers.CStack;
import forge.screens.match.views.VAntes;
import forge.screens.match.views.VDev;
import forge.screens.match.views.VField;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.special.PhaseIndicator;
import forge.util.GuiDisplayUtil;
import forge.util.MyRandom;
import forge.util.NameGenerator;
import forge.view.CardView;
import forge.view.FFrame;
import forge.view.FView;
import forge.view.IGameView;
import forge.view.PlayerView;

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
    private PlayerView localPlayer;

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

        final ForgePreferences prefs = FModel.getPreferences();
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

        this.shortcuts = KeyboardShortcuts.attachKeyboardShortcuts();
        this.display = FView.SINGLETON_INSTANCE.getLpnDocument();

        final ForgePreferences prefs = FModel.getPreferences();

        this.closeAction = CloseAction.valueOf(prefs.getPref(FPref.UI_CLOSE_ACTION));

        FView.SINGLETON_INSTANCE.setSplashProgessBarMessage("About to load current quest.");
        // Preload quest data if present
        final File dirQuests = new File(ForgeConstants.QUEST_SAVE_DIR);
        final String questname = FModel.getQuestPreferences().getPref(QPref.CURRENT_QUEST);
        final File data = new File(dirQuests.getPath(), questname);
        if (data.exists()) {
            FModel.getQuest().load(QuestDataIO.loadData(data));
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
                FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(FSkin.getIcon(FSkinProp.BG_MATCH));
            }
            SOverlayUtils.showTargetingOverlay();
        }

        Singletons.getView().getNavigationBar().updateSelectedTab();
        return true;
    }

    private boolean isMatchBackgroundImageVisible() {
        return FModel.getPreferences().getPrefBoolean(FPref.UI_MATCH_IMAGE_VISIBLE);
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

    public boolean mayShowCard(final CardView c) {
        return gameView == null || !gameHasHumanPlayer || gameView.mayShowCard(c);
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public SoundSystem getSoundSystem() {
        return soundSystem;
    }

    private Game game;
    private IGameView gameView;
    private boolean gameHasHumanPlayer;

    @Deprecated
    public Game getObservedGame() {
        return game;
    }

    public IGameView getGameView() {
        return this.gameView;
    }

    public PlayerView getLocalPlayer() {
        return localPlayer;
    }

    public final void stopGame() {
        List<Player> pp = new ArrayList<Player>();
        for (Player p : game.getPlayers()) {
            if (p.getOriginalLobbyPlayer() == getGuiPlayer()) {
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
        boolean humanHasPriority = priorityPlayer == null || priorityPlayer.getLobbyPlayer() == getGuiPlayer();

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

    public final void startGameInSameMatch() {
        this.startGameWithUi(game.getMatch());
    }
    public final void startGameAndClearMatch() {
        game.getMatch().clearGamesPlayed();
        startGameInSameMatch();
    }

    public final void startGameWithUi(final Match match) {
        if (this.game != null) {
            this.setCurrentScreen(FScreen.MATCH_SCREEN);
            SOverlayUtils.hideOverlay();
            FOptionPane.showMessageDialog("Cannot start a new game while another game is already in progress.");
            return; //TODO: See if it's possible to run multiple games at once without crashing
        }
        setPlayerName(match.getPlayers());
        this.game = match.createGame();
        final LobbyPlayer me = getGuiPlayer();
        for (final Player p : this.game.getPlayers()) {
            if (p.getLobbyPlayer().equals(me)) {
                this.gameView = (IGameView) p.getController();
                break;
            }
        }

        attachToGame(this.gameView);

        // It's important to run match in a different thread to allow GUI inputs to be invoked from inside game. 
        // Game is set on pause while gui player takes decisions
        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                match.startGame(game);
            }
        });
        SOverlayUtils.hideOverlay();
    }

    public final void endCurrentGame() {
        if (this.game == null) { return; }

        Singletons.getView().getNavigationBar().closeTab(FScreen.MATCH_SCREEN);
        this.game = null;
    }

    private final FControlGameEventHandler fcVisitor = new FControlGameEventHandler();
    private final FControlGamePlayback playbackControl = new FControlGamePlayback();
    private void attachToGame(final IGameView game0) {
        if (game0.getGameType().equals(GameType.Quest)) {
            QuestController qc = FModel.getQuest();
            // Reset new list when the Match round starts, not when each game starts
            if (game0.isFirstGameInMatch()) {
                qc.getCards().resetNewList();
            }
            game0.subscribeToEvents(qc); // this one listens to player's mulligans ATM
        }

        inputQueue = new InputQueue();

        game0.subscribeToEvents(Singletons.getControl().getSoundSystem());

        //switch back to match screen music
        Singletons.getControl().getSoundSystem().setBackgroundMusic(MusicPlaylist.MATCH);

        final LobbyPlayer humanLobbyPlayer = getGuiPlayer();
        // The UI controls should use these game data as models
        final List<PlayerView> players = game0.getPlayers();
        CMatchUI.SINGLETON_INSTANCE.initMatch(players, humanLobbyPlayer);

        localPlayer = null;
        gameHasHumanPlayer = false;
        for (final PlayerView p : players) {
            if (p.getLobbyPlayer() == humanLobbyPlayer) {
                localPlayer = p;
                gameHasHumanPlayer = true;
                break;
            }
        }

        CDock.SINGLETON_INSTANCE.setModel(game0);
        CStack.SINGLETON_INSTANCE.setModel(game0, localPlayer);
        CPlayers.SINGLETON_INSTANCE.setModel(game0);
        CLog.SINGLETON_INSTANCE.setModel(game0);

        actuateMatchPreferences();
        VAntes.SINGLETON_INSTANCE.setModel(players);
        
        setCurrentScreen(FScreen.MATCH_SCREEN);
        SDisplayUtil.showTab(EDocID.REPORT_LOG.getDoc());

        CPrompt.SINGLETON_INSTANCE.getInputControl().setGame(game0);

        // Listen to DuelOutcome event to show ViewWinLose
        game0.subscribeToEvents(fcVisitor);

        // Add playback controls to match if needed
        if (localPlayer != null) {
            game0.subscribeToEvents(playbackControl);
        }

        for (final VField field : VMatchUI.SINGLETON_INSTANCE.getFieldViews()) {
            field.getDetailsPanel().getLblLibrary().setHoverable(ForgePreferences.DEV_MODE);
        }

        // per player observers were set in CMatchUI.SINGLETON_INSTANCE.initMatch
        //Set Field shown to current player.
        final VField nextField = CMatchUI.SINGLETON_INSTANCE.getFieldViewFor(localPlayer);
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
        final ForgePreferences prefs = FModel.getPreferences();
        if (StringUtils.isBlank(prefs.getPref(FPref.PLAYER_NAME))) {
            boolean isPlayerOneHuman = players.get(0).getPlayer() instanceof LobbyPlayerHuman;
            boolean isPlayerTwoComputer = players.get(1).getPlayer() instanceof LobbyPlayerAi;
            if (isPlayerOneHuman && isPlayerTwoComputer) {
                GamePlayerUtil.setPlayerName();
            }
        }
    }

    public void startMatch(GameType gameType, List<RegisteredPlayer> players) {
        startMatch(gameType, null, players);
    }

    public void startMatch(GameType gameType, Set<GameType> appliedVariants, List<RegisteredPlayer> players) {
        boolean useRandomFoil = FModel.getPreferences().getPrefBoolean(FPref.UI_RANDOM_FOIL);
        for(RegisteredPlayer rp : players) {
            rp.setRandomFoil(useRandomFoil);
        }

        GameRules rules = new GameRules(gameType);
        if (null != appliedVariants && !appliedVariants.isEmpty())
            rules.setAppliedVariants(appliedVariants);
        rules.setPlayForAnte(FModel.getPreferences().getPrefBoolean(FPref.UI_ANTE));
        rules.setMatchAnteRarity(FModel.getPreferences().getPrefBoolean(FPref.UI_ANTE_MATCH_RARITY));
        rules.setManaBurn(FModel.getPreferences().getPrefBoolean(FPref.UI_MANABURN));
        rules.canCloneUseTargetsImage = FModel.getPreferences().getPrefBoolean(FPref.UI_CLONE_MODE_SOURCE);
        
        final Match mc = new Match(rules, players);
        SOverlayUtils.startGameOverlay();
        SOverlayUtils.showOverlay();
        FThreads.invokeInEdtLater(new Runnable(){
            @Override
            public void run() {
                startGameWithUi(mc);
            }
        });
    }

    /**
     * TODO: Needs to be reworked for efficiency with rest of prefs saves in
     * codebase.
     */
    public void writeMatchPreferences() {
        final ForgePreferences prefs = FModel.getPreferences();
        final List<VField> fieldViews = VMatchUI.SINGLETON_INSTANCE.getFieldViews();

        // AI field is at index [1]
        PhaseIndicator fvAi = fieldViews.get(1).getPhaseIndicator();
        prefs.setPref(FPref.PHASE_AI_UPKEEP, String.valueOf(fvAi.getLblUpkeep().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_DRAW, String.valueOf(fvAi.getLblDraw().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_MAIN1, String.valueOf(fvAi.getLblMain1().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_BEGINCOMBAT, String.valueOf(fvAi.getLblBeginCombat().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_DECLAREATTACKERS, String.valueOf(fvAi.getLblDeclareAttackers().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_DECLAREBLOCKERS, String.valueOf(fvAi.getLblDeclareBlockers().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_FIRSTSTRIKE, String.valueOf(fvAi.getLblFirstStrike().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_COMBATDAMAGE, String.valueOf(fvAi.getLblCombatDamage().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_ENDCOMBAT, String.valueOf(fvAi.getLblEndCombat().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_MAIN2, String.valueOf(fvAi.getLblMain2().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_EOT, String.valueOf(fvAi.getLblEndTurn().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_CLEANUP, String.valueOf(fvAi.getLblCleanup().getEnabled()));

        // Human field is at index [0]
        PhaseIndicator fvHuman = fieldViews.get(0).getPhaseIndicator();
        prefs.setPref(FPref.PHASE_HUMAN_UPKEEP, String.valueOf(fvHuman.getLblUpkeep().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_DRAW, String.valueOf(fvHuman.getLblDraw().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_MAIN1, String.valueOf(fvHuman.getLblMain1().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_BEGINCOMBAT, String.valueOf(fvHuman.getLblBeginCombat().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_DECLAREATTACKERS, String.valueOf(fvHuman.getLblDeclareAttackers().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_DECLAREBLOCKERS, String.valueOf(fvHuman.getLblDeclareBlockers().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_FIRSTSTRIKE, String.valueOf(fvHuman.getLblFirstStrike().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_COMBATDAMAGE, String.valueOf(fvHuman.getLblCombatDamage().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_ENDCOMBAT, String.valueOf(fvHuman.getLblEndCombat().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_MAIN2, String.valueOf(fvHuman.getLblMain2().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_EOT, fvHuman.getLblEndTurn().getEnabled());
        prefs.setPref(FPref.PHASE_HUMAN_CLEANUP, fvHuman.getLblCleanup().getEnabled());

        final VDev v = VDev.SINGLETON_INSTANCE;

        // prefs.setPref(FPref.DEV_MILLING_LOSS, v.getLblMilling().getEnabled());
        prefs.setPref(FPref.DEV_UNLIMITED_LAND, v.getLblUnlimitedLands().getEnabled());

        prefs.save();
    }

    /**
     * TODO: Needs to be reworked for efficiency with rest of prefs saves in
     * codebase.
     */
    private void actuateMatchPreferences() {
        final ForgePreferences prefs = FModel.getPreferences();
        final List<VField> fieldViews = VMatchUI.SINGLETON_INSTANCE.getFieldViews();

        // AI field is at index [0]
        PhaseIndicator fvAi = fieldViews.get(1).getPhaseIndicator();
        fvAi.getLblUpkeep().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_UPKEEP));
        fvAi.getLblDraw().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_DRAW));
        fvAi.getLblMain1().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_MAIN1));
        fvAi.getLblBeginCombat().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_BEGINCOMBAT));
        fvAi.getLblDeclareAttackers().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_DECLAREATTACKERS));
        fvAi.getLblDeclareBlockers().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_DECLAREBLOCKERS));
        fvAi.getLblFirstStrike().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_FIRSTSTRIKE));
        fvAi.getLblCombatDamage().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_COMBATDAMAGE));
        fvAi.getLblEndCombat().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_ENDCOMBAT));
        fvAi.getLblMain2().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_MAIN2));
        fvAi.getLblEndTurn().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_EOT));
        fvAi.getLblCleanup().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_CLEANUP));

        // Human field is at index [1]
        PhaseIndicator fvHuman = fieldViews.get(0).getPhaseIndicator();
        fvHuman.getLblUpkeep().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_UPKEEP));
        fvHuman.getLblDraw().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_DRAW));
        fvHuman.getLblMain1().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_MAIN1));
        fvHuman.getLblBeginCombat().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_BEGINCOMBAT));
        fvHuman.getLblDeclareAttackers().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_DECLAREATTACKERS));
        fvHuman.getLblDeclareBlockers().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_DECLAREBLOCKERS));
        fvHuman.getLblFirstStrike().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_FIRSTSTRIKE));
        fvHuman.getLblCombatDamage().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_COMBATDAMAGE));
        fvHuman.getLblEndCombat().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_ENDCOMBAT));
        fvHuman.getLblMain2().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_MAIN2));
        fvHuman.getLblEndTurn().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_EOT));
        fvHuman.getLblCleanup().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_CLEANUP));

        //Singletons.getView().getViewMatch().setLayoutParams(prefs.getPref(FPref.UI_LAYOUT_PARAMS));
    }

    // Ai player creation, will stay here for a while 
    
    /** Returns a random name from the supplied list. */
    public String getRandomName() {
        String playerName = GuiDisplayUtil.getPlayerName();
        String aiName = NameGenerator.getRandomName("Any", "Generic", playerName);
        return aiName;
    }
    
    public final LobbyPlayer getAiPlayer() { return getAiPlayer(getRandomName()); }
    public final LobbyPlayer getAiPlayer(String name) {
        int avatarCount = GuiBase.getInterface().getAvatarCount();
        return getAiPlayer(name, avatarCount == 0 ? 0 : MyRandom.getRandom().nextInt(avatarCount));
    }
    public final LobbyPlayer getAiPlayer(String name, int avatarIndex) {
        LobbyPlayerAi player = new LobbyPlayerAi(name);

        // TODO: implement specific AI profiles for quest mode.
        String lastProfileChosen = FModel.getPreferences().getPref(FPref.UI_CURRENT_AI_PROFILE);
        player.setRotateProfileEachGame(lastProfileChosen.equals(AiProfileUtil.AI_PROFILE_RANDOM_DUEL));
        if(lastProfileChosen.equals(AiProfileUtil.AI_PROFILE_RANDOM_MATCH)) {
            lastProfileChosen = AiProfileUtil.getRandomProfile();
            System.out.println(String.format("AI profile %s was chosen for the lobby player %s.", lastProfileChosen, player.getName()));
        }
        player.setAiProfile(lastProfileChosen);
        player.setAvatarIndex(avatarIndex);
        return player;
    }

    private final LobbyPlayer guiPlayer = new LobbyPlayerHuman("Human");
    public final LobbyPlayer getGuiPlayer() {
        return guiPlayer;
    }
}

