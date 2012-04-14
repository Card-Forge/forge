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

import forge.gui.match.VMatchUI;

/**
 * <p>
 * ButtonUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ButtonUtil {
    /**
     * <p>
     * reset.
     * </p>
     */
    public static void reset() {
        ButtonUtil.getOK().setText("OK");
        ButtonUtil.getCancel().setText("Cancel");

        ButtonUtil.getOK().setEnabled(false);
        ButtonUtil.getCancel().setEnabled(false);
    }

    /**
     * <p>
     * enableOnlyOK.
     * </p>
     */
    public static void enableOnlyOK() {
        ButtonUtil.getOK().setEnabled(true);
        ButtonUtil.getCancel().setEnabled(false);
    }

    /**
     * <p>
     * enableOnlyCancel.
     * </p>
     */
    public static void enableOnlyCancel() {
        ButtonUtil.getOK().setEnabled(false);
        ButtonUtil.getCancel().setEnabled(true);
    }

    /**
     * <p>
     * disableAll.
     * </p>
     */
    public static void disableAll() {
        ButtonUtil.getOK().setEnabled(false);
        ButtonUtil.getCancel().setEnabled(false);
    }

    /**
     * <p>
     * enableAll.
     * </p>
     */
    public static void enableAll() {
        ButtonUtil.getOK().setEnabled(true);
        ButtonUtil.getCancel().setEnabled(true);
    }

    /**
     * <p>
     * disableOK.
     * </p>
     */
    public static void disableOK() {
        ButtonUtil.getOK().setEnabled(false);
    }

    /**
     * <p>
     * disableCancel.
     * </p>
     */
    public static void disableCancel() {
        ButtonUtil.getCancel().setEnabled(false);
    }

    /**
     * <p>
     * getOK.
     * </p>
     * 
     * @return a {@link forge.MyButton} object.
     */
    private static JButton getOK() {
        return VMatchUI.SINGLETON_INSTANCE.getBtnOK();
    }

    /**
     * <p>
     * getCancel.
     * </p>
     * 
     * @return a {@link forge.MyButton} object.
     */
    private static JButton getCancel() {
        return VMatchUI.SINGLETON_INSTANCE.getBtnCancel();
    }
}
