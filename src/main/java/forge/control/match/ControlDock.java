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
package forge.control.match;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.AllZone;
import forge.Singletons;
import forge.properties.ForgePreferences;
import forge.view.match.ViewDock;
import forge.view.match.ViewDock.KeyboardShortcutField;

/**
 * Child controller, handles dock button operations.
 * 
 */
public class ControlDock {
    private final ViewDock view;

    /**
     * Child controller, handles dock button operations.
     * 
     * @param v
     *            &emsp; ViewDock obj
     */
    public ControlDock(final ViewDock v) {
        this.view = v;
    }

    /** Concede game, bring up WinLose UI. */
    public void concede() {
        AllZone.getHumanPlayer().concede();
        AllZone.getGameAction().checkStateEffects();
    }

    /**
     * Gets the view.
     * 
     * @return ViewDock
     */
    public ViewDock getView() {
        return this.view;
    }

    /** Updates and saves ForgePreferences with current shortcuts. */
    public void saveKeyboardShortcuts() {
        final ForgePreferences fp = Singletons.getModel().getPreferences();
        final Map<String, KeyboardShortcutField> shortcuts = this.view.getKeyboardShortcutFields();

        fp.setShowStackShortcut(shortcuts.get("showstack").getCodeString());
        fp.setShowCombatShortcut(shortcuts.get("showcombat").getCodeString());
        fp.setShowPlayersShortcut(shortcuts.get("showplayers").getCodeString());
        fp.setShowConsoleShortcut(shortcuts.get("showconsole").getCodeString());
        fp.setShowDevShortcut(shortcuts.get("showdev").getCodeString());
        fp.setConcedeShortcut(shortcuts.get("concede").getCodeString());
        fp.setShowPictureShortcut(shortcuts.get("showpicture").getCodeString());
        fp.setShowDetailShortcut(shortcuts.get("showdetail").getCodeString());

        try {
            fp.save();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        AllZone.getOverlay().hideOverlay();
    }

    /**
     * - Adds keycode to list stored in name of a text field. - Code is not
     * added if already in list. - Backspace removes last code in list. - Sets
     * text of text field with character equivalent of keycodes.
     * 
     * @param e
     *            &emsp; KeyEvent
     */
    public void addKeyCode(final KeyEvent e) {
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

    /**
     * End turn.
     */
    public void endTurn() {
        // Big thanks to you, Gameplay Guru, since I was too lazy to figure this
        // out
        // before release. Doublestrike 24-11-11
        System.err.println("forge.control.match > ControlDock > endTurn()");
        System.out.println("Should skip to the end of turn, or entire turn.");
        System.err.println("If some gameplay guru could implement this, that would be great...");
    }
}
