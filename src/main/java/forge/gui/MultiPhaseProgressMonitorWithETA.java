package forge.gui;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import forge.Gui_ProgressBarWindow;
import net.slightlymagic.braids.util.UtilFunctions;
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
	 * Like all swing components, this constructor must be invoked from the 
	 * swing Event Dispatching Thread. The rest of the methods of this class
	 * are exempt from this requirement.
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

        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("must be called from within an event dispatch thread");
        }

		this.title = title;

		if (totalUnitsFirstPhase > 0 && dialog == null) {
		    throw new IllegalStateException("dialog is null");
		}
	}


	/**
	 * For developer testing.
	 *
	 * @param args  ignored
	 */
    public static void main(final String[] args) {

    	System.out.println("Initializing...");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

            	final int totalUnitsFirstPhase = 5000;
                final MultiPhaseProgressMonitorWithETA monitor =
            	        new MultiPhaseProgressMonitorWithETA("Testing 2 phases", 2, totalUnitsFirstPhase, 1.0f,
            	                new float[] {2, 1});

                SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
                    @Override
                    public Object doInBackground() {

                        System.out.println("Running...");

                        for (int i = 0; i <= totalUnitsFirstPhase; i++) {
                            monitor.incrementUnitsCompletedThisPhase(1);

                            System.out.print("\ri = " + i);

                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException ignored) {
                                // blank
                            }
                        }
                        System.out.println();

                        final int totalUnitsSecondPhase = 2000;
                        monitor.markCurrentPhaseAsComplete(totalUnitsSecondPhase);

                        for (int i = 0; i <= totalUnitsSecondPhase; i++) {
                            monitor.incrementUnitsCompletedThisPhase(1);

                            System.out.print("\ri = " + i);

                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException ignored) {
                                // blank
                            }
                        }

                        monitor.markCurrentPhaseAsComplete(0);

                        System.out.println();
                        System.out.println("Done!");

                        return null;
                    }

                };

                worker.execute();
            }
        });
    }



	@Override
	/**
	 * @param numUnits cannot be higher than Integer.MAX_VALUE
	 * 
	 * @see net.slightlymagic.braids.util.progress_monitor.ProgressMonitor#setTotalUnitsThisPhase(long)
	 */
	public void setTotalUnitsThisPhase(final long numUnits) {
		super.setTotalUnitsThisPhase(numUnits);
		
		if (numUnits > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("numUnits must be <= " + Integer.MAX_VALUE);
		}

        if (numUnits > 0) {
            // dialog must exist before we exit this method.
            UtilFunctions.invokeInEventDispatchThreadAndWait(new Runnable() {
                public void run() {
                    // (Re)create the progress bar.
                    if (dialog != null) {
                        dialog.dispose();
                        dialog = null;
                    }

                    dialog = new Gui_ProgressBarWindow();
                }
            });
        }

		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
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
		});

	}

    @Override
	/**
	 * @see net.slightlymagic.braids.util.progress_monitor.ProgressMonitor#incrementUnitsCompletedThisPhase(long)
	 */
    public void incrementUnitsCompletedThisPhase(final long numUnits) {
		super.incrementUnitsCompletedThisPhase(numUnits);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (int i = 0 ; i < numUnits ; i++) {
                    dialog.increment();
                }
            }
        });

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
    public void displayUpdate(final String message) {

        final Runnable proc = new Runnable() {
            public void run() {
            	// i've been having trouble getting the dialog to display its title.
        		dialog.setTitle(title);

            	JProgressBar bar = dialog.getProgressBar();
        		bar.setString(message);

                justUpdatedUI();
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            proc.run();
        }
        else {
            SwingUtilities.invokeLater(proc);
        }
    }


    @Override
    public final void dispose() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                getDialog().dispose();
            }
        });
    }


    /**
     * @return the JDialog for the current phase; use this judiciously to
     * manipulate the dialog directly.
     */
    public JDialog getDialog() {
    	return dialog;
    }
    
}
