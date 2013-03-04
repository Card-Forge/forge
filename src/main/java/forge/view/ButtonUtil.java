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
package forge.view;

import javax.swing.JButton;

import forge.gui.SOverlayUtils;
import forge.gui.match.VMatchUI;

/**
 * Manages match UI OK/Cancel button enabling and focus
 */
public class ButtonUtil {
    public static void setButtonText(String okLabel, String cancelLabel) {
        getOk().setText(okLabel);
        getCancel().setText(cancelLabel);
    }
    
    public static void reset() {
        disableAll();
        getOk().setText("OK");
        getCancel().setText("Cancel");
    }

    public static void enableOnlyOk() {
        getCancel().setEnabled(false);
        enableAndFocus(getOk());
    }

    public static void enableOnlyCancel() {
        getOk().setEnabled(false);
        enableAndFocus(getCancel());
    }

    public static void disableAll() {
        getOk().setEnabled(false);
        getCancel().setEnabled(false);
    }
    
    public static void enableAllFocusOk() {
        enableAndFocus(getOk());
        getCancel().setEnabled(true);
    }

    private static void enableAndFocus(final JButton button) {
        button.setEnabled(true);
        
        // ensure we don't steal focus from an overlay
        if (!SOverlayUtils.overlayHasFocus()) {
            button.requestFocusInWindow();
        }
    }
    
    private static JButton getOk() {
        return VMatchUI.SINGLETON_INSTANCE.getBtnOK();
    }

    private static JButton getCancel() {
        return VMatchUI.SINGLETON_INSTANCE.getBtnCancel();
    }
}
