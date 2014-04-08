package forge.screens.home.sanctioned;

import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Returns a JMenu containing options for constructed game.
 */

public final class ConstructedGameMenu {
    private ConstructedGameMenu() { }

    private static ForgePreferences prefs = FModel.getPreferences();

    public static JMenu getMenu() {
        JMenu menu = new JMenu("Game");
        menu.setMnemonic(KeyEvent.VK_G);
        menu.add(getMenuItem_SingletonMode());
        menu.add(getMenuItem_ArtifactsMode());
        menu.add(getMenuItem_SmallCreaturesMode());
        return menu;
    }

    private static JMenuItem getMenuItem_SmallCreaturesMode() {
        JCheckBoxMenuItem menu = new JCheckBoxMenuItem("Remove Small Creatures");
        MenuUtil.setMenuHint(menu, "Remove 1/1 and 0/X creatures in generated decks.");
        menu.setState(prefs.getPrefBoolean(FPref.DECKGEN_NOSMALL));
        menu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSmallCreaturesMode(((JMenuItem)e.getSource()).isSelected());
            }
        });
        return menu;
    }

    /**
     * Enables/disables 1/1 and 0/X creatures in generated decks.
     */
    private static void setSmallCreaturesMode(boolean b) {
        prefs.setPref(FPref.DECKGEN_NOSMALL, b);
        prefs.save();
    }

    private static JMenuItem getMenuItem_ArtifactsMode() {
        JCheckBoxMenuItem menu = new JCheckBoxMenuItem("Remove Artifacts");
        MenuUtil.setMenuHint(menu, "Remove artifact cards in generated decks.");
        menu.setState(prefs.getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
        menu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setArtifactsMode(((JMenuItem)e.getSource()).isSelected());
            }
        });
        return menu;
    }

    /**
     * Enables/disables artifact cards in generated decks.

     */
    private static void setArtifactsMode(boolean b) {
        prefs.setPref(FPref.DECKGEN_ARTIFACTS, b);
        prefs.save();
    }

    private static JMenuItem getMenuItem_SingletonMode() {
        JCheckBoxMenuItem menu = new JCheckBoxMenuItem("Singleton Mode");
        MenuUtil.setMenuHint(menu, "Prevent non-land duplicates in generated decks.");
        menu.setState(prefs.getPrefBoolean(FPref.DECKGEN_SINGLETONS));
        menu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSingletonMode(((JMenuItem)e.getSource()).isSelected());
            }
        });
        return menu;
    }

    /**
     * Enables/disables non-land duplicates in generated decks.
     */
    private static void setSingletonMode(boolean b) {
        prefs.setPref(FPref.DECKGEN_SINGLETONS, b);
        prefs.save();
    }

}