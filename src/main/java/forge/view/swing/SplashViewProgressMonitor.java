package forge.view.swing;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor;

/** 
 *  Swing component view, to be used with BaseProgressMonitor.
 *
 */
@SuppressWarnings("serial")
public class SplashViewProgressMonitor extends JProgressBar {
    private BaseProgressMonitor currentModel;
    private double completed;
    private double total;

    public SplashViewProgressMonitor() {
        super();
        setString("");
        setStringPainted(true);
    }
    
    public final void incrementProgressBar() {
        // Update bar "stripe"
        SwingUtilities.invokeLater(new Runnable() { 
            public void run() {
               stripeUpdate();
            }
        });
        
        // Update bar message
            // Note: shouldUpdateUI() severely retards motion 
            // of the preloader, so is temporarily disabled.
        //if (getCurrentModel().shouldUpdateUI()) {  
            if ((getCurrentModel().getNumPhases() > 1)) {
                displayUpdate(
                 "Phase " + getCurrentModel().getCurrentPhase() + ". "
                 //+ getUnitsCompletedSoFarThisPhase() + " units processed. "
                 //+ "Overall: " + getTotalPercentCompleteAsString() + "% complete, "
                 + "Overall ETA in " + getCurrentModel().getRelativeETAAsString() + "."
                 );
            }
            else {
                displayUpdate(
                 //"Overall: " +
                 getCurrentModel().getUnitsCompletedSoFarThisPhase() + " units processed; "
                 //+ "(" + getTotalPercentCompleteAsString() + "%); "
                 + "ETA in " + getCurrentModel().getRelativeETAAsString() + "."
                 );
            }
           // getCurrentModel().justUpdatedUI();
        //}

       
        if (getCurrentModel().getCurrentPhase() == getCurrentModel().getNumPhases()
            && getCurrentModel().getUnitsCompletedSoFarThisPhase() >= getCurrentModel().getTotalUnitsThisPhase())
        {
            displayUpdate("Done! Firing up the GUI...");
        } 
     }
    
    /**
     * Shows the message inside the progress dialog; does not always work on
     * all platforms.
     *
     * @param message  the message to display
     */
    public final void displayUpdate(final String message) {
        final Runnable proc = new Runnable() { 
            public void run() {
                setString(message);
                getCurrentModel().justUpdatedUI();
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            proc.run();
        }
        else {
            SwingUtilities.invokeLater(proc);
        }
    }
    
    /**
     * Moves the stripe inside the progress dialog.
     *
     */ 
    public final void stripeUpdate() {
        completed = getCurrentModel().getUnitsCompletedSoFarThisPhase();
        total = getCurrentModel().getTotalUnitsThisPhase();
        
        setValue((int)Math.round((int)completed/total*100));
    }
    
    /**
     * Resets the stripe inside the progress dialog back to zero.
     *
     */
    public final void stripeReset() {
        setValue(0);
    }
    
    public void setCurrentModel(BaseProgressMonitor neoModel) {
        currentModel = neoModel;
    }
    
    public BaseProgressMonitor getCurrentModel() {
        return currentModel;
    }
}
