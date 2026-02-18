package forge.screens.match.menus;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import com.google.common.primitives.Ints;

import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.VAutoYields;
import forge.screens.match.controllers.CDock.ArcState;
import forge.toolbox.FSkin.SkinIcon;
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
        if (prefs.getPrefBoolean(FPref.YIELD_EXPERIMENTAL_OPTIONS)) {
            menu.add(getYieldOptionsMenu());
        } else {
            menu.add(getMenuItem_AutoYields());
        }
        menu.addSeparator();
        menu.add(getMenuItem_ViewDeckList());
        return menu;
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

    private JMenu getYieldOptionsMenu() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu yieldMenu = new JMenu(localizer.getMessage("lblYieldOptions"));

        // Auto-Yields (manage per-ability yields)
        yieldMenu.add(getMenuItem_AutoYields());
        yieldMenu.addSeparator();

        // Sub-menu 1: Interrupt Settings
        final JMenu interruptMenu = new JMenu(localizer.getMessage("lblInterruptSettings"));
        interruptMenu.add(createYieldCheckbox(localizer.getMessage("lblInterruptOnAttackers"), FPref.YIELD_INTERRUPT_ON_ATTACKERS));
        interruptMenu.add(createYieldCheckbox(localizer.getMessage("lblInterruptOnBlockers"), FPref.YIELD_INTERRUPT_ON_BLOCKERS));
        interruptMenu.add(createYieldCheckbox(localizer.getMessage("lblInterruptOnTargeting"), FPref.YIELD_INTERRUPT_ON_TARGETING));
        interruptMenu.add(createYieldCheckbox(localizer.getMessage("lblInterruptOnMassRemoval"), FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL));
        interruptMenu.add(createYieldCheckbox(localizer.getMessage("lblInterruptOnOpponentSpell"), FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL));
        interruptMenu.add(createYieldCheckbox(localizer.getMessage("lblInterruptOnCombat"), FPref.YIELD_INTERRUPT_ON_COMBAT));
        interruptMenu.add(createYieldCheckbox(localizer.getMessage("lblInterruptOnReveal"), FPref.YIELD_INTERRUPT_ON_REVEAL));
        yieldMenu.add(interruptMenu);

        // Sub-menu 2: Automatic Suggestions
        final JMenu suggestionsMenu = new JMenu(localizer.getMessage("lblAutomaticSuggestions"));
        suggestionsMenu.add(createYieldCheckbox(localizer.getMessage("lblSuggestStackYield"), FPref.YIELD_SUGGEST_STACK_YIELD));
        suggestionsMenu.add(createYieldCheckbox(localizer.getMessage("lblSuggestNoMana"), FPref.YIELD_SUGGEST_NO_MANA));
        suggestionsMenu.add(createYieldCheckbox(localizer.getMessage("lblSuggestNoActions"), FPref.YIELD_SUGGEST_NO_ACTIONS));
        suggestionsMenu.addSeparator();
        suggestionsMenu.add(createYieldCheckbox(localizer.getMessage("lblSuppressOnOwnTurn"), FPref.YIELD_SUPPRESS_ON_OWN_TURN));
        suggestionsMenu.add(createYieldCheckbox(localizer.getMessage("lblSuppressAfterYield"), FPref.YIELD_SUPPRESS_AFTER_END));
        yieldMenu.add(suggestionsMenu);

        return yieldMenu;
    }

    private JCheckBoxMenuItem createYieldCheckbox(String label, FPref pref) {
        // Custom checkbox that doesn't close the menu when clicked
        final JCheckBoxMenuItem item = new JCheckBoxMenuItem(label) {
            @Override
            protected void processMouseEvent(java.awt.event.MouseEvent e) {
                if (e.getID() == java.awt.event.MouseEvent.MOUSE_RELEASED && contains(e.getPoint())) {
                    doClick(0);
                    setArmed(true);
                } else {
                    super.processMouseEvent(e);
                }
            }
        };
        item.setSelected(prefs.getPrefBoolean(pref));
        item.addActionListener(e -> {
            prefs.setPref(pref, item.isSelected());
            prefs.save();
        });
        return item;
    }
}
