package forge.menus;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

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
import forge.gui.framework.IVTopLevelUI;
import forge.gui.framework.SLayoutIO;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.screens.match.VMatchUI;
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
            menu.add(getMenu_FileOptions());
            menu.add(getMenu_ViewOptions());
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
            final JMenu layoutMenu = getMenu_MultiplayerFieldLayout();
            final JMenu panelsMenu = getMenu_MultiplayerFieldPanels();
            menu.add(getMenuItem_SortMultiplayerFields(layoutMenu, panelsMenu));
            menu.add(layoutMenu);
            menu.add(panelsMenu);
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

    private static final ActionListener changeSkin = e -> {
        MouseUtil.setCursor(Cursor.WAIT_CURSOR);
        FSkin.changeSkin(e.getActionCommand());
        MouseUtil.resetCursor();
    };

    private static JMenuItem getMenuItem_ShowBackgroundImage() {
        final Localizer localizer = Localizer.getInstance();
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(localizer.getMessage("lblBackgroundImage"));
        menuItem.setState(prefs.getPrefBoolean(FPref.UI_MATCH_IMAGE_VISIBLE));
        menuItem.addActionListener(getShowBackgroundImageAction(menuItem));
        return menuItem;
    }

    private static ActionListener getShowBackgroundImageAction(final JCheckBoxMenuItem menuItem) {
        return e -> {
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
        };
    }

    /** Creates a JCheckBoxMenuItem that stays open on click. */
    private static JCheckBoxMenuItem createStayOpenCheckBox(final String text) {
        return new JCheckBoxMenuItem(text) {
            @Override
            protected void processMouseEvent(final MouseEvent e) {
                if (e.getID() == MouseEvent.MOUSE_RELEASED && contains(e.getPoint())) {
                    doClick(0);
                    setArmed(true);
                } else {
                    super.processMouseEvent(e);
                }
            }
        };
    }

    /** Creates a JRadioButtonMenuItem that stays open on click. */
    private static JRadioButtonMenuItem createStayOpenRadioButton(final String text) {
        return new JRadioButtonMenuItem(text) {
            @Override
            protected void processMouseEvent(final MouseEvent e) {
                if (e.getID() == MouseEvent.MOUSE_RELEASED && contains(e.getPoint())) {
                    doClick(0);
                    setArmed(true);
                } else {
                    super.processMouseEvent(e);
                }
            }
        };
    }

    private static JCheckBoxMenuItem getMenuItem_SortMultiplayerFields(
            final JMenu layoutMenu, final JMenu panelsMenu) {
        final Localizer localizer = Localizer.getInstance();
        final boolean enabled = !"OFF".equals(prefs.getPref(FPref.UI_MULTIPLAYER_FIELD_LAYOUT));
        final JCheckBoxMenuItem menuItem = createStayOpenCheckBox(
                localizer.getMessage("lblSortMultiplayerFields"));
        menuItem.setToolTipText(localizer.getMessage("lblSortMultiplayerFieldsTooltip"));
        menuItem.setState(enabled);
        layoutMenu.setEnabled(enabled);
        panelsMenu.setEnabled(enabled);
        menuItem.addActionListener(e -> {
            final boolean on = menuItem.getState();
            prefs.setPref(FPref.UI_MULTIPLAYER_FIELD_LAYOUT, on ? "GRID" : "OFF");
            prefs.save();
            layoutMenu.setEnabled(on);
            panelsMenu.setEnabled(on);
            relayoutMatchFields();
        });
        return menuItem;
    }

    private static JMenu getMenu_MultiplayerFieldLayout() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("lblMultiplayerFieldLayout"));
        final ButtonGroup group = new ButtonGroup();
        final String current = prefs.getPref(FPref.UI_MULTIPLAYER_FIELD_LAYOUT);

        final String[] values = {"GRID", "ROWS"};
        final String[] labelKeys = {"lblFieldLayoutGrid", "lblFieldLayoutRows"};
        final String[] tooltipKeys = {"lblFieldLayoutGridTooltip", "lblFieldLayoutRowsTooltip"};

        for (int i = 0; i < values.length; i++) {
            final JRadioButtonMenuItem item = createStayOpenRadioButton(
                    localizer.getMessage(labelKeys[i]));
            item.setToolTipText(localizer.getMessage(tooltipKeys[i]));
            item.setSelected(values[i].equals(current));
            final String value = values[i];
            item.addActionListener(e -> {
                prefs.setPref(FPref.UI_MULTIPLAYER_FIELD_LAYOUT, value);
                prefs.save();
                relayoutMatchFields();
            });
            group.add(item);
            menu.add(item);
        }
        return menu;
    }

    private static JMenu getMenu_MultiplayerFieldPanels() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("lblMultiplayerFieldPanels"));
        final ButtonGroup group = new ButtonGroup();
        final String current = prefs.getPref(FPref.UI_MULTIPLAYER_FIELD_PANELS);

        final String[] values = {"TABBED", "SPLIT"};
        final String[] labelKeys = {"lblFieldPanelsTabbed", "lblFieldPanelsSplit"};
        final String[] tooltipKeys = {"lblFieldPanelsTabbedTooltip", "lblFieldPanelsSplitTooltip"};

        for (int i = 0; i < values.length; i++) {
            final JRadioButtonMenuItem item = createStayOpenRadioButton(
                    localizer.getMessage(labelKeys[i]));
            item.setToolTipText(localizer.getMessage(tooltipKeys[i]));
            item.setSelected(values[i].equals(current));
            final String value = values[i];
            item.addActionListener(e -> {
                prefs.setPref(FPref.UI_MULTIPLAYER_FIELD_PANELS, value);
                prefs.save();
                relayoutMatchFields();
            });
            group.add(item);
            menu.add(item);
        }
        return menu;
    }

    private static void relayoutMatchFields() {
        final FScreen screen = Singletons.getControl().getCurrentScreen();
        if (screen != null && screen.isMatchScreen()) {
            final IVTopLevelUI view = screen.getView();
            if (view instanceof VMatchUI) {
                ((VMatchUI) view).relayoutMultiplayerFields();
            }
        }
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
        return e -> {
            final boolean showTabs = menuItem.getState();
            FView.SINGLETON_INSTANCE.refreshAllCellLayouts(showTabs);
            prefs.setPref(FPref.UI_HIDE_GAME_TABS, !showTabs);
            prefs.save();
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
        return e -> SLayoutIO.saveLayout();
    }

    private JMenuItem getMenuItem_OpenLayout() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblOpen") +"..");
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_OPENLAYOUT) : null));
        menuItem.addActionListener(getOpenLayoutAction());
        return menuItem;
    }

    private static ActionListener getOpenLayoutAction() {
        return e -> SLayoutIO.openLayout();
    }

    private JMenuItem getMenuItem_RevertLayout() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblRefresh"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_REVERTLAYOUT) : null));
        menuItem.addActionListener(getRevertLayoutAction());
        return menuItem;
    }

    private static ActionListener getRevertLayoutAction() {
        return e -> SLayoutIO.revertLayout();
    }

    private static JMenuItem getMenuItem_SetWindowSize() {
        final Localizer localizer = Localizer.getInstance();
        final JMenuItem menuItem = new JMenuItem(localizer.getMessage("lblSetWindowSize"));
        menuItem.addActionListener(getSetWindowSizeAction());
        return menuItem;
    }

    private static ActionListener getSetWindowSizeAction() {
        return e -> {
            final String[] options = {"800x600", "1024x768", "1280x720", "1600x900", "1920x1080", "2560x1440", "3840x2160"};
            final Localizer localizer = Localizer.getInstance();
            final String choice = GuiChoose.oneOrNone(localizer.getMessage("lblChooseNewWindowSize"), options);
            if (choice != null) {
                final String[] dims = choice.split("x");
                Singletons.getView().getFrame().setSize(Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
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
        return e -> {
            final FFrame frame = Singletons.getView().getFrame();
            frame.setFullScreen(!frame.isFullScreen());
        };
    }
}
