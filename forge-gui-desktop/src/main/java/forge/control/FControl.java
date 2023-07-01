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

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import forge.ImageCache;
import forge.LobbyPlayer;
import forge.Singletons;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.quest.data.QuestPreferences.QPref;
import forge.gamemodes.quest.io.QuestDataIO;
import forge.gui.GuiBase;
import forge.gui.SOverlayUtils;
import forge.gui.framework.FScreen;
import forge.gui.framework.InvalidLayoutFileException;
import forge.gui.framework.SLayoutIO;
import forge.gui.framework.SOverflowUtil;
import forge.gui.framework.SResizingUtil;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.menus.ForgeMenu;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import forge.util.RestartUtil;
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
    private JLayeredPane display;
    private FScreen currentScreen;
    private boolean altKeyLastDown;
    private CloseAction closeAction;
    private final List<HostedMatch> currentMatches = Lists.newArrayList();

    public enum CloseAction {
        NONE,
        CLOSE_SCREEN,
        EXIT_FORGE
    }

    private boolean hasCurrentMatches() {
        cleanMatches();
        return !currentMatches.isEmpty();
    }

    public List<HostedMatch> getCurrentMatches() {
        cleanMatches();
        return Collections.unmodifiableList(currentMatches);
    }

    public void addMatch(final HostedMatch match) {
        cleanMatches();
        currentMatches.add(match);
    }

    private void cleanMatches() {
        for (final HostedMatch match : ImmutableList.copyOf(currentMatches)) {
            if (match.isMatchOver()) {
                currentMatches.remove(match);
            }
        }
    }

    /**
     * <p>
     * FControl.
     * </p>
     * Controls all Forge UI functionality inside one JFrame. This class
     * switches between various display screens in that JFrame. Controllers are
     * instantiated separately by each screen's top level view class.
     */
    FControl() {
        final Localizer localizer = Localizer.getInstance();
        Singletons.getView().getFrame().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                switch (closeAction) {
                case NONE: //prompt user for close action if not previously specified
                    final List<String> options = ImmutableList.of(localizer.getMessage("lblCloseScreen"), localizer.getMessage("lblExitForge"), localizer.getMessage("lblCancel"));
                    final int reply = FOptionPane.showOptionDialog(
                            localizer.getMessage("txCloseAction1") + "\n\n" + localizer.getMessage("txCloseAction2"),
                            localizer.getMessage("titCloseAction"),
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
        return closeAction;
    }

    public void setCloseAction(final CloseAction closeAction0) {
        if (closeAction == closeAction0) { return; }
        closeAction = closeAction0;
        Singletons.getView().getNavigationBar().updateBtnCloseTooltip();

        final ForgePreferences prefs = FModel.getPreferences();
        prefs.setPref(FPref.UI_CLOSE_ACTION, closeAction0.toString());
        prefs.save();
    }

    public boolean canExitForge(final boolean forRestart) {
        final Localizer localizer = Localizer.getInstance();
        final String action = (forRestart ? localizer.getMessage("lblRestart") : localizer.getMessage("lblExit"));
        String userPrompt =(forRestart ? localizer.getMessage("lblAreYouSureYouWishRestartForge") : localizer.getMessage("lblAreYouSureYouWishExitForge"));
        final boolean hasCurrentMatches = hasCurrentMatches();
        if (hasCurrentMatches) {
            userPrompt = localizer.getMessage("lblOneOrMoreGamesActive") + ". " + userPrompt;
        }
        if (!FOptionPane.showConfirmDialog(userPrompt, action + " Forge", action, localizer.getMessage("lblCancel"), !hasCurrentMatches)) { //default Yes if no game active
            return false;
        }
        return CDeckEditorUI.SINGLETON_INSTANCE.canSwitchAway(true);
    }

    public boolean restartForge() {
        if (!canExitForge(true)) {
            return false;
        }

        if (RestartUtil.prepareForRestart()) {
            System.exit(0);
            return true;
        }
        return false;
    }

    public boolean exitForge() {
        if (!canExitForge(false)) {
            return false;
        }
        Singletons.getView().getFrame().setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        System.exit(0);
        return true;
    }

    /** After view and model have been initialized, control can start.*/
    public void initialize() {
        // Preloads skin components (using progress bar).
        FSkin.loadFull(true);

        display = FView.SINGLETON_INSTANCE.getLpnDocument();

        final ForgePreferences prefs = FModel.getPreferences();

        //set ExperimentalNetworkOption from preference
        boolean propertyConfig = prefs != null && prefs.getPrefBoolean(ForgePreferences.FPref.UI_NETPLAY_COMPAT);
        GuiBase.enablePropertyConfig(propertyConfig);

        closeAction = CloseAction.valueOf(prefs.getPref(FPref.UI_CLOSE_ACTION));

        final Localizer localizer = Localizer.getInstance();
        FView.SINGLETON_INSTANCE.setSplashProgessBarMessage(localizer.getMessage("lblLoadingQuest"));
        // Preload quest data if present
        final File dirQuests = new File(ForgeConstants.QUEST_SAVE_DIR);
        final String questname = FModel.getQuestPreferences().getPref(QPref.CURRENT_QUEST);
        final File data = new File(dirQuests.getPath(), questname);
        if (data.exists()) {
            try {
                FModel.getQuest().load(QuestDataIO.loadData(data));
            } catch(IOException ex) {
                ex.printStackTrace();
                System.out.println(String.format("Error loading quest data (%s).. skipping for now..", questname));
            }
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
        FView.SINGLETON_INSTANCE.setSplashProgessBarMessage(localizer.getMessage("lblOpeningMainWindow"));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Singletons.getView().initialize();
            }
        });
    }

    private void setGlobalKeyboardHandler() {
        final KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(this);
    }

    public ForgeMenu getForgeMenu() {
        if (forgeMenu == null) {
            forgeMenu = new ForgeMenu();
        }
        return forgeMenu;
    }

    public FScreen getCurrentScreen() {
        return currentScreen;
    }

    /**
     * Switches between display screens in top level JFrame.
     */
    public boolean setCurrentScreen(final FScreen screen) {
        return setCurrentScreen(screen, false);
    }
    public boolean setCurrentScreen(final FScreen screen, final boolean previousScreenClosed) {
        //TODO: Uncomment the line below if this function stops being used to refresh
        //the current screen in some places (such as Continue and Restart in the match screen)
        //if (currentScreen == screen) { return; }

        //give previous screen a chance to perform special switch handling and/or cancel switching away from screen
        if (currentScreen != screen && !Singletons.getView().getNavigationBar().canSwitch(screen)) {
            return false;
        }

        if (currentScreen != null && currentScreen.isMatchScreen()) { //hide targeting overlay and reset image if was on match screen
            //SOverlayUtils.hideTargetingOverlay();
            if (isMatchBackgroundImageVisible()) {
                FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(new ImageIcon());
            }
        }

        clearChildren(JLayeredPane.DEFAULT_LAYER);
        SOverlayUtils.hideOverlay();
        ImageCache.clear(); //reduce memory usage by clearing image cache when switching screens

        currentScreen = screen;

        //load layout for new current screen
        screen.getController().register();
        try {
            SLayoutIO.loadLayout(null);
        } catch (final InvalidLayoutFileException ex) {
            final Localizer localizer = Localizer.getInstance();
            SOptionPane.showMessageDialog(String.format(localizer.getMessage("lblerrLoadingLayoutFile"), screen.getTabCaption()), "Warning!");
            if (screen.deleteLayoutFile()) {
                SLayoutIO.loadLayout(null); //try again
            }
        }

        screen.getController().initialize();
        screen.getView().populate();

        if (screen.isMatchScreen()) {
            if (isMatchBackgroundImageVisible()) {
                if (screen.getDaytime() == null)
                    FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(FSkin.getIcon(FSkinProp.BG_MATCH), true);
                else {
                    if ("Day".equals(screen.getDaytime()))
                        FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(FSkin.getIcon(FSkinProp.BG_DAY), true);
                    else
                        FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(FSkin.getIcon(FSkinProp.BG_NIGHT), true);
                }
            } else {
                FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage((Image)null);
            }
            //SOverlayUtils.showTargetingOverlay();
        } else {
            FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage((Image)null);
        }

        Singletons.getView().getNavigationBar().updateSelectedTab();
        return true;
    }

    private boolean isMatchBackgroundImageVisible() {
        return FModel.getPreferences().getPrefBoolean(FPref.UI_MATCH_IMAGE_VISIBLE);
    }

    public boolean ensureScreenActive(final FScreen screen) {
        if (currentScreen == screen) { return true; }

        return setCurrentScreen(screen);
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

    public Dimension getDisplaySize() {
        return display.getSize();
    }

    /* (non-Javadoc)
     * @see java.awt.KeyEventDispatcher#dispatchKeyEvent(java.awt.event.KeyEvent)
     */
    @Override
    public boolean dispatchKeyEvent(final KeyEvent e) {
        // Show Forge menu if Alt key pressed without modifiers and released without pressing any other keys in between
        if (e.getKeyCode() == KeyEvent.VK_ALT) {
            if (e.getID() == KeyEvent.KEY_RELEASED) {
                if (altKeyLastDown) {
                    forgeMenu.show(true);
                    return true;
                }
            }
            else if (e.getID() == KeyEvent.KEY_PRESSED && e.getModifiersEx() == InputEvent.ALT_DOWN_MASK) {
                altKeyLastDown = true;
            }
        }
        else {
            altKeyLastDown = false;
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                //give Forge menu the chance to handle the key event
                return forgeMenu.handleKeyEvent(e);
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

    public final LobbyPlayer getGuiPlayer() {
        return GamePlayerUtil.getGuiPlayer();
    }
}
