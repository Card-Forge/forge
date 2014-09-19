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
        ADD_CARD_TO_HAND("Add Card to Hand"),
        ADD_CARD_TO_PLAY("Add Card to Play"),
        SET_PLAYER_LIFE("Set Player Life"),
        WIN_GAME("Win Game"),
        SETUP_GAME_STATE("Setup Game State"),
        PLAY_UNLIMITED_LANDS("Play Unlimited Lands"),
        ADD_COUNTER("Add Counters to Permanent"),
        TAP_PERMANENT("Tap Permanents"),
        UNTAP_PERMANENT("Untap Permanents"),
        RIGGED_PLANAR_ROLL("Rigged Planar Roll"),
        PLANESWALK_TO("Planeswalk to"),
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
        menu.add(getMenuItem(DevMenuItem.SET_PLAYER_LIFE));
        menu.add(getMenuItem(DevMenuItem.WIN_GAME));
        menu.addSeparator();
        menu.add(getMenuItem(DevMenuItem.SETUP_GAME_STATE));
        menu.add(getCheckboxMenuItem(DevMenuItem.PLAY_UNLIMITED_LANDS, Singletons.getControl().getGameView().devGetUnlimitedLands()));
        menu.add(getMenuItem(DevMenuItem.ADD_COUNTER));
        menu.addSeparator();
        menu.add(getMenuItem(DevMenuItem.TAP_PERMANENT));
        menu.add(getMenuItem(DevMenuItem.UNTAP_PERMANENT));
        menu.addSeparator();
        menu.add(getMenuItem(DevMenuItem.RIGGED_PLANAR_ROLL));
        menu.add(getMenuItem(DevMenuItem.PLANESWALK_TO));
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
        case SET_PLAYER_LIFE:   { controller.setPlayerLife(); break; }
        case WIN_GAME:          { controller.winGame(); break; }
        case SETUP_GAME_STATE:  { controller.setupGameState(); break; }
        case PLAY_UNLIMITED_LANDS:   { controller.togglePlayManyLandsPerTurn(); break; }
        case ADD_COUNTER:       { controller.addCounterToPermanent(); break; }
        case TAP_PERMANENT:     { controller.tapPermanent(); break; }
        case UNTAP_PERMANENT:   { controller.untapPermanent(); break; }
        case RIGGED_PLANAR_ROLL:{ controller.riggedPlanerRoll(); break; }
        case PLANESWALK_TO:     { controller.planeswalkTo(); break; }
        case DEV_CORNER:        { openDevForumInBrowser(); break; }
        default:
            break;
        }
    }

    private static void openDevForumInBrowser() {
        MenuUtil.openUrlInBrowser("http://www.slightlymagic.net/forum/viewforum.php?f=52");
    }

}
