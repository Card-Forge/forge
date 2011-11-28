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
package forge.view.home;

import net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor;

/**
 * Creates an instance of BaseProgressMonitor that is used in the splash frame.
 * 
 * Not all mutators notify the view yet.
 * 
 */
public class SplashProgressModel extends BaseProgressMonitor {

    private SplashProgressComponent currentView = null;

    /**
     * Constructor called with no arguments, indicating 1 phase and number of
     * phase units assumed to be 1 also (can be updated later).
     * 
     */
    public SplashProgressModel() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor#
     * incrementUnitsCompletedThisPhase(long)
     */

    /**
     * Increment units completed this phase.
     * 
     * @param numUnits
     *            long
     * @see net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor#incrementUnitsCompletedThisPhase(long)
     */
    @Override
    public final void incrementUnitsCompletedThisPhase(final long numUnits) {
        super.incrementUnitsCompletedThisPhase(numUnits);
        this.getCurrentView().updateProgressBar();
    }

    /**
     * Gets view from which data is sent for display.
     * 
     * @return the current view
     */
    public final SplashProgressComponent getCurrentView() {
        return this.currentView;
    }

    /**
     * Sets view to which data is sent for display.
     * 
     * @param neoView
     *            the new current view
     */
    public final void setCurrentView(final SplashProgressComponent neoView) {
        this.currentView = neoView;
    }

}
