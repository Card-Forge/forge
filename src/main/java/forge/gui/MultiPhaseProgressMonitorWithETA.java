package forge.gui;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.slightlymagic.braids.util.UtilFunctions;
import net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor;
import forge.Gui_ProgressBarWindow;

/**
 * GUI Progress Monitor that displays the ETA (Estimated Time of Arrival or
 * completion) on some platforms and supports one or multiple phases of
 * progress.
 *
 * In this implementation, each phase opens a new dialog.
 */
public class MultiPhaseProgressMonitorWithETA extends BaseProgressMonitor {

    private transient Gui_ProgressBarWindow dialog;
    private transient String title;

    /**
     * Convenience for
     * MultiPhaseProgressMonitorWithETA(title, numPhases, totalUnitsFirstPhase,
     * minUIUpdateIntervalSec, null).
     *
     * @see #MultiPhaseProgressMonitorWithETA(String,int,long,float,float[])
     *
     * @param neoTitle  the title to give the dialog box(es)
     *
     * @param numPhases  the total number of phases to expect
     *
     * @param totalUnitsFirstPhase  the total number of units that will be
     *  processed in the first phase
     *
     * @param minUIUpdateIntervalSec  the approximate interval at which to
     * update the dialog box in seconds
     */
    public MultiPhaseProgressMonitorWithETA(final String neoTitle, final int numPhases,
            final long totalUnitsFirstPhase, final float minUIUpdateIntervalSec) // NOPMD by Braids on 8/18/11 11:16 PM
    {
        this(neoTitle, numPhases, totalUnitsFirstPhase, minUIUpdateIntervalSec, null);
    }
    /**
     * Create a GUI progress monitor and open its first dialog.
     *
     * Like all swing components, this constructor must be invoked from the
     * swing Event Dispatching Thread. The rest of the methods of this class
     * are exempt from this requirement.
     *
     * @param neoTitle  the title to give the dialog box(es)
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
    public MultiPhaseProgressMonitorWithETA(final String neoTitle, final int numPhases,
            final long totalUnitsFirstPhase, final float minUIUpdateIntervalSec, // NOPMD by Braids on 8/18/11 11:16 PM
            final float[] phaseWeights)
    {
        super(numPhases, totalUnitsFirstPhase, minUIUpdateIntervalSec,
                phaseWeights);

        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("must be called from within an event dispatch thread");
        }

        this.title = neoTitle;

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

        System.out.println("Initializing..."); // NOPMD by Braids on 8/18/11 11:13 PM

        SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on 8/18/11 11:16 PM
            public void run() {

                final int totalUnitsFirstPhase = 5000; // NOPMD by Braids on 8/18/11 11:16 PM
                final MultiPhaseProgressMonitorWithETA monitor =
                        new MultiPhaseProgressMonitorWithETA("Testing 2 phases", 2, totalUnitsFirstPhase, 1.0f,
                                new float[] {2, 1});

                final SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
                    @Override
                    public Object doInBackground() {

                        System.out.println("Running..."); // NOPMD by Braids on 8/18/11 11:14 PM

                        for (int i = 0; i <= totalUnitsFirstPhase; i++) {
                            monitor.incrementUnitsCompletedThisPhase(1);

                            System.out.print("\ri = " + i); // NOPMD by Braids on 8/18/11 11:14 PM

                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException ignored) {
                                // blank
                            }
                        }
                        System.out.println(); // NOPMD by Braids on 8/18/11 11:14 PM

                        final int totalUnitsSecondPhase = 2000; // NOPMD by Braids on 8/18/11 11:17 PM
                        monitor.markCurrentPhaseAsComplete(totalUnitsSecondPhase);

                        for (int i = 0; i <= totalUnitsSecondPhase; i++) {
                            monitor.incrementUnitsCompletedThisPhase(1);

                            System.out.print("\ri = " + i); // NOPMD by Braids on 8/18/11 11:14 PM

                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException ignored) {
                                // blank
                            }
                        }

                        monitor.markCurrentPhaseAsComplete(0);

                        System.out.println(); // NOPMD by Braids on 8/18/11 11:14 PM
                        System.out.println("Done!"); // NOPMD by Braids on 8/18/11 11:14 PM

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
    public final void setTotalUnitsThisPhase(final long numUnits) {
        super.setTotalUnitsThisPhase(numUnits);

        if (numUnits > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("numUnits must be <= " + Integer.MAX_VALUE);
        }

        if (numUnits > 0) {
            // dialog must exist before we exit this method.
            UtilFunctions.invokeInEventDispatchThreadAndWait(new Runnable() { // NOPMD by Braids on 8/18/11 11:17 PM
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

        SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on 8/18/11 11:18 PM
            public void run() {
                dialog.setTitle(title);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setVisible(true);
                dialog.setResizable(true);
                dialog.getProgressBar().setIndeterminate(false);
                dialog.setProgressRange(0, (int) numUnits);
                dialog.reset();

                final JProgressBar bar = dialog.getProgressBar();
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
    public final void incrementUnitsCompletedThisPhase(final long numUnits) {
        super.incrementUnitsCompletedThisPhase(numUnits);

        SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on 8/18/11 11:18 PM
            public void run() {
                for (int i = 0; i < numUnits; i++) {
                    dialog.increment();
                }
            }
        });

        if (shouldUpdateUI()) {

            if ((getNumPhases() > 1)) {
                displayUpdate(
                 "Phase " + getCurrentPhase() + ". "
                 //+ getUnitsCompletedSoFarThisPhase() + " units processed. "
                 //+ "Overall: " + getTotalPercentCompleteAsString() + "% complete, "
                 + "Overall ETA in " + getRelativeETAAsString() + "."
                 );
            }
            else {
                displayUpdate(
                 //"Overall: " +
                 getUnitsCompletedSoFarThisPhase() + " units processed; "
                 //+ "(" + getTotalPercentCompleteAsString() + "%); "
                 + "ETA in " + getRelativeETAAsString() + "."
                 );
            }
        }

        if (getCurrentPhase() == getNumPhases()
            && getUnitsCompletedSoFarThisPhase() >= getTotalUnitsThisPhase())
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
    public final void displayUpdate(final String message) {

        final Runnable proc = new Runnable() { // NOPMD by Braids on 8/18/11 11:18 PM
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
        SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on 8/18/11 11:18 PM
            public void run() {
                getDialog().dispose();
            }
        });
    }


    /**
     * @return the JDialog for the current phase; use this judiciously to
     * manipulate the dialog directly.
     */
    public final JDialog getDialog() {
        return dialog;
    }

}
