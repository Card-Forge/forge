/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
import javax.swing.KeyStroke;

import org.apache.commons.lang3.StringUtils;

import forge.AllZone;
import forge.Constant.Zone;
import forge.Singletons;
import forge.control.match.ControlField;
import forge.properties.ForgePreferences;
import forge.view.GuiTopLevel;
import forge.view.match.ViewTopLevel;

/**
 * <p>
 * ControlMatchUI
 * </p>
 * Top-level controller for matches. Implements Display.
 * 
 */
public class ControlMatchUI {
    private final ViewTopLevel view;

    /**
     * <p>
     * ControlMatchUI
     * </p>
     * Constructs instance of match UI controller, used as a single point of
     * top-level control for child UIs - in other words, this class controls the
     * controllers. Tasks targeting the view of individual components are found
     * in a separate controller for that component and should not be included
     * here.
     * 
     * This constructor is called after child components have been instantiated.
     * When children are instantiated, they also instantiate their controller.
     * So, this class must be called after everything is already in place.
     * 
     * @param v
     *            &emsp; A ViewTopLevel object
     */
    public ControlMatchUI(final ViewTopLevel v) {
        this.view = v;
    }

    /**
     * Fires up controllers for each component of UI.
     * 
     */
    public void initMatch() {
        // All child components have been assembled; observers and listeners can
        // be added safely.
        this.view.getTabberController().addObservers();
        this.view.getTabberController().addListeners();

        this.view.getInputController().addListeners();

        this.view.getHandController().addObservers();
        this.view.getHandController().addListeners();

        // Update all observers with values for start of match.
        final List<ControlField> fieldControllers = this.view.getFieldControllers();
        for (final ControlField f : fieldControllers) {
            f.addObservers();
            f.addListeners();
            f.getPlayer().updateObservers();
        }

        AllZone.getHumanPlayer().getZone(Zone.Hand).updateObservers();
        AllZone.getComputerPlayer().getZone(Zone.Hand).updateObservers();
        AllZone.getStack().updateObservers();
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();
        AllZone.getInputControl().updateObservers();
        this.view.getTabberController().updateObservers();
        this.mapKeyboardShortcuts();
    }

    /**
     * Resets all phase buttons in all fields to "inactive", so highlight won't
     * be drawn on them. "Enabled" state remains the same.
     */
    // This method is in the top-level controller because it affects ALL fields
    // (not just one).
    public void resetAllPhaseButtons() {
        final List<ControlField> fieldControllers = this.view.getFieldControllers();

        for (final ControlField c : fieldControllers) {
            c.resetPhaseButtons();
        }
    }

    /**
     * Maps actions to shortcuts, and attaches each shortcut to the InputMap of
     * the top level view.
     * 
     */
    @SuppressWarnings("serial")
    private void mapKeyboardShortcuts() {
        final InputMap im = ((GuiTopLevel) AllZone.getDisplay()).getController().getMatchController().getView()
                .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        final ForgePreferences fp = Singletons.getModel().getPreferences();
        String str;
        KeyStroke key;

        // Actions which correspond to key presses
        final Action actShowStack = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                ControlMatchUI.this.view.getTabberController().showPnlStack();
            }
        };

        final Action actShowCombat = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                ControlMatchUI.this.view.getTabberController().showPnlCombat();
            }
        };

        final Action actShowConsole = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                ControlMatchUI.this.view.getTabberController().showPnlConsole();
            }
        };

        final Action actShowPlayers = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                ControlMatchUI.this.view.getTabberController().showPnlPlayers();
            }
        };

        final Action actShowDev = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                ControlMatchUI.this.view.getTabberController().showPnlDev();
            }
        };

        final Action actConcede = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                ControlMatchUI.this.view.getDockController().concede();
            }
        };

        ViewTopLevel t = ((GuiTopLevel) AllZone.getDisplay()).getController().getMatchController().getView();

        // Show stack
        // (Get keycode string, convert to char, convert to keystroke, put on
        // input map.)
        str = fp.getShowStackShortcut();
        key = KeyStroke.getKeyStroke(this.codes2Chars(str));

        im.put(key, str);
        t.getActionMap().put(im.get(key), actShowStack);

        // Show combat
        str = fp.getShowCombatShortcut();
        key = KeyStroke.getKeyStroke(this.codes2Chars(str));

        im.put(key, str);
        t.getActionMap().put(im.get(key), actShowCombat);

        // Show console
        str = fp.getShowConsoleShortcut();
        key = KeyStroke.getKeyStroke(this.codes2Chars(str));

        im.put(key, str);
        t.getActionMap().put(im.get(key), actShowConsole);

        // Show players
        str = fp.getShowPlayersShortcut();
        key = KeyStroke.getKeyStroke(this.codes2Chars(str));

        im.put(key, str);
        t.getActionMap().put(im.get(key), actShowPlayers);

        // Show devmode
        str = fp.getShowDevShortcut();
        key = KeyStroke.getKeyStroke(this.codes2Chars(str));

        im.put(key, str);
        t.getActionMap().put(im.get(key), actShowDev);

        // Concede game
        str = fp.getConcedeShortcut();
        key = KeyStroke.getKeyStroke(this.codes2Chars(str));

        im.put(key, str);
        t.getActionMap().put(im.get(key), actConcede);
    }

    /**
     * Converts a string of key codes (space delimited) into their respective
     * key texts. This helps juggling between input maps, display text, save
     * values, and input data.
     * 
     * @param s0
     *            &emsp; A string of keycodes
     * @return String
     */
    private String codes2Chars(final String s0) {
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
    }

    /** @return ViewTopLevel */
    public ViewTopLevel getView() {
        return view;
    }
}
