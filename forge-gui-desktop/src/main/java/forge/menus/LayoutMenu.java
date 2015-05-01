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
import forge.assets.FSkinProp;
import forge.gui.GuiChoose;
import forge.gui.MouseUtil;
import forge.gui.framework.FScreen;
import forge.gui.framework.SLayoutIO;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedMenuItem;
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

        final JMenu menu = new JMenu("Layout");
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
        final JMenu menu = new JMenu("View");
        menu.add(getMenuItem_ShowTabs());
        if (currentScreen != null && currentScreen.isMatchScreen()) {
            menu.add(getMenuItem_ShowBackgroundImage());
        }
        return menu;
    }

    private JMenu getMenu_FileOptions() {
        final JMenu menu = new JMenu("File");
        menu.add(getMenuItem_OpenLayout());
        menu.add(getMenuItem_SaveLayout());
        return menu;
    }

    private static JMenu getMenu_ThemeOptions() {
        final JMenu menu = new JMenu("Theme");
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
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem("Background Image");
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
                    FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(FSkin.getIcon(FSkinProp.BG_MATCH));
                } else {
                    FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage((Image)null);
                }
                FView.SINGLETON_INSTANCE.getPnlInsets().repaint();
            }
        };
    }

    private static JMenuItem getMenuItem_ShowTabs() {
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem("Panel Tabs");
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
        final SkinnedMenuItem menuItem = new SkinnedMenuItem("Save Current Layout");
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
        final SkinnedMenuItem menuItem = new SkinnedMenuItem("Open...");
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
        final SkinnedMenuItem menuItem = new SkinnedMenuItem("Refresh");
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
        final JMenuItem menuItem = new JMenuItem("Set Window Size");
        menuItem.addActionListener(getSetWindowSizeAction());
        return menuItem;
    }

    private static ActionListener getSetWindowSizeAction() {
        return new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                final String[] options = {"800x600", "1024x768", "1280x720"};
                final String choice = GuiChoose.oneOrNone("Choose new window size", options);
                if (choice != null) {
                    final String[] dims = choice.split("x");
                    Singletons.getView().getFrame().setSize(Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
                }
            }
        };
    }

    private static JMenuItem fullScreenItem;
    public static void updateFullScreenItemText() {
        fullScreenItem.setText(Singletons.getView().getFrame().isFullScreen() ? "Exit Full Screen" : "Full Screen");
    }
    private static JMenuItem getMenuItem_FullScreen() {
        fullScreenItem = new JMenuItem("Full Screen");
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
