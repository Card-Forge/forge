package forge.screens.match.menus;

import forge.assets.FSkinProp;
import forge.match.MatchUtil;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.CMatchUI;
import forge.screens.match.controllers.CDock;
import forge.toolbox.FSkin.*;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Returns a JMenu containing options associated with current game.
 * <p>
 * Replicates options available in Dock tab.
 */
public final class GameMenu {
    private GameMenu() { }

    private static CDock controller =  CDock.SINGLETON_INSTANCE;
    private static ForgePreferences prefs = FModel.getPreferences();
    private static boolean showIcons;

    public static JMenu getMenu(boolean showMenuIcons) {
        JMenu menu = new JMenu("Game");
        menu.setMnemonic(KeyEvent.VK_G);
        menu.add(getMenuItem_Undo());
        menu.add(getMenuItem_Concede());
        menu.add(getMenuItem_EndTurn());
        menu.add(getMenuItem_AlphaStrike());
        menu.addSeparator();
        menu.add(getMenuItem_TargetingArcs());
        menu.add(CardOverlaysMenu.getMenu(showMenuIcons));
        menu.addSeparator();
        menu.add(getMenuItem_GameSoundEffects());
        menu.addSeparator();
        menu.add(getMenuItem_ViewDeckList());
        return menu;
    }

    private static SkinnedCheckBoxMenuItem getMenuItem_GameSoundEffects() {
        SkinnedCheckBoxMenuItem menuItem = new SkinnedCheckBoxMenuItem("Sound Effects");
        menuItem.setState(prefs.getPrefBoolean(FPref.UI_ENABLE_SOUNDS));
        menuItem.addActionListener(getSoundEffectsAction());
        return menuItem;
    }
    private static ActionListener getSoundEffectsAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                toggleGameSoundEffects();
            }
        };
    }
    private static void toggleGameSoundEffects() {
        boolean isSoundEffectsEnabled = !prefs.getPrefBoolean(FPref.UI_ENABLE_SOUNDS);
        prefs.setPref(FPref.UI_ENABLE_SOUNDS, isSoundEffectsEnabled);
        prefs.save();
    }

    private static SkinnedMenuItem getMenuItem_Undo() {
        SkinnedMenuItem menuItem = new SkinnedMenuItem("Undo");
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_Z));
        menuItem.addActionListener(getUndoAction());
        return menuItem;
    }

    private static ActionListener getUndoAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MatchUtil.undoLastAction();
            }
        };
    }

    private static SkinnedMenuItem getMenuItem_Concede() {
        SkinnedMenuItem menuItem = new SkinnedMenuItem("Concede");
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_CONCEDE) : null));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_Q));
        menuItem.addActionListener(getConcedeAction());
        return menuItem;
    }

    private static ActionListener getConcedeAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CMatchUI.SINGLETON_INSTANCE.concede();
            }
        };
    }

    private static SkinnedMenuItem getMenuItem_AlphaStrike() {
        SkinnedMenuItem menuItem = new SkinnedMenuItem("Alpha Strike");
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_ALPHASTRIKE) : null));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_A));
        menuItem.addActionListener(getAlphaStrikeAction());
        return menuItem;
    }

    private static ActionListener getAlphaStrikeAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.alphaStrike();
            }
        };
    }

    private static SkinnedMenuItem getMenuItem_EndTurn() {
        SkinnedMenuItem menuItem = new SkinnedMenuItem("End Turn");
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_ENDTURN) : null));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_E));
        menuItem.addActionListener(getEndTurnAction());
        return menuItem;
    }

    private static ActionListener getEndTurnAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.endTurn();
            }
        };
    }

    private static SkinnedMenu getMenuItem_TargetingArcs() {
        SkinnedMenu menu = new SkinnedMenu("Targeting Arcs");
        ButtonGroup group = new ButtonGroup();

        SkinIcon menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSOFF);

        SkinnedRadioButtonMenuItem menuItem;
        menuItem = getTargetingArcRadioButton("Off", FSkinProp.ICO_ARCSOFF, 0);
        if (menuItem.isSelected()) { menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSOFF); }
        group.add(menuItem);
        menu.add(menuItem);
        menuItem = getTargetingArcRadioButton("Card mouseover", FSkinProp.ICO_ARCSHOVER, 1);
        if (menuItem.isSelected()) { menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSHOVER); }
        group.add(menuItem);
        menu.add(menuItem);
        menuItem = getTargetingArcRadioButton("Always On", FSkinProp.ICO_ARCSON, 2);
        if (menuItem.isSelected()) { menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSON); }
        group.add(menuItem);

        menu.setIcon((showIcons ? menuIcon : null));
        menu.add(menuItem);

        return menu;
    }

    private static SkinnedRadioButtonMenuItem getTargetingArcRadioButton(String caption, FSkinProp icon, final int arcState) {
        final SkinnedRadioButtonMenuItem menuItem = new SkinnedRadioButtonMenuItem(caption);
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(icon) : null));
        menuItem.setSelected(arcState == controller.getArcState());
        menuItem.addActionListener(getTargetingRadioButtonAction(arcState));
        return menuItem;
    }

    private static ActionListener getTargetingRadioButtonAction(final int arcState) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                prefs.setPref(FPref.UI_TARGETING_OVERLAY, String.valueOf(arcState));
                prefs.save();
                controller.setArcState(arcState);
                setTargetingArcMenuIcon((SkinnedMenuItem)e.getSource());
            }
        };
    }

    private static void setTargetingArcMenuIcon(SkinnedMenuItem item) {
        JPopupMenu pop = (JPopupMenu)item.getParent();
        JMenu menu = (JMenu)pop.getInvoker();
        menu.setIcon(item.getIcon());
    }

    private static SkinnedMenuItem getMenuItem_ViewDeckList() {
        SkinnedMenuItem menuItem = new SkinnedMenuItem("Deck List");
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_DECKLIST) : null));
        menuItem.addActionListener(getViewDeckListAction());
        return menuItem;
    }

    private static ActionListener getViewDeckListAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.viewDeckList();
            }
        };
    }

}
