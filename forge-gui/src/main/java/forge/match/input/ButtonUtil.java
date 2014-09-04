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
package forge.match.input;

import forge.interfaces.IButton;
import forge.interfaces.IGuiBase;

/**
 * Manages match UI OK/Cancel button enabling and focus
 */
public class ButtonUtil {
    public static void update(final IGuiBase gui, boolean okEnabled, boolean cancelEnabled, boolean focusOk) {
        update(gui, "OK", "Cancel", okEnabled, cancelEnabled, focusOk);
    }
    public static void update(final IGuiBase gui, String okLabel, String cancelLabel, boolean okEnabled, boolean cancelEnabled, boolean focusOk) {
        IButton btnOk = gui.getBtnOK();
        IButton btnCancel = gui.getBtnCancel();

        btnOk.setText(okLabel);
        btnCancel.setText(cancelLabel);
        btnOk.setEnabled(okEnabled);
        btnCancel.setEnabled(cancelEnabled);
        if (okEnabled && focusOk) {
            gui.focusButton(btnOk);
        }
        else if (cancelEnabled) {
            gui.focusButton(btnCancel);
        }
    }
}
