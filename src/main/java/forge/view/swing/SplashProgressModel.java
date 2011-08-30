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
     * Constructor called with no arguments, indicating 1 phase
     * and number of phase units assumed to be 1 also (can be updated later). 
     * @param numPhases
     */
    public SplashProgressModel() {
        super();        
    }
    
    @Override
    /**
     * @see net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor#incrementUnitsCompletedThisPhase(long)
     */
    public void incrementUnitsCompletedThisPhase(long numUnits) {
        super.incrementUnitsCompletedThisPhase(numUnits);
        getCurrentView().updateProgressBar();
    }
    
    /**
     * Gets view from which data is sent for display
     * 
     * @return
     */
    public SplashProgressComponent getCurrentView() {
        return currentView;
    }

    /**
     * Sets view to which data is sent for display
     * 
     * @param neoView
     */
    public void setCurrentView(SplashProgressComponent neoView) {
        currentView = neoView;
    }
    
}
