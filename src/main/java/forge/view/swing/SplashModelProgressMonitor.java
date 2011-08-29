package forge.view.swing;

import net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class SplashModelProgressMonitor extends BaseProgressMonitor {

    private SplashViewProgressMonitor currentView = null;
    
    /**
     * TODO: Write javadoc for Constructor.
     * @param numPhases
     */
    public SplashModelProgressMonitor(int numPhases) {
        super(numPhases);        
    }
    
    @Override
    /**
     * @see net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor#incrementUnitsCompletedThisPhase(long)
     */
    public void incrementUnitsCompletedThisPhase(long numUnits) {
        super.incrementUnitsCompletedThisPhase(numUnits);
        getCurrentView().incrementProgressBar();
    }
    
    public void setCurrentView(SplashViewProgressMonitor neoView) {
        currentView = neoView;
    }
    
    public SplashViewProgressMonitor getCurrentView() {
        return currentView;
    }

}
