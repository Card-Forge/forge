package forge.menus;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import forge.Singletons;
import forge.control.FControl;
import forge.control.KeyboardShortcuts;
import forge.gui.GuiChoose;
import forge.gui.MouseUtil;
import forge.gui.framework.FScreen;
import forge.gui.framework.IVTopLevelUI;
import forge.gui.framework.SLayoutIO;
import forge.game.GameLogEntryType;
import forge.game.GameLogVerbosity;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.screens.match.VMatchUI;
import forge.screens.match.views.VHand;
import forge.view.arcane.FloatingZone;
import forge.toolbox.FOptionPane;
import forge.toolbox.FButton;
import forge.toolbox.FCheckBox;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedMenuItem;
import forge.view.FDialog;
import net.miginfocom.swing.MigLayout;
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
    private static final Localizer localizer = Localizer.getInstance();

    public JMenu getMenu() {
        currentScreen = Singletons.getControl().getCurrentScreen();

        final JMenu menu = new JMenu(localizer.getMessage("lblLayout"));
        menu.setMnemonic(KeyEvent.VK_L);
        if (currentScreen != FScreen.HOME_SCREEN) {
            menu.add(getMenu_FileOptions());
        }
        menu.add(getMenu_ThemeOptions());
        if (currentScreen != null && currentScreen.isMatchScreen()) {
            menu.add(getMenuItem_ShowBackgroundImage());
        }
        menu.addSeparator();
        menu.add(getMenuItem_FullScreen());
        menu.add(getMenuItem_SetWindowSize());
        if (currentScreen != FScreen.HOME_SCREEN) {
            menu.add(getMenuItem_RevertLayout());
            menu.add(getMenuItem_ResetMatchLayout());
        }
        if (currentScreen != null && currentScreen != FScreen.HOME_SCREEN) {
            menu.addSeparator();
            menu.add(getMenuItem_ShowTabs());

            final JCheckBoxMenuItem newCountItem = MenuUtil.createStayOpenCheckBox(
                    localizer.getMessage("lblNewCardCountInTab"));
            newCountItem.setState(prefs.getPrefBoolean(FPref.UI_ZONE_TAB_NEW_COUNT));
            newCountItem.addActionListener(e -> {
                prefs.setPref(FPref.UI_ZONE_TAB_NEW_COUNT, newCountItem.getState());
                prefs.save();
                FloatingZone.refreshAll();
                refreshHandCards();
            });
            menu.add(newCountItem);

            if (currentScreen.isMatchScreen()) {
                menu.addSeparator();
                menu.add(getMenu_HandOptions());
                menu.add(getMenu_PromptPane());
                menu.add(getMenu_LogPane());

                // Multiplayer field ordering
                menu.addSeparator();
                final JMenu layoutMenu = getMenu_MultiplayerFieldLayout();
                final JMenu panelsMenu = getMenu_MultiplayerFieldPanels();
                menu.add(getMenuItem_SortMultiplayerFields(layoutMenu, panelsMenu));
                menu.add(layoutMenu);
                menu.add(panelsMenu);
            }
        }
        return menu;
    }

    private JMenu getMenu_FileOptions() {
        final JMenu menu = new JMenu(localizer.getMessage("lblFile"));
        menu.add(getMenuItem_OpenLayout());
        menu.add(getMenuItem_SaveLayout());
        return menu;
    }

    private static JMenu getMenu_ThemeOptions() {
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
        final JCheckBoxMenuItem menuItem = MenuUtil.createStayOpenCheckBox(localizer.getMessage("lblBackgroundImage"));
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

    private static JMenu getMenu_HandOptions() {
        final JMenu menu = new JMenu(localizer.getMessage("lblHandPanel"));

        // Order Hand by CMC and Color
        final JCheckBoxMenuItem orderHandItem = MenuUtil.createStayOpenCheckBox(localizer.getMessage("nlOrderHand"));
        orderHandItem.setState(prefs.getPrefBoolean(FPref.UI_ORDER_HAND));
        orderHandItem.addActionListener(e -> {
            prefs.setPref(FPref.UI_ORDER_HAND, orderHandItem.getState());
            prefs.save();
            refreshHandCards();
            refreshHandLayout();
        });
        menu.add(orderHandItem);

        // Prevent Card Overlap
        final JCheckBoxMenuItem noOverlapItem = MenuUtil.createStayOpenCheckBox(localizer.getMessage("lblPreventCardOverlap"));
        noOverlapItem.setState(prefs.getPrefBoolean(FPref.UI_HAND_NO_OVERLAP));
        noOverlapItem.addActionListener(e -> {
            prefs.setPref(FPref.UI_HAND_NO_OVERLAP, noOverlapItem.getState());
            prefs.save();
            refreshHandLayout();
        });
        menu.add(noOverlapItem);

        // Limit Cards Per Row — checkbox + slider
        int currentMax = prefs.getPrefInt(FPref.UI_HAND_MAX_CARDS_PER_ROW);
        final boolean limitEnabled = currentMax > 0;

        final java.awt.Color bg = javax.swing.UIManager.getColor("PopupMenu.background");
        final JPanel sliderPanel = new JPanel(new java.awt.BorderLayout());
        sliderPanel.setBackground(bg);

        final JCheckBoxMenuItem limitItem = MenuUtil.createStayOpenCheckBox(localizer.getMessage("lblLimitCardsPerRow"));
        limitItem.setState(limitEnabled);

        final FSkin.SkinnedSlider slider = new FSkin.SkinnedSlider(javax.swing.SwingConstants.HORIZONTAL, 4, 12,
                limitEnabled ? currentMax : 8);
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);
        slider.setBackground(bg);
        slider.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        slider.setFont(FSkin.getFont());
        slider.setEnabled(limitEnabled);

        limitItem.addActionListener(e -> {
            final boolean on = limitItem.getState();
            slider.setEnabled(on);
            if (on) {
                prefs.setPref(FPref.UI_HAND_MAX_CARDS_PER_ROW, String.valueOf(slider.getValue()));
            } else {
                prefs.setPref(FPref.UI_HAND_MAX_CARDS_PER_ROW, "0");
            }
            prefs.save();
            refreshHandLayout();
        });

        slider.addChangeListener(e -> {
            if (limitItem.getState()) {
                prefs.setPref(FPref.UI_HAND_MAX_CARDS_PER_ROW, String.valueOf(slider.getValue()));
                prefs.save();
                refreshHandLayout();
            }
        });

        sliderPanel.add(slider, java.awt.BorderLayout.CENTER);

        menu.add(limitItem);
        menu.add(sliderPanel);

        return menu;
    }

    private static void refreshHandCards() {
        final FScreen screen = Singletons.getControl().getCurrentScreen();
        if (screen != null && screen.isMatchScreen()) {
            final IVTopLevelUI view = screen.getView();
            if (view instanceof VMatchUI vmu) {
                for (final VHand h : vmu.getControl().getHandViews()) {
                    h.getLayoutControl().updateHand();
                }
            }
        }
    }

    private static void refreshHandLayout() {
        final FScreen screen = Singletons.getControl().getCurrentScreen();
        if (screen != null && screen.isMatchScreen()) {
            final IVTopLevelUI view = screen.getView();
            if (view instanceof VMatchUI vmu) {
                int maxCards = prefs.getPrefInt(FPref.UI_HAND_MAX_CARDS_PER_ROW);
                final boolean noOverlap = prefs.getPrefBoolean(FPref.UI_HAND_NO_OVERLAP);
                for (final VHand h : vmu.getControl().getHandViews()) {
                    h.getHandArea().setMaxCardsPerRow(maxCards);
                    h.getHandArea().setNoOverlap(noOverlap);
                    h.getHandArea().doLayout();
                    h.getHandArea().repaint();
                }
            }
        }
    }

    private static JCheckBoxMenuItem getMenuItem_SortMultiplayerFields(
            final JMenu layoutMenu, final JMenu panelsMenu) {
        final boolean enabled = !"OFF".equals(prefs.getPref(FPref.UI_MULTIPLAYER_FIELD_LAYOUT));
        final JCheckBoxMenuItem menuItem = MenuUtil.createStayOpenCheckBox(
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
        final JMenu menu = new JMenu(localizer.getMessage("lblMultiplayerFieldLayout"));
        final ButtonGroup group = new ButtonGroup();
        final String current = prefs.getPref(FPref.UI_MULTIPLAYER_FIELD_LAYOUT);

        final String[] values = {"GRID", "ROWS"};
        final String[] labelKeys = {"lblFieldLayoutGrid", "lblFieldLayoutRows"};
        final String[] tooltipKeys = {"lblFieldLayoutGridTooltip", "lblFieldLayoutRowsTooltip"};

        for (int i = 0; i < values.length; i++) {
            final JRadioButtonMenuItem item = MenuUtil.createStayOpenRadioButton(
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
        final JMenu menu = new JMenu(localizer.getMessage("lblMultiplayerFieldPanels"));
        final ButtonGroup group = new ButtonGroup();
        final String current = prefs.getPref(FPref.UI_MULTIPLAYER_FIELD_PANELS);

        final String[] values = {"TABBED", "SPLIT"};
        final String[] labelKeys = {"lblFieldPanelsTabbed", "lblFieldPanelsSplit"};
        final String[] tooltipKeys = {"lblFieldPanelsTabbedTooltip", "lblFieldPanelsSplitTooltip"};

        for (int i = 0; i < values.length; i++) {
            final JRadioButtonMenuItem item = MenuUtil.createStayOpenRadioButton(
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

    private static JMenu getMenu_PromptPane() {
        final JMenu menu = new JMenu(localizer.getMessage("lblPromptPanel"));
        MenuUtil.addPrefCheckBox(menu, localizer.getMessage("cbCompactPrompt"), FPref.UI_COMPACT_PROMPT)
                .addActionListener(e -> repopulatePrompt());
        MenuUtil.addPrefCheckBox(menu, localizer.getMessage("lblShowSpellDescription"), FPref.UI_DETAILED_SPELLDESC_IN_PROMPT);
        MenuUtil.addPrefCheckBox(menu, localizer.getMessage("cbHideReminderText"), FPref.UI_HIDE_REMINDER_TEXT);
        MenuUtil.addPrefCheckBox(menu, localizer.getMessage("lblShowStormCount"), FPref.UI_SHOW_STORM_COUNT_IN_PROMPT);
        MenuUtil.addPrefCheckBox(menu, localizer.getMessage("cbRemindOnPriority"), FPref.UI_REMIND_ON_PRIORITY);
        return menu;
    }

    private static JMenu getMenu_LogPane() {
        final JMenu menu = new JMenu(localizer.getMessage("lblLogPanel"));
        final GameLogVerbosity currentVerbosity = GameLogVerbosity.fromString(prefs.getPref(FPref.DEV_LOG_ENTRY_TYPE));

        MenuUtil.addPrefCheckBox(menu, localizer.getMessage("lblLogShowCardImages"), FPref.UI_LOG_SHOW_CARD_IMAGES)
                .addActionListener(e -> refreshLog());

        // Detail Level sub-submenu
        final JMenu detailMenu = new JMenu(localizer.getMessage("lblDetailLevel"));
        final ButtonGroup group = new ButtonGroup();

        // Custom Categories menu item (declared early so radio button listeners can reference it)
        final JMenuItem customItem = new JMenuItem(localizer.getMessage("lblCustomCategories") + "...");
        customItem.addActionListener(e -> showCustomLogCategoriesDialog());
        customItem.setEnabled(currentVerbosity == GameLogVerbosity.CUSTOM);

        // Preset radio buttons (Low, Medium, High)
        final String[] tooltipKeys = {"lblLogVerbosityLow", "lblLogVerbosityMedium", "lblLogVerbosityHigh"};
        final GameLogVerbosity[] presets = {GameLogVerbosity.LOW, GameLogVerbosity.MEDIUM, GameLogVerbosity.HIGH};
        for (int i = 0; i < presets.length; i++) {
            final GameLogVerbosity verbosity = presets[i];
            final JRadioButtonMenuItem item = MenuUtil.createStayOpenRadioButton(verbosity.toString());
            item.setToolTipText(localizer.getMessage(tooltipKeys[i]));
            item.setSelected(verbosity == currentVerbosity);
            item.addActionListener(e -> {
                prefs.setPref(FPref.DEV_LOG_ENTRY_TYPE, verbosity.name());
                prefs.save();
                customItem.setEnabled(false);
                refreshLog();
            });
            group.add(item);
            detailMenu.add(item);
        }

        // Custom radio button
        final JRadioButtonMenuItem customRadio = MenuUtil.createStayOpenRadioButton(
                GameLogVerbosity.CUSTOM.toString());
        customRadio.setToolTipText(localizer.getMessage("lblLogVerbosityCustom"));
        customRadio.setSelected(currentVerbosity == GameLogVerbosity.CUSTOM);
        customRadio.addActionListener(e -> {
            prefs.setPref(FPref.DEV_LOG_ENTRY_TYPE, GameLogVerbosity.CUSTOM.name());
            prefs.save();
            customItem.setEnabled(true);
            refreshLog();
        });
        group.add(customRadio);
        detailMenu.add(customRadio);

        // Custom Categories dialog item
        detailMenu.addSeparator();
        detailMenu.add(customItem);

        menu.add(detailMenu);
        return menu;
    }

    private static void refreshLog() {
        final FScreen screen = Singletons.getControl().getCurrentScreen();
        if (screen != null && screen.isMatchScreen()) {
            final IVTopLevelUI view = screen.getView();
            if (view instanceof VMatchUI) {
                ((VMatchUI) view).getControl().refreshLog();
            }
        }
    }

    private static void repopulatePrompt() {
        final FScreen screen = Singletons.getControl().getCurrentScreen();
        if (screen != null && screen.isMatchScreen()) {
            final IVTopLevelUI view = screen.getView();
            if (view instanceof VMatchUI) {
                ((VMatchUI) view).getControl().repopulatePrompt();
            }
        }
    }

    public static void showCustomLogCategoriesDialog() {
        final FDialog dlg = new FDialog();
        dlg.setTitle(localizer.getMessage("lblCustomLogSettings"));

        final JPanel checkPanel = new JPanel(new MigLayout("insets 10, gap 5 3, wrap 2, fillx"));
        checkPanel.setOpaque(false);

        final Set<GameLogEntryType> customTypes = prefs.getCustomLogTypes();
        for (final GameLogEntryType type : GameLogEntryType.values()) {
            final FCheckBox cb = new FCheckBox(type.getCaption());
            cb.setSelected(customTypes.contains(type));
            cb.addActionListener(e -> {
                final Set<GameLogEntryType> current = prefs.getCustomLogTypes();
                if (cb.isSelected()) {
                    current.add(type);
                } else {
                    current.remove(type);
                }
                prefs.setCustomLogTypes(current);
                refreshLog();
            });
            checkPanel.add(cb, "sg check");
        }

        final FScrollPane scroller = new FScrollPane(checkPanel, false);
        final FButton btnOk = new FButton("OK");
        btnOk.addActionListener(e -> dlg.dispose());

        dlg.add(scroller, "w 400!, h 300!, wrap");
        dlg.add(btnOk, "w 80!, h 26!, ax center");
        dlg.pack();
        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);
    }

    private static JMenuItem getMenuItem_ShowTabs() {
        final JCheckBoxMenuItem menuItem = MenuUtil.createStayOpenCheckBox(localizer.getMessage("lblPanelTabs"));
        final KeyStroke ks = KeyboardShortcuts.getKeyStrokeForPref(FPref.SHORTCUT_PANELTABS);
        if (ks != null) { menuItem.setAccelerator(ks); }
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
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblSaveCurrentLayout"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_SAVELAYOUT) : null));
        menuItem.addActionListener(getSaveLayoutAction());
        return menuItem;
    }

    private static ActionListener getSaveLayoutAction() {
        return e -> SLayoutIO.saveLayout();
    }

    private JMenuItem getMenuItem_OpenLayout() {
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblOpen") +"..");
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_OPENLAYOUT) : null));
        menuItem.addActionListener(getOpenLayoutAction());
        return menuItem;
    }

    private static ActionListener getOpenLayoutAction() {
        return e -> SLayoutIO.openLayout();
    }

    private JMenuItem getMenuItem_RevertLayout() {
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblRefresh"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_REVERTLAYOUT) : null));
        menuItem.addActionListener(getRevertLayoutAction());
        return menuItem;
    }

    private static ActionListener getRevertLayoutAction() {
        return e -> SLayoutIO.revertLayout();
    }

    private static JMenuItem getMenuItem_ResetMatchLayout() {
        final JMenuItem menuItem = new JMenuItem(localizer.getMessage("btnDeleteMatchUI"));
        menuItem.addActionListener(e -> {
            final String userPrompt = localizer.getMessage("AresetMatchScreenLayout");
            if (FOptionPane.showConfirmDialog(userPrompt, localizer.getMessage("TresetMatchScreenLayout"))) {
                if (FScreen.deleteMatchLayoutFile()) {
                    FOptionPane.showMessageDialog(localizer.getMessage("OKresetMatchScreenLayout"));
                }
            }
        });
        return menuItem;
    }

    private static JMenuItem getMenuItem_SetWindowSize() {
        final JMenuItem menuItem = new JMenuItem(localizer.getMessage("lblSetWindowSize"));
        menuItem.addActionListener(getSetWindowSizeAction());
        return menuItem;
    }

    private static ActionListener getSetWindowSizeAction() {
        return e -> {
            final String[] options = {"800x600", "1024x768", "1280x720", "1600x900", "1920x1080", "2560x1440", "3840x2160"};
            final String choice = GuiChoose.oneOrNone(localizer.getMessage("lblChooseNewWindowSize"), options);
            if (choice != null) {
                final String[] dims = choice.split("x");
                Singletons.getView().getFrame().setSize(Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
            }
        };
    }

    private static JMenuItem fullScreenItem;
    public static void updateFullScreenItemText() {
        fullScreenItem.setText(Singletons.getView().getFrame().isFullScreen() ? localizer.getMessage("lblExitFullScreen") : localizer.getMessage("lblFullScreen"));
    }
    private static JMenuItem getMenuItem_FullScreen() {
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
