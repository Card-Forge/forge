package forge.control;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.apache.commons.lang3.StringUtils;

import forge.Singletons;
import forge.gui.framework.EDocID;
import forge.gui.framework.SDisplayUtil;
import forge.gui.home.settings.VSubmenuPreferences.KeyboardShortcutField;
import forge.gui.match.controllers.CDock;
import forge.properties.ForgePreferences.FPref;

/** 
 * Consolidates keyboard shortcut assembly into one location
 * for all shortcuts in the project.
 * 
 * Just map a new Shortcut object here, set a default in preferences,
 * and you're done.
 */
public class KeyboardShortcuts {
    /**
     * Attaches all keyboard shortcuts for match UI,
     * and returns a list of shortcuts with necessary properties for later access.
     *
     * @return List<Shortcut> Shortcut objects
     */
    @SuppressWarnings("serial")
    public static List<Shortcut> attachKeyboardShortcuts() {
        final JComponent c = (JComponent) Singletons.getView().getFrame().getContentPane();
        final InputMap im = c.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        final ActionMap am = c.getActionMap();

        //final ForgePreferences fp = Singletons.getModel().getPreferences();

        List<Shortcut> list = new ArrayList<Shortcut>();

        //========== Match Shortcuts
        /** Show stack. */
        final Action actShowStack = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (Singletons.getControl().getState() != 1) { return; }
                SDisplayUtil.showTab(EDocID.MATCH_STACK.getDoc());
            }
        };

        /** Show combat. */
        final Action actShowCombat = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (Singletons.getControl().getState() != 1) { return; }
                SDisplayUtil.showTab(EDocID.MATCH_COMBAT.getDoc());
            }
        };

        /** Show console. */
        final Action actShowConsole = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (Singletons.getControl().getState() != 1) { return; }
                SDisplayUtil.showTab(EDocID.MATCH_LOG.getDoc());
            }
        };

        /** Show players panel. */
        final Action actShowPlayers = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (Singletons.getControl().getState() != 1) { return; }
                SDisplayUtil.showTab(EDocID.MATCH_PLAYERS.getDoc());
            }
        };

        /** Show dev panel. */
        final Action actShowDev = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (Singletons.getControl().getState() != 1) { return; }
                SDisplayUtil.showTab(EDocID.MATCH_DEV_MODE.getDoc());
            }
        };

        /** Concede game. */
        final Action actConcede = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (Singletons.getControl().getState() != 1) { return; }
                CDock.SINGLETON_INSTANCE.concede();
            }
        };

        //========== Instantiate shortcut objects and add to list.
        list.add(new Shortcut(FPref.SHORTCUT_SHOWSTACK, "Match: show stack panel", actShowStack, am, im));
        list.add(new Shortcut(FPref.SHORTCUT_SHOWCOMBAT, "Match: show combat panel", actShowCombat, am, im));
        list.add(new Shortcut(FPref.SHORTCUT_SHOWCONSOLE, "Match: show console panel", actShowConsole, am, im));
        list.add(new Shortcut(FPref.SHORTCUT_SHOWPLAYERS, "Match: show players panel", actShowPlayers, am, im));
        list.add(new Shortcut(FPref.SHORTCUT_SHOWDEV, "Match: show dev panel", actShowDev, am, im));
        list.add(new Shortcut(FPref.SHORTCUT_CONCEDE, "Match: concede game", actConcede, am, im));
        return list;
    } // End initMatchShortcuts()

    /**
     * Converts a string of key codes (space delimited) into their respective
     * key texts. This helps juggling between input maps, display text, save
     * values, and input data.
     * 
     * @param s0
     *            &emsp; A string of keycodes
     * @return String
     */
    public static String codes2Chars(final String s0) {
        final List<String> codes = new ArrayList<String>(Arrays.asList(s0.split(" ")));
        final List<String> displayText = new ArrayList<String>();
        String temp;

        for (final String s : codes) {
            temp = KeyEvent.getKeyText(Integer.valueOf(s));

            if (!s.isEmpty()) {
                // Probably a better way to do this; but I couldn't find it
                // after a decent look around. The main problem is that
                // KeyEvent.getKeyText() will return "Ctrl", but the input
                // map expects "control". Similar case problems with Shift and
                // Alt.
                // Doublestrike 21-11-11
                if (temp.equalsIgnoreCase("ctrl")) {
                    temp = "control";
                } else if (temp.equalsIgnoreCase("shift")) {
                    temp = "shift";
                } else if (temp.equalsIgnoreCase("alt")) {
                    temp = "alt";
                } else if (temp.equalsIgnoreCase("escape")) {
                    temp = "escape";
                }

                displayText.add(temp);
            }
        }

        return StringUtils.join(displayText, ' ');
    } // End codes2chars

    /**
     * 
     * Instantiates a shortcut key instance with various properties for use
     * throughout the project.
     *
     */
    public static class Shortcut {
        /** */
        private FPref prefkey;
        /** */
        private String description;
        /** */
        private Action handler;
        /** */
        private ActionMap actionMap;
        /** */
        private InputMap inputMap;
        /** */
        private KeyStroke key;
        /** */
        private String str;

        /**
         * 
         * Instantiates a shortcut key instance with various properties for use
         * throughout the project.
         * 
         * @param prefkey0 String, ident key in forge.preferences
         * @param description0 String, description of shortcut function
         * @param handler0 Action, action on execution of shortcut
         * @param am0 ActionMap, of container targeted by shortcut
         * @param im0 InputMap, of container targeted by shortcut
         */
        public Shortcut(final FPref prefkey0, final String description0,
                final Action handler0, final ActionMap am0, final InputMap im0) {

            prefkey = prefkey0;
            description = description0;
            handler = handler0;
            actionMap = am0;
            inputMap = im0;
            attach();
        }

        /** @return {@link java.lang.String} */
        public String getDescription() {
            return description;
        }

        /**
         * String ident key in forge.preferences.
         * @return {@link java.lang.String}
         */
        public FPref getPrefKey() {
            return prefkey;
        }

        /** */
        public void attach() {
            detach();
            str = Singletons.getModel().getPreferences().getPref(prefkey);
            key = KeyStroke.getKeyStroke(KeyboardShortcuts.codes2Chars(str));
            inputMap.put(key, str);
            actionMap.put(str, handler);
        }

        /** */
        public void detach() {
            inputMap.remove(key);
            actionMap.remove(str);
        }
    } // End class Shortcut

    /**
     * - Adds keycode to list stored in name of a text field. - Code is not
     * added if already in list. - Backspace removes last code in list. - Sets
     * text of text field with character equivalent of keycodes.
     * 
     * @param e
     *            &emsp; KeyEvent
     */
    public static void addKeyCode(final KeyEvent e) {
        final KeyboardShortcutField ksf = (KeyboardShortcutField) e.getSource();
        final String newCode = Integer.toString(e.getKeyCode());
        final String codestring = ksf.getCodeString();
        List<String> existingCodes;

        if (codestring != null) {
            existingCodes = new ArrayList<String>(Arrays.asList(codestring.split(" ")));
        } else {
            existingCodes = new ArrayList<String>();
        }

        // Backspace (8) will remove last code from list.
        if (e.getKeyCode() == 8) {
            existingCodes.remove(existingCodes.size() - 1);
        } else if (!existingCodes.contains(newCode)) {
            existingCodes.add(newCode);
        }

        ksf.setCodeString(StringUtils.join(existingCodes, ' '));
    }
}
