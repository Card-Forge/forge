package forge.screens.home.sanctioned;

import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.util.Localizer;

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
        final Localizer localizer = Localizer.getInstance();
        JMenu menu = new JMenu(localizer.getMessage("lblGame"));
        menu.setMnemonic(KeyEvent.VK_G);
        menu.add(getMenuItem_SingletonMode());
        menu.add(getMenuItem_ArtifactsMode());
        menu.add(getMenuItem_SmallCreaturesMode());
        return menu;
    }

    private static JMenuItem getMenuItem_SmallCreaturesMode() {
        final Localizer localizer = Localizer.getInstance();
        JCheckBoxMenuItem menu = new JCheckBoxMenuItem(localizer.getMessage("cbRemoveSmall"));
        MenuUtil.setMenuHint(menu, localizer.getMessage("lblRemoveSmallCreatures"));
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
        final Localizer localizer = Localizer.getInstance();
        JCheckBoxMenuItem menu = new JCheckBoxMenuItem(localizer.getMessage("cbRemoveArtifacts"));
        MenuUtil.setMenuHint(menu, localizer.getMessage("lblRemoveArtifacts"));
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
        final Localizer localizer = Localizer.getInstance();
        JCheckBoxMenuItem menu = new JCheckBoxMenuItem(localizer.getMessage("cbSingletons"));
        MenuUtil.setMenuHint(menu, localizer.getMessage("PreventNonLandDuplicates"));
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