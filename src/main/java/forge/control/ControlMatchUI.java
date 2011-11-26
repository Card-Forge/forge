package forge.control;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;

import forge.AllZone;
import forge.Singletons;
import forge.Constant.Zone;
import forge.control.match.ControlField;
import javax.swing.KeyStroke;

import org.apache.commons.lang3.StringUtils;

import forge.properties.ForgePreferences;
import forge.view.match.ViewTopLevel;

/**
 * <p>ControlMatchUI</p>
 * Top-level controller for matches. Implements Display.
 *
 */
public class ControlMatchUI {
    private ViewTopLevel view;

    /**
     * <p>ControlMatchUI</p>
     * Constructs instance of match UI controller, used as a single
     * point of top-level control for child UIs - in other words, this class
     * controls the controllers.  Tasks targeting the view of individual
     * components are found in a separate controller for that component
     * and should not be included here.
     * 
     * This constructor is called after child components have been instantiated.
     * When children are instantiated, they also instantiate their controller.
     * So, this class must be called after everything is already in place.
     * 
     * @param v &emsp; A ViewTopLevel object
     */
    public ControlMatchUI(ViewTopLevel v) {
        view = v;
    }

    /**
     * Fires up controllers for each component of UI.
     * 
     */
    public void initMatch() {
        // All child components have been assembled; observers and listeners can
        // be added safely.
        view.getAreaSidebar().getTabber().getController().addObservers();
        view.getAreaSidebar().getTabber().getController().addListeners();

        view.getAreaUser().getPnlInput().getController().addListeners();

        view.getAreaUser().getPnlHand().getController().addObservers();
        view.getAreaUser().getPnlHand().getController().addListeners();

        // Update all observers with values for start of match.
        List<ControlField> fieldControllers = view.getFieldControllers();
        for (ControlField f : fieldControllers) {
            f.addObservers();
            f.addListeners();
            f.getPlayer().updateObservers();
        }

        AllZone.getHumanPlayer().getZone(Zone.Hand).updateObservers();
        AllZone.getComputerPlayer().getZone(Zone.Hand).updateObservers();
        AllZone.getStack().updateObservers();
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();
        AllZone.getInputControl().updateObservers();
        view.getAreaSidebar().getTabber().getController().updateObservers();
        this.mapKeyboardShortcuts();
    }

    /**
     * Resets all phase buttons in all fields to "inactive", so highlight won't be
     * drawn on them. "Enabled" state remains the same.
     */
    // This method is in the top-level controller because it affects ALL fields (not just one).
    public void resetAllPhaseButtons() {
        List<ControlField> fieldControllers = view.getFieldControllers();

        for (ControlField c : fieldControllers) {
            c.resetPhaseButtons();
        }
    }

    /**
     * Maps actions to shortcuts, and attaches each shortcut to the
     * InputMap of the top level view.
     * 
     */
    @SuppressWarnings("serial")
    private void mapKeyboardShortcuts() {
        InputMap im = ((ViewTopLevel) AllZone.getDisplay()).getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ForgePreferences fp = Singletons.getModel().getPreferences();
        String str;
        KeyStroke key;

        // Actions which correspond to key presses
        Action actShowStack = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { view.getTabberController().showPnlStack(); }
        };

        Action actShowCombat = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { view.getTabberController().showPnlCombat(); }
        };

        Action actShowConsole = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { view.getTabberController().showPnlConsole(); }
        };

        Action actShowPlayers = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { view.getTabberController().showPnlPlayers(); }
        };

        Action actShowDev = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { view.getTabberController().showPnlDev(); }
        };

        Action actConcede = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { view.getDockController().concede(); }
        };

        Action actShowPicture = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { view.getCardviewerController().showPnlCardPic(); }
        };

        Action actShowDetail = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { view.getCardviewerController().showPnlCardDetail(); }
        };

        // Show stack
        // (Get keycode string, convert to char, convert to keystroke, put on input map.)
        str = fp.getShowStackShortcut();
        key = KeyStroke.getKeyStroke(codes2Chars(str));

        im.put(key, str);
        ((ViewTopLevel) AllZone.getDisplay()).getActionMap().put(im.get(key), actShowStack);

        // Show combat
        str = fp.getShowCombatShortcut();
        key = KeyStroke.getKeyStroke(codes2Chars(str));

        im.put(key, str);
        ((ViewTopLevel) AllZone.getDisplay()).getActionMap().put(im.get(key), actShowCombat);

        // Show console
        str = fp.getShowConsoleShortcut();
        key = KeyStroke.getKeyStroke(codes2Chars(str));

        im.put(key, str);
        ((ViewTopLevel) AllZone.getDisplay()).getActionMap().put(im.get(key), actShowConsole);

        // Show players
        str = fp.getShowPlayersShortcut();
        key = KeyStroke.getKeyStroke(codes2Chars(str));

        im.put(key, str);
        ((ViewTopLevel) AllZone.getDisplay()).getActionMap().put(im.get(key), actShowPlayers);

        // Show devmode
        str = fp.getShowDevShortcut();
        key = KeyStroke.getKeyStroke(codes2Chars(str));

        im.put(key, str);
        ((ViewTopLevel) AllZone.getDisplay()).getActionMap().put(im.get(key), actShowDev);

        // Concede game
        str = fp.getConcedeShortcut();
        key = KeyStroke.getKeyStroke(codes2Chars(str));

        im.put(key, str);
        ((ViewTopLevel) AllZone.getDisplay()).getActionMap().put(im.get(key), actConcede);

        // Show card picture
        str = fp.getShowPictureShortcut();
        key = KeyStroke.getKeyStroke(codes2Chars(str));

        im.put(key, str);
        ((ViewTopLevel) AllZone.getDisplay()).getActionMap().put(im.get(key), actShowPicture);

        // Show card detail
        str = fp.getShowDetailShortcut();
        key = KeyStroke.getKeyStroke(codes2Chars(str));

        im.put(key, str);
        ((ViewTopLevel) AllZone.getDisplay()).getActionMap().put(im.get(key), actShowDetail);
    }

    /**
     * Converts a string of key codes (space delimited) into their respective
     * key texts.  This helps juggling between input maps, display text, save
     * values, and input data.
     * 
     * @param s0 &emsp; A string of keycodes
     * @return String
     */
    private String codes2Chars(String s0) {
        List<String> codes = new ArrayList<String>(Arrays.asList(s0.split(" ")));
        List<String> displayText = new ArrayList<String>();
        String temp;

        for (String s : codes) {
            temp = KeyEvent.getKeyText(Integer.valueOf(s));

            if (!s.isEmpty()) {
                // Probably a better way to do this; but I couldn't find it
                // after a decent look around. The main problem is that
                // KeyEvent.getKeyText() will return "Ctrl", but the input
                // map expects "control". Similar case problems with Shift and Alt.
                // Doublestrike 21-11-11
                if (temp.equalsIgnoreCase("ctrl")) {
                    temp = "control";
                }
                else if (temp.equalsIgnoreCase("shift")) {
                    temp = "shift";
                }
                else if (temp.equalsIgnoreCase("alt")) {
                    temp = "alt";
                }
                else if (temp.equalsIgnoreCase("escape")) {
                    temp = "escape";
                }

                displayText.add(temp);
            }
        }

        return StringUtils.join(displayText, ' ');
    }
}
