package forge.gui;

import javax.swing.JDialog;
import javax.swing.JProgressBar;

import forge.Gui_ProgressBarWindow;
import net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor;

/**
 * GUI Progress Monitor that displays the ETA (Estimated Time of Arrival or
 * completion) on some platforms and supports one or multiple phases of 
 * progress.
 * 
 * In this implementation, each phase opens a new dialog.
 */
public class MultiPhaseProgressMonitorWithETA extends BaseProgressMonitor {

	private Gui_ProgressBarWindow dialog;
	private String title;

	/**
	 * Convenience for 
	 * MultiPhaseProgressMonitorWithETA(title, numPhases, totalUnitsFirstPhase,
	 * minUIUpdateIntervalSec, null);
	 * 
	 * @see #MultiPhaseProgressMonitorWithETA(String,int,long,float,float[])
	 */
	public MultiPhaseProgressMonitorWithETA(String title, int numPhases,
			long totalUnitsFirstPhase, float minUIUpdateIntervalSec) 
	{
		this(title, numPhases, totalUnitsFirstPhase, minUIUpdateIntervalSec, null);
	}
	/**
	 * Create a GUI progress monitor and open its first dialog.
	 * 
	 * @param title  the title to give the dialog box(es)
	 * 
	 * @param numPhases  the total number of phases to expect
	 * 
	 * @param totalUnitsFirstPhase  the total number of units that will be
	 *  processed in the first phase
	 *  
	 * @param minUIUpdateIntervalSec  the approximate interval at which to 
	 * update the dialog box in seconds
	 * 
	 * @param phaseWeights  see BaseProgressMonitor
	 * 
	 * @see BaseProgressMonitor#BaseProgressMonitor(int,long,float,float[])
	 */
	public MultiPhaseProgressMonitorWithETA(String title, int numPhases,
			long totalUnitsFirstPhase, float minUIUpdateIntervalSec,
			float[] phaseWeights) 
	{
		super(numPhases, totalUnitsFirstPhase, minUIUpdateIntervalSec,
				phaseWeights);
		
		this.title = title;
	}

	
	/** 
	 * For developer testing.
	 */
    public static void main(String[] args) {

    	System.out.println("Initializing...");

    	MultiPhaseProgressMonitorWithETA monitor = 
    		new MultiPhaseProgressMonitorWithETA("Testing 2 phases", 2, 5000, 
    				1.0f, null);
    	
    	System.out.println("Running...");

    	for (int i = 0; i <= 5000; i++) {
    		monitor.incrementUnitsCompletedThisPhase(1);
    		
    		System.out.print("\ri = " + i);
    		
    		try {
				Thread.sleep(1);
			} catch (InterruptedException ignored) {
				;
			}
    	}
    	System.out.println();
    	
    	monitor.markCurrentPhaseAsComplete(2000);

    	for (int i = 0; i <= 2000; i++) {
    		monitor.incrementUnitsCompletedThisPhase(1);
    		
    		System.out.print("\ri = " + i);
    		
    		try {
				Thread.sleep(1);
			} catch (InterruptedException ignored) {
				;
			}
    	}
    	
    	monitor.markCurrentPhaseAsComplete(0);

    	System.out.println();
   
    	System.out.println("Done!");
    }
	

	
	@Override
	/**
	 * @param numUnits cannot be higher than Integer.MAX_VALUE
	 * 
	 * @see net.slightlymagic.braids.util.progress_monitor.ProgressMonitor#setTotalUnitsThisPhase(long)
	 */
	public void setTotalUnitsThisPhase(long numUnits) {
		super.setTotalUnitsThisPhase(numUnits);
		
		if (numUnits > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("numUnits must be <= " + Integer.MAX_VALUE);
		}

		if (numUnits > 0) {
			// (Re)create the progress bar.
			if (dialog != null) {
				dialog.dispose();
				dialog = null;
			}
			
			dialog = new Gui_ProgressBarWindow();
			dialog.setTitle(title);
	        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	        dialog.setVisible(true);
	        dialog.setResizable(true);        
	        dialog.getProgressBar().setIndeterminate(false);
	        dialog.setProgressRange(0, (int) numUnits);
	        dialog.reset();

			JProgressBar bar = dialog.getProgressBar();
			bar.setString("");
			bar.setStringPainted(true);
			bar.setValue(0);	
		}
	}
	
    @Override
	/**
	 * @see net.slightlymagic.braids.util.progress_monitor.ProgressMonitor#incrementUnitsCompletedThisPhase(long)
	 */
    public void incrementUnitsCompletedThisPhase(long numUnits) {
		super.incrementUnitsCompletedThisPhase(numUnits);
        
        for (int i = 0 ; i < numUnits ; i++) {
        	dialog.increment();
        }

        if (shouldUpdateUI()) {

        	if ((getNumPhases() > 1)) {
                displayUpdate(
                 "Phase " + getCurrentPhase() + ". " +
                 //getUnitsCompletedSoFarThisPhase() + " units processed. " + 
                 //"Overall: " + getTotalPercentCompleteAsString() + "% complete, " +
                 "Overall ETA in " + getRelativeETAAsString() + "."
                 );
            }
            else {
                displayUpdate(
                 //"Overall: " + 
                 getUnitsCompletedSoFarThisPhase() + " units processed; " +
                 //"(" + getTotalPercentCompleteAsString() + "%); " +
                 "ETA in " + getRelativeETAAsString() + "."
                 );
            }
        }
        
        if (getCurrentPhase() == getNumPhases() && 
        	getUnitsCompletedSoFarThisPhase() >= getTotalUnitsThisPhase())
        {
        	displayUpdate("Done!");
        }
     }

    /**
     * Shows the message inside the progress dialog; does not always work on
     * all platforms.
     * 
     * @param message  the message to display
     */
    public void displayUpdate(String message) {

    	// i've been having trouble getting the dialog to display its title.
		dialog.setTitle(title);

    	JProgressBar bar = dialog.getProgressBar();
		bar.setString(message);
    	
        justUpdatedUI();
    }

    
    @Override
    public void dispose() {
    	getDialog().dispose();
    }
    

    /**
     * @return the JDialog for the current phase; use this judiciously to
     * manipulate the dialog directly.
     */
    public JDialog getDialog() {
    	return dialog;
    }
    
}
