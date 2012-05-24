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
package forge.gui.home;

import forge.gui.framework.DragCell;
import forge.gui.framework.IVTopLevelUI;
import forge.gui.framework.SLayoutIO;
import forge.view.FView;

/**
 * Top level view class for home UI drag layout.<br>
 * Uses singleton pattern.<br>
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */

public enum VHomeUI implements IVTopLevelUI {
    /** */
    SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.gui.framework.IVTopLevelUI#instantiate()
     */
    @Override
    public void instantiate() {

    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVTopLevelUI#populate()
     */
    @Override
    public void populate() {
        SLayoutIO.loadLayout(null);

        // Adjust cells to lock in the home view
        for (final DragCell d : FView.SINGLETON_INSTANCE.getDragCells()) {
            d.hideHead();
        }

        CMainMenu.SINGLETON_INSTANCE.selectPrevious();
    }
}
