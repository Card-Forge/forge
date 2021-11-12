package forge.menus;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import forge.Singletons;
import forge.control.FControl;
import forge.gui.GuiChoose;
import forge.gui.MouseUtil;
import forge.gui.framework.FScreen;
import forge.gui.framework.SLayoutIO;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedMenuItem;
import forge.util.Localizer;
import forge.view.FFrame;
import forge.view.FView;

/**
 * Returns a JMenu containing options associated with game screen layout.
 * <p>
 * Replicates options available in Dock tab.
 */
public final class LayoutMenu {
    public LayoutMenu() {
    }

    private FScreen currentScreen;
    private static final ForgePreferences prefs = FModel.getPreferences();
    private final boolean showIcons = false;

    public JMenu getMenu() {
        currentScreen = Singletons.getControl().getCurrentScreen();
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("lblLayout"));
        menu.setMnemonic(KeyEvent.VK_L);
        if (currentScreen != FScreen.HOME_SCREEN) {
            menu.add(getMenu_ViewOptions());
            menu.add(getMenu_FileOptions());
        }
        menu.add(getMenu_ThemeOptions());
        menu.addSeparator();
        menu.add(getMenuItem_FullScreen());
        menu.add(getMenuItem_SetWindowSize());
        if (currentScreen != FScreen.HOME_SCREEN) {
            menu.add(getMenuItem_RevertLayout());
        }
        return menu;
    }

    private JMenu getMenu_ViewOptions() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("lblView"));
        menu.add(getMenuItem_ShowTabs());
        if (currentScreen != null && currentScreen.isMatchScreen()) {
            menu.add(getMenuItem_ShowBackgroundImage());
        }
        return menu;
    }

    private JMenu getMenu_FileOptions() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("lblFile"));
        menu.add(getMenuItem_OpenLayout());
        menu.add(getMenuItem_SaveLayout());
        return menu;
    }

    private static JMenu getMenu_ThemeOptions() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("lblTheme"));
        JRadioButtonMenuItem menuItem;
        final ButtonGroup group = new ButtonGroup();
        final String currentSkin = prefs.getPref(FPref.UI_SKIN);
        for (final String skin : FSkin.getAllSkins()) {
            menuItem = new JRadioButtonMenuItem(skin);
            group.add(menuItem);
            if (skin.equals(currentSkin)) {
                menuItem.setSelected(true);
            }
            menuItem.setActionCommand(skin);
            menuItem.addActionListener(changeSkin);
            menu.add(menuItem);
        }
        return menu;
    }

    private static final ActionListener changeSkin = new ActionListener() {
        @Override public void actionPerformed(final ActionEvent e) {
            MouseUtil.setCursor(Cursor.WAIT_CURSOR);
            FSkin.changeSkin(e.getActionCommand());
            MouseUtil.resetCursor();
        }
    };

    private static JMenuItem getMenuItem_ShowBackgroundImage() {
        final Localizer localizer = Localizer.getInstance();
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(localizer.getMessage("lblBackgroundImage"));
        menuItem.setState(prefs.getPrefBoolean(FPref.UI_MATCH_IMAGE_VISIBLE));
        menuItem.addActionListener(getShowBackgroundImageAction(menuItem));
        return menuItem;
    }

    private static ActionListener getShowBackgroundImageAction(final JCheckBoxMenuItem menuItem) {
        return new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                final boolean isVisible = menuItem.getState();
                prefs.setPref(FPref.UI_MATCH_IMAGE_VISIBLE, isVisible);
                if (isVisible) {
                    if (FControl.instance.getCurrentScreen().getDaytime() == null)
                        FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(FSkin.getIcon(FSkinProp.BG_MATCH), true);
                    else {
                        if ("Day".equals(FControl.instance.getCurrentScreen().getDaytime()))
                            FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(FSkin.getIcon(FSkinProp.BG_DAY), true);
                        else
                            FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(FSkin.getIcon(FSkinProp.BG_NIGHT), true);
                    }
                } else {
                    FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage((Image)null);
                }
                FView.SINGLETON_INSTANCE.getPnlInsets().repaint();
            }
        };
    }

    private static JMenuItem getMenuItem_ShowTabs() {
        final Localizer localizer = Localizer.getInstance();
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(localizer.getMessage("lblPanelTabs"));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_T));
        menuItem.setState(!prefs.getPrefBoolean(FPref.UI_HIDE_GAME_TABS));
        menuItem.addActionListener(getShowTabsAction(menuItem));
        return menuItem;
    }
    private static ActionListener getShowTabsAction(final JCheckBoxMenuItem menuItem) {
        return new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                final boolean showTabs = menuItem.getState();
                FView.SINGLETON_INSTANCE.refreshAllCellLayouts(showTabs);
                prefs.setPref(FPref.UI_HIDE_GAME_TABS, !showTabs);
                prefs.save();
            }
        };
    }

    private JMenuItem getMenuItem_SaveLayout() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblSaveCurrentLayout"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_SAVELAYOUT) : null));
        menuItem.addActionListener(getSaveLayoutAction());
        return menuItem;
    }

    private static ActionListener getSaveLayoutAction() {
        return new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                SLayoutIO.saveLayout();
            }
        };
    }

    private JMenuItem getMenuItem_OpenLayout() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblOpen") +"..");
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_OPENLAYOUT) : null));
        menuItem.addActionListener(getOpenLayoutAction());
        return menuItem;
    }

    private static ActionListener getOpenLayoutAction() {
        return new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                SLayoutIO.openLayout();
            }
        };
    }

    private JMenuItem getMenuItem_RevertLayout() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblRefresh"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_REVERTLAYOUT) : null));
        menuItem.addActionListener(getRevertLayoutAction());
        return menuItem;
    }

    private static ActionListener getRevertLayoutAction() {
        return new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                SLayoutIO.revertLayout();
            }
        };
    }

    private static JMenuItem getMenuItem_SetWindowSize() {
        final Localizer localizer = Localizer.getInstance();
        final JMenuItem menuItem = new JMenuItem(localizer.getMessage("lblSetWindowSize"));
        menuItem.addActionListener(getSetWindowSizeAction());
        return menuItem;
    }

    private static ActionListener getSetWindowSizeAction() {
        return new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                final String[] options = {"800x600", "1024x768", "1280x720", "1600x900", "1920x1080", "2560x1440", "3840x2160"};
                final Localizer localizer = Localizer.getInstance();
                final String choice = GuiChoose.oneOrNone(localizer.getMessage("lblChooseNewWindowSize"), options);
                if (choice != null) {
                    final String[] dims = choice.split("x");
                    Singletons.getView().getFrame().setSize(Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
                }
            }
        };
    }

    private static JMenuItem fullScreenItem;
    public static void updateFullScreenItemText() {
        final Localizer localizer = Localizer.getInstance();
        fullScreenItem.setText(Singletons.getView().getFrame().isFullScreen() ? localizer.getMessage("lblExitFullScreen") : localizer.getMessage("lblFullScreen"));
    }
    private static JMenuItem getMenuItem_FullScreen() {
        final Localizer localizer = Localizer.getInstance();
        fullScreenItem = new JMenuItem(localizer.getMessage("lblFullScreen"));
        updateFullScreenItemText();
        fullScreenItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
        fullScreenItem.addActionListener(getFullScreenAction());
        return fullScreenItem;
    }
    private static ActionListener getFullScreenAction() {
        return new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                final FFrame frame = Singletons.getView().getFrame();
                frame.setFullScreen(!frame.isFullScreen());
            }
        };
    }
}
