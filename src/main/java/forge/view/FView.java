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

import net.slightlymagic.braids.util.progress_monitor.BraidsProgressMonitor;
import forge.model.FModel;

/**
 * Generic view (as in model-view-controller) interface for Forge.
 */
public interface FView {

    /**
     * Tell the view that the model has been bootstrapped, and its data is ready
     * for initial display.
     * 
     * @param model
     *            the model that has finished bootstrapping
     */
    void setModel(FModel model);

    /**
     * Get the progress monitor for loading all cards at once.
     * 
     * @return a progress monitor having only one phase; may be null
     */
    BraidsProgressMonitor getCardLoadingProgressMonitor();
}
