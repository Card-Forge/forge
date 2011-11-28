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
package forge.gui;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import net.slightlymagic.braids.util.UtilFunctions;
import net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor;
import forge.GuiProgressBarWindow;

/**
 * GUI Progress Monitor that displays the ETA (Estimated Time of Arrival or
 * completion) on some platforms and supports one or multiple phases of
 * progress.
 * 
 * In this implementation, each phase opens a new dialog.
 */
public class MultiPhaseProgressMonitorWithETA extends BaseProgressMonitor {

    private transient GuiProgressBarWindow dialog;
    private transient String title;

    /**
     * Convenience for MultiPhaseProgressMonitorWithETA(title, numPhases,
     * totalUnitsFirstPhase, minUIUpdateIntervalSec, null).
     * 
     * @param neoTitle
     *            the title to give the dialog box(es)
     * @param numPhases
     *            the total number of phases to expect
     * @param totalUnitsFirstPhase
     *            the total number of units that will be processed in the first
     *            phase
     * @param minUIUpdateIntervalSec
     *            the approximate interval at which to update the dialog box in
     *            seconds
     * @see #MultiPhaseProgressMonitorWithETA(String,int,long,float,float[])
     */
    public MultiPhaseProgressMonitorWithETA(final String neoTitle, final int numPhases,
            final long totalUnitsFirstPhase, final float minUIUpdateIntervalSec) // NOPMD
                                                                                 // by
                                                                                 // Braids
                                                                                 // on
                                                                                 // 8/18/11
                                                                                 // 11:16
                                                                                 // PM
    {
        this(neoTitle, numPhases, totalUnitsFirstPhase, minUIUpdateIntervalSec, null);
    }

    /**
     * Create a GUI progress monitor and open its first dialog.
     * 
     * Like all swing components, this constructor must be invoked from the
     * swing Event Dispatching Thread. The rest of the methods of this class are
     * exempt from this requirement.
     * 
     * @param neoTitle
     *            the title to give the dialog box(es)
     * 
     * @param numPhases
     *            the total number of phases to expect
     * 
     * @param totalUnitsFirstPhase
     *            the total number of units that will be processed in the first
     *            phase
     * 
     * @param minUIUpdateIntervalSec
     *            the approximate interval at which to update the dialog box in
     *            seconds
     * 
     * @param phaseWeights
     *            see BaseProgressMonitor
     * 
     * @see BaseProgressMonitor#BaseProgressMonitor(int,long,float,float[])
     */
    public MultiPhaseProgressMonitorWithETA(final String neoTitle, final int numPhases,
            final long totalUnitsFirstPhase, final float minUIUpdateIntervalSec, // NOPMD
                                                                                 // by
                                                                                 // Braids
                                                                                 // on
                                                                                 // 8/18/11
                                                                                 // 11:16
                                                                                 // PM
            final float[] phaseWeights) {
        super(numPhases, totalUnitsFirstPhase, minUIUpdateIntervalSec, phaseWeights);

        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("must be called from within an event dispatch thread");
        }

        this.title = neoTitle;

