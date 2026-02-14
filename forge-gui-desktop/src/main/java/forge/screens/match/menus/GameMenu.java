package forge.screens.match.menus;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.google.common.primitives.Ints;

import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.VAutoYields;
import forge.screens.match.views.VField;
import forge.screens.match.controllers.CDock.ArcState;
import forge.toolbox.FSkin.SkinIcon;
import forge.toolbox.FSkin.SkinnedCheckBoxMenuItem;
import forge.toolbox.FSkin.SkinnedMenu;
import forge.toolbox.FSkin.SkinnedMenuItem;
import forge.toolbox.FSkin.SkinnedRadioButtonMenuItem;
import forge.util.Localizer;

/**
 * Returns a JMenu containing options associated with current game.
 * <p>
 * Replicates options available in Dock tab.
 */
public final class GameMenu {
    private final CMatchUI matchUI;
    public GameMenu(final CMatchUI matchUI) {
        this.matchUI = matchUI;
    }

    private static ForgePreferences prefs = FModel.getPreferences();
    private static boolean showIcons;

    public JMenu getMenu() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("lblGame"));
        menu.setMnemonic(KeyEvent.VK_G);
        menu.add(getMenuItem_Undo());
        menu.add(getMenuItem_Concede());
        menu.add(getMenuItem_EndTurn());
        menu.add(getMenuItem_AlphaStrike());
        menu.addSeparator();
        menu.add(getMenuItem_TargetingArcs());
        menu.add(new CardOverlaysMenu(matchUI).getMenu());
        menu.add(getSubmenu_StackGroupPermanents());
        menu.add(getMenuItem_TokensSeparateRow());
        menu.add(getMenuItem_AutoYields());
        menu.addSeparator();
        menu.add(getMenuItem_ViewDeckList());
        menu.addSeparator();
        menu.add(getMenuItem_GameSoundEffects());
        return menu;
    }

    private static SkinnedCheckBoxMenuItem getMenuItem_GameSoundEffects() {
        final Localizer localizer = Localizer.getInstance();
        SkinnedCheckBoxMenuItem menuItem = new SkinnedCheckBoxMenuItem(localizer.getMessage("lblSoundEffects"));
        menuItem.setState(prefs.getPrefBoolean(FPref.UI_ENABLE_SOUNDS));
        menuItem.addActionListener(getSoundEffectsAction());
        return menuItem;
    }
    private static ActionListener getSoundEffectsAction() {
        return e -> toggleGameSoundEffects();
    }
    private static void toggleGameSoundEffects() {
        final boolean isSoundEffectsEnabled = !prefs.getPrefBoolean(FPref.UI_ENABLE_SOUNDS);
        prefs.setPref(FPref.UI_ENABLE_SOUNDS, isSoundEffectsEnabled);
        prefs.save();
    }

    private SkinnedMenuItem getMenuItem_Undo() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblUndo"));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_Z));
        menuItem.addActionListener(getUndoAction());
        return menuItem;
    }

    private ActionListener getUndoAction() {
        return e -> matchUI.getGameController().undoLastAction();
    }

    private SkinnedMenuItem getMenuItem_Concede() {
        SkinnedMenuItem menuItem = new SkinnedMenuItem(matchUI.getConcedeCaption());
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_CONCEDE) : null));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_Q));
        menuItem.addActionListener(getConcedeAction());
        return menuItem;
    }

    private ActionListener getConcedeAction() {
        return e -> matchUI.concede();
    }

    private SkinnedMenuItem getMenuItem_AlphaStrike() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblAlphaStrike"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_ALPHASTRIKE) : null));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_A));
        menuItem.addActionListener(getAlphaStrikeAction());
        return menuItem;
    }

    private ActionListener getAlphaStrikeAction() {
        return e -> matchUI.getGameController().alphaStrike();
    }

    private SkinnedMenuItem getMenuItem_EndTurn() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblEndTurn"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_ENDTURN) : null));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_E));
        menuItem.addActionListener(getEndTurnAction());
        return menuItem;
    }

    private ActionListener getEndTurnAction() {
        return e -> matchUI.getGameController().passPriorityUntilEndOfTurn();
    }

    private SkinnedMenu getMenuItem_TargetingArcs() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenu menu = new SkinnedMenu(localizer.getMessage("lblTargetingArcs"));
        final ButtonGroup group = new ButtonGroup();

        SkinIcon menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSOFF);

        if (matchUI.getCDock().getArcState() == null) {
            final String arcStateStr = FModel.getPreferences().getPref(FPref.UI_TARGETING_OVERLAY);
            final Integer arcState = Ints.tryParse(arcStateStr);
            matchUI.getCDock().setArcState(ArcState.values()[arcState == null ? 0 : arcState]);
        }

        SkinnedRadioButtonMenuItem menuItem;
        menuItem = getTargetingArcRadioButton(localizer.getMessage("lblOff"), FSkinProp.ICO_ARCSOFF, ArcState.OFF);
        if (menuItem.isSelected()) { menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSOFF); }
        group.add(menuItem);
        menu.add(menuItem);
        menuItem = getTargetingArcRadioButton(localizer.getMessage("lblCardMouseOver"), FSkinProp.ICO_ARCSHOVER, ArcState.MOUSEOVER);
        if (menuItem.isSelected()) { menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSHOVER); }
        group.add(menuItem);
        menu.add(menuItem);
        menuItem = getTargetingArcRadioButton(localizer.getMessage("lblAlwaysOn"), FSkinProp.ICO_ARCSON, ArcState.ON);
        if (menuItem.isSelected()) { menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSON); }
        group.add(menuItem);

        menu.setIcon((showIcons ? menuIcon : null));
        menu.add(menuItem);

        return menu;
    }

    private SkinnedRadioButtonMenuItem getTargetingArcRadioButton(final String caption, final FSkinProp icon, final ArcState arcState) {
        final SkinnedRadioButtonMenuItem menuItem = new SkinnedRadioButtonMenuItem(caption);
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(icon) : null));
        menuItem.setSelected(arcState == matchUI.getCDock().getArcState());
        menuItem.addActionListener(getTargetingRadioButtonAction(arcState));
        return menuItem;
    }

    private ActionListener getTargetingRadioButtonAction(final ArcState arcState) {
        return e -> {
            prefs.setPref(FPref.UI_TARGETING_OVERLAY, String.valueOf(arcState.ordinal()));
            prefs.save();
            matchUI.getCDock().setArcState(arcState);
            setTargetingArcMenuIcon((SkinnedRadioButtonMenuItem)e.getSource());
        };
    }

    private static void setTargetingArcMenuIcon(SkinnedRadioButtonMenuItem item) {
        final JPopupMenu pop = (JPopupMenu)item.getParent();
        final JMenu menu = (JMenu)pop.getInvoker();
        menu.setIcon(item.getIcon());
    }

    private SkinnedMenuItem getMenuItem_AutoYields() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblAutoYields"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_WARNING) : null));
        menuItem.addActionListener(getAutoYieldsAction());
        return menuItem;
    }

    private ActionListener getAutoYieldsAction() {
        return e -> {
            final VAutoYields autoYields = new VAutoYields(matchUI);
            autoYields.showAutoYields();
        };
    }

    private SkinnedMenuItem getMenuItem_ViewDeckList() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblDeckList"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_DECKLIST) : null));
        menuItem.addActionListener(getViewDeckListAction());
        return menuItem;
    }

    private ActionListener getViewDeckListAction() {
        return e -> matchUI.viewDeckList();
    }

    private SkinnedMenu getSubmenu_StackGroupPermanents() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenu submenu = new SkinnedMenu(localizer.getMessage("cbpStackGroupPermanents"));
        final ButtonGroup group = new ButtonGroup();
        final String current = prefs.getPref(FPref.UI_GROUP_PERMANENTS);

        final String[] options = {"Default", "Stack Creatures", "Group Creatures/Tokens", "Group All Permanents"};
        final String[] tooltips = {
            "Creatures are never grouped or stacked. Identical lands and tokens are stacked (up to 5). Identical artifacts and enchantments are stacked (up to 4). Stacking fans cards out so each copy is partially visible.",
            "Same as Default, but creatures are also stacked (up to 4).",
            "Group identical creatures and tokens (5 or more) into a single compact pile with a count badge.",
            "Group all identical permanents (5 or more) into a single compact pile with a count badge."
        };
        for (int i = 0; i < options.length; i++) {
            SkinnedRadioButtonMenuItem item = new SkinnedRadioButtonMenuItem(options[i]);
            item.setToolTipText(tooltips[i]);
            item.setSelected(options[i].equals(current));
            item.addActionListener(getGroupPermanentsAction(options[i]));
            group.add(item);
            submenu.add(item);
        }
        return submenu;
    }

    private SkinnedCheckBoxMenuItem getMenuItem_TokensSeparateRow() {
        final Localizer localizer = Localizer.getInstance();
        SkinnedCheckBoxMenuItem menuItem = new SkinnedCheckBoxMenuItem(localizer.getMessage("cbpTokensSeparateRow"));
        menuItem.setToolTipText("Show tokens in their own row instead of mixed with creatures.");
        menuItem.setState(prefs.getPrefBoolean(FPref.UI_TOKENS_IN_SEPARATE_ROW));
        menuItem.addActionListener(e -> {
            final boolean enabled = !prefs.getPrefBoolean(FPref.UI_TOKENS_IN_SEPARATE_ROW);
            prefs.setPref(FPref.UI_TOKENS_IN_SEPARATE_ROW, enabled);
            prefs.save();
            SwingUtilities.invokeLater(() -> {
                for (final VField f : matchUI.getFieldViews()) {
                    f.getTabletop().doLayout();
                }
            });
        });
        return menuItem;
    }

    private ActionListener getGroupPermanentsAction(final String value) {
        return e -> {
            prefs.setPref(FPref.UI_GROUP_PERMANENTS, value);
            prefs.save();
            SwingUtilities.invokeLater(() -> {
                for (final VField f : matchUI.getFieldViews()) {
                    f.getTabletop().doLayout();
                }
            });
        };
    }
}
