package forge.screens.match.menus;

import forge.Singletons;
import forge.menus.MenuUtil;
import forge.screens.match.controllers.CDev;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Gets a menu that replicates all the DevMode options.
 * <p>
 * Simply calls the associated method in CDev.
 */
public class DevModeMenu implements ActionListener {
    private DevModeMenu() { };

    private static DevModeMenu devMenu = new DevModeMenu();

    // Using an enum to avoid having to create multiple
    // ActionListeners each calling a single method.
    private enum DevMenuItem {
        GENERATE_MANA("Generate Mana"),
        TUTOR_FOR_CARD("Tutor for Card"),
        ADD_CARD_TO_HAND("Add card to hand"),
        ADD_CARD_TO_PLAY("Add card to play"),
        RIGGED_PLANAR_ROLL("Rigged planar roll"),
        PLANESWALK_TO("Planeswalk to"),
        PLAY_MANY_LANDS("Play many lands per Turn"),
        SETUP_GAME_STATE("Setup Game State"),
        ADD_COUNTER("Add Counter to Permanent"),
        TAP_PERMANENT("Tap Permanent"),
        UNTAP_PERMANENT("Untap Permanent"),
        SET_PLAYER_LIFE("Set Player Life"),
        DEV_CORNER("Developer's Corner");

        protected String caption;
        private DevMenuItem(String value) {
            this.caption = value;
        }
        protected static DevMenuItem getValue(String s) {
            for (DevMenuItem t : DevMenuItem.values()) {
                if (t.caption == s)
                    return t;
            }
            return null;
        }
    };

    private static CDev controller = CDev.SINGLETON_INSTANCE;

    public static JMenu getMenu() {
        JMenu menu = new JMenu("Dev");
        menu.setMnemonic(KeyEvent.VK_D);
        menu.add(getMenuItem(DevMenuItem.GENERATE_MANA));
        menu.add(getMenuItem(DevMenuItem.TUTOR_FOR_CARD));
        menu.addSeparator();
        menu.add(getMenuItem(DevMenuItem.ADD_CARD_TO_HAND));
        menu.add(getMenuItem(DevMenuItem.ADD_CARD_TO_PLAY));
        menu.addSeparator();
        menu.add(getMenuItem(DevMenuItem.RIGGED_PLANAR_ROLL));
        menu.add(getMenuItem(DevMenuItem.PLANESWALK_TO));
        menu.addSeparator();
        menu.add(getCheckboxMenuItem(DevMenuItem.PLAY_MANY_LANDS, Singletons.getControl().getGameView().devGetUnlimitedLands()));
        menu.add(getMenuItem(DevMenuItem.SETUP_GAME_STATE));
        menu.add(getMenuItem(DevMenuItem.ADD_COUNTER));
        menu.addSeparator();
        menu.add(getMenuItem(DevMenuItem.TAP_PERMANENT));
        menu.add(getMenuItem(DevMenuItem.UNTAP_PERMANENT));
        menu.addSeparator();
        menu.add(getMenuItem(DevMenuItem.SET_PLAYER_LIFE));
        menu.addSeparator();
        menu.add(getMenuItem(DevMenuItem.DEV_CORNER));
        return menu;
    }

    private static JMenuItem getMenuItem(DevMenuItem m) {
        JMenuItem menuItem = new JMenuItem(m.caption);
        menuItem.addActionListener(devMenu);
        return menuItem;
    }

    private static JCheckBoxMenuItem getCheckboxMenuItem(DevMenuItem m, boolean isSelected) {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(m.caption);
        menuItem.setState(isSelected);
        menuItem.addActionListener(devMenu);
        return menuItem;
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        switch (DevMenuItem.getValue(e.getActionCommand())) {
        case GENERATE_MANA:     { controller.generateMana(); break; }
        case TUTOR_FOR_CARD:    { controller.tutorForCard(); break; }
        case ADD_CARD_TO_HAND:  { controller.addCardToHand(); break; }
        case ADD_CARD_TO_PLAY:  { controller.addCardToBattlefield(); break; }
        case RIGGED_PLANAR_ROLL:{ controller.riggedPlanerRoll(); break; }
        case PLANESWALK_TO:     { controller.planeswalkTo(); break; }
        case PLAY_MANY_LANDS:   { controller.togglePlayManyLandsPerTurn(); break; }
        case SETUP_GAME_STATE:  { controller.setupGameState(); break; }
        case ADD_COUNTER:       { controller.addCounterToPermanent(); break; }
        case TAP_PERMANENT:     { controller.tapPermanent(); break; }
        case UNTAP_PERMANENT:   { controller.untapPermanent(); break; }
        case SET_PLAYER_LIFE:   { controller.setPlayerLife(); break; }
        case DEV_CORNER:        { openDevForumInBrowser(); break; }
        }
    }

    private static void openDevForumInBrowser() {
        MenuUtil.openUrlInBrowser("http://www.slightlymagic.net/forum/viewforum.php?f=52");
    }

}
