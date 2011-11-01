package forge.view.swing;

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
