package forge.gui.menus;

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
import forge.control.FControl.Screens;
import forge.gui.GuiChoose;
import forge.gui.match.controllers.CDock;
import forge.gui.menubar.MenuUtil;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.view.FView;

/**
 * Returns a JMenu containing options associated with game screen layout.
 * <p>
 * Replicates options available in Dock tab.
 */
public final class LayoutMenu {
    private LayoutMenu() { }

    private static CDock controller =  CDock.SINGLETON_INSTANCE;
    private static Screens currentScreen;
    private static ForgePreferences prefs = Singletons.getModel().getPreferences();
    private static boolean showIcons = false;

    public static JMenu getMenu() {
        currentScreen = Singletons.getControl().getState();

        JMenu menu = new JMenu("Layout");
        menu.setMnemonic(KeyEvent.VK_L);
        menu.add(getMenu_ViewOptions());
        if (currentScreen != Screens.HOME_SCREEN) {
            menu.add(getMenu_FileOptions());
        }
        menu.add(getMenu_ThemeOptions());
        menu.addSeparator();
        menu.add(getMenuItem_SetWindowSize());
        if (currentScreen != Screens.HOME_SCREEN) {
            menu.add(getMenuItem_RevertLayout());
        }
        return menu;
    }

    private static JMenu getMenu_ViewOptions() {
        JMenu menu = new JMenu("View");
        menu.add(getMenuItem_ShowTitleBar());
        if (currentScreen != Screens.HOME_SCREEN) {
            menu.add(getMenuItem_ShowTabs());
        }
        if (currentScreen == Screens.MATCH_SCREEN) {
            menu.add(getMenuItem_ShowBackgroundImage());
        }
        return menu;
    }

    private static JMenu getMenu_FileOptions() {
        JMenu menu = new JMenu("File");
        menu.add(getMenuItem_OpenLayout());
        menu.add(getMenuItem_SaveLayout());
        return menu;
    }

    private static JMenu getMenu_ThemeOptions() {
        JMenu menu = new JMenu("Theme");
        JRadioButtonMenuItem menuItem;
        ButtonGroup group = new ButtonGroup();
        String currentSkin = prefs.getPref(FPref.UI_SKIN);
        String[] skins = FSkin.getSkinNamesArray(true);
        for (String skin : skins) {
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
        @Override
        public void actionPerformed(ActionEvent e) {
            FSkin.changeSkin(e.getActionCommand());
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
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean isVisible = menuItem.getState();
                prefs.setPref(FPref.UI_MATCH_IMAGE_VISIBLE, isVisible);
                if (isVisible) {
                    FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(FSkin.getIcon(FSkin.Backgrounds.BG_MATCH));
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
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean showTabs = menuItem.getState();
                FView.SINGLETON_INSTANCE.refreshAllCellLayouts(showTabs);
                prefs.setPref(FPref.UI_HIDE_GAME_TABS, !showTabs);
            }
        };
    }

    private static JMenuItem getMenuItem_ShowTitleBar() {
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem("Titlebar");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
        menuItem.setState(Singletons.getView().getFrame().getShowTitleBar());
        menuItem.addActionListener(getShowTitleBarAction(menuItem));
        return menuItem;
    }
    private static ActionListener getShowTitleBarAction(final JCheckBoxMenuItem menuItem) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean showTitleBar = menuItem.getState();
                Singletons.getView().getFrame().setShowTitleBar(showTitleBar);
            }
        };
    }

    private static JMenuItem getMenuItem_SaveLayout() {
        JMenuItem menuItem = new JMenuItem("Save Current Layout");
        FSkin.get(menuItem).setIcon((showIcons ? MenuUtil.getMenuIcon(FSkin.DockIcons.ICO_SAVELAYOUT) : null));
        menuItem.addActionListener(getSaveLayoutAction());
        return menuItem;
    }

    private static ActionListener getSaveLayoutAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.saveLayout();
            }
        };
    }

    private static JMenuItem getMenuItem_OpenLayout() {
        JMenuItem menuItem = new JMenuItem("Open...");
        FSkin.get(menuItem).setIcon((showIcons ? MenuUtil.getMenuIcon(FSkin.DockIcons.ICO_OPENLAYOUT) : null));
        menuItem.addActionListener(getOpenLayoutAction());
        return menuItem;
    }

    private static ActionListener getOpenLayoutAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.openLayout();
            }
        };
    }

    private static JMenuItem getMenuItem_RevertLayout() {
        JMenuItem menuItem = new JMenuItem("Refresh");
        FSkin.get(menuItem).setIcon((showIcons ? MenuUtil.getMenuIcon(FSkin.DockIcons.ICO_REVERTLAYOUT) : null));
        menuItem.addActionListener(getRevertLayoutAction());
        return menuItem;
    }

    private static ActionListener getRevertLayoutAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.revertLayout();
            }
        };
    }

    private static JMenuItem getMenuItem_SetWindowSize() {
        JMenuItem menuItem = new JMenuItem("Set Window Size");
        menuItem.addActionListener(getSetWindowSizeAction());
        return menuItem;
    }

    private static ActionListener getSetWindowSizeAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] options = {"800x600", "1024x768", "1280x720"};
                final String choice = GuiChoose.oneOrNone("Choose new window size", options);
                if (choice != null)
                {
                    String[] dims = choice.split("x");
                    Singletons.getView().getFrame().setSize(Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
                }
            }
        };
    }

}