        if ((totalUnitsFirstPhase > 0) && (this.dialog == null)) {
            throw new IllegalStateException("dialog is null");
        }
    }

    /**
     * For developer testing.
     * 
     * @param args
     *            ignored
     */
    public static void main(final String[] args) {

        System.out.println("Initializing..."); // NOPMD by Braids on 8/18/11
                                               // 11:13 PM

        SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on
                                                    // 8/18/11 11:16 PM
                    @Override
                    public void run() {

                        final int totalUnitsFirstPhase = 5000; // NOPMD by
                                                               // Braids on
                                                               // 8/18/11 11:16
                                                               // PM
                        final MultiPhaseProgressMonitorWithETA monitor = new MultiPhaseProgressMonitorWithETA(
                                "Testing 2 phases", 2, totalUnitsFirstPhase, 1.0f, new float[] { 2, 1 });

                        final SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
                            @Override
                            public Object doInBackground() {

                                System.out.println("Running..."); // NOPMD by
                                                                  // Braids on
                                                                  // 8/18/11
                                                                  // 11:14 PM

                                for (int i = 0; i <= totalUnitsFirstPhase; i++) {
                                    monitor.incrementUnitsCompletedThisPhase(1);

                                    System.out.print("\ri = " + i); // NOPMD by
                                                                    // Braids on
                                                                    // 8/18/11
                                                                    // 11:14 PM

                                    try {
                                        Thread.sleep(1);
                                    } catch (final InterruptedException ignored) {
                                        // blank
                                    }
                                }
                                System.out.println(); // NOPMD by Braids on
                                                      // 8/18/11 11:14 PM

                                final int totalUnitsSecondPhase = 2000; // NOPMD
                                                                        // by
                                                                        // Braids
                                                                        // on
                                                                        // 8/18/11
                                                                        // 11:17
                                                                        // PM
                                monitor.markCurrentPhaseAsComplete(totalUnitsSecondPhase);

                                for (int i = 0; i <= totalUnitsSecondPhase; i++) {
                                    monitor.incrementUnitsCompletedThisPhase(1);

                                    System.out.print("\ri = " + i); // NOPMD by
                                                                    // Braids on
                                                                    // 8/18/11
                                                                    // 11:14 PM

                                    try {
                                        Thread.sleep(1);
                                    } catch (final InterruptedException ignored) {
                                        // blank
                                    }
                                }

                                monitor.markCurrentPhaseAsComplete(0);

                                System.out.println(); // NOPMD by Braids on
                                                      // 8/18/11 11:14 PM
                                System.out.println("Done!"); // NOPMD by Braids
                                                             // on 8/18/11 11:14
                                                             // PM

                                return null;
                            }

                        };

                        worker.execute();
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor#
     * setTotalUnitsThisPhase(long)
     */
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
            UtilFunctions.invokeInEventDispatchThreadAndWait(new Runnable() { // NOPMD
                                                                              // by
                                                                              // Braids
                                                                              // on
                                                                              // 8/18/11
                                                                              // 11:17
                                                                              // PM
                        @Override
                        public void run() {
                            // (Re)create the progress bar.
                            if (MultiPhaseProgressMonitorWithETA.this.dialog != null) {
                                MultiPhaseProgressMonitorWithETA.this.dialog.dispose();
                                MultiPhaseProgressMonitorWithETA.this.dialog = null;
                            }

                            MultiPhaseProgressMonitorWithETA.this.dialog = new GuiProgressBarWindow();
                        }
                    });
        }

        SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on
                                                    // 8/18/11 11:18 PM
                    @Override
                    public void run() {
                        MultiPhaseProgressMonitorWithETA.this.dialog
                                .setTitle(MultiPhaseProgressMonitorWithETA.this.title);
                        MultiPhaseProgressMonitorWithETA.this.dialog
                                .setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                        MultiPhaseProgressMonitorWithETA.this.dialog.setVisible(true);
                        MultiPhaseProgressMonitorWithETA.this.dialog.setResizable(true);
                        MultiPhaseProgressMonitorWithETA.this.dialog.getProgressBar().setIndeterminate(false);
                        MultiPhaseProgressMonitorWithETA.this.dialog.setProgressRange(0, (int) numUnits);
                        MultiPhaseProgressMonitorWithETA.this.dialog.reset();

                        final JProgressBar bar = MultiPhaseProgressMonitorWithETA.this.dialog.getProgressBar();
                        bar.setString("");
                        bar.setStringPainted(true);
                        bar.setValue(0);
                    }
                });

    }

    /*
     * (non-Javadoc)
     * 
     * @see net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor#
     * incrementUnitsCompletedThisPhase(long)
     */
    @Override
    /**
     * @see net.slightlymagic.braids.util.progress_monitor.ProgressMonitor#incrementUnitsCompletedThisPhase(long)
     */
    public final void incrementUnitsCompletedThisPhase(final long numUnits) {
        super.incrementUnitsCompletedThisPhase(numUnits);

        SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on
                                                    // 8/18/11 11:18 PM
                    @Override
                    public void run() {
                        for (int i = 0; i < numUnits; i++) {
                            MultiPhaseProgressMonitorWithETA.this.dialog.increment();
                        }
                    }
                });

        if (this.shouldUpdateUI()) {

            if ((this.getNumPhases() > 1)) {
                this.displayUpdate("Phase " + this.getCurrentPhase() + ". "
                // + getUnitsCompletedSoFarThisPhase() + " units processed. "
                // + "Overall: " + getTotalPercentCompleteAsString() +
                // "% complete, "
                        + "Overall ETA in " + this.getRelativeETAAsString() + ".");
            } else {
                this.displayUpdate(
                // "Overall: " +
                this.getUnitsCompletedSoFarThisPhase() + " units processed; "
                // + "(" + getTotalPercentCompleteAsString() + "%); "
                        + "ETA in " + this.getRelativeETAAsString() + ".");
            }
        }

        if ((this.getCurrentPhase() == this.getNumPhases())
                && (this.getUnitsCompletedSoFarThisPhase() >= this.getTotalUnitsThisPhase())) {
            this.displayUpdate("Done!");
        }
    }

    /**
     * Shows the message inside the progress dialog; does not always work on all
     * platforms.
     * 
     * @param message
     *            the message to display
     */
    public final void displayUpdate(final String message) {

        final Runnable proc = new Runnable() { // NOPMD by Braids on 8/18/11
                                               // 11:18 PM
            @Override
            public void run() {
                // i've been having trouble getting the dialog to display its
                // title.
                MultiPhaseProgressMonitorWithETA.this.dialog.setTitle(MultiPhaseProgressMonitorWithETA.this.title);

                final JProgressBar bar = MultiPhaseProgressMonitorWithETA.this.dialog.getProgressBar();
                bar.setString(message);

                MultiPhaseProgressMonitorWithETA.this.justUpdatedUI();
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            proc.run();
        } else {
            SwingUtilities.invokeLater(proc);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor#dispose
     * ()
     */
    @Override
    public final void dispose() {
        SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on
                                                    // 8/18/11 11:18 PM
                    @Override
                    public void run() {
                        MultiPhaseProgressMonitorWithETA.this.getDialog().dispose();
                    }
                });
    }

    /**
     * Gets the dialog.
     * 
     * @return the JDialog for the current phase; use this judiciously to
     *         manipulate the dialog directly.
     */
    public final JDialog getDialog() {
        return this.dialog;
    }

}
