package forge.view.util;

import javax.swing.SwingUtilities;

import forge.view.util.ProgressBar_Base;

/**
 * GUI Progress Monitor that displays the ETA (Estimated Time of Arrival or
 * completion) on some platforms and supports one or multiple phases of
 * progress.
 *
 * In this implementation, the progress bar must be embedded in a parent component.
 */
@SuppressWarnings("serial")
public class ProgressBar_Embedded extends ProgressBar_Base {

    public ProgressBar_Embedded(int numPhases, long totalUnitsFirstPhase) {
        super(numPhases, totalUnitsFirstPhase);
        
        setIndeterminate(false);
        setString("");
        setStringPainted(true);
        setValue(0);
    }
    
   // public void increment() {
   //     setValue(getValue() + 1);
   //     System.out.println("x");
   //     //if (getValue() % 10 == 0) { repaint(); }
   // }

    @Override
    /**
     * @see forge.view.util.ProgressBar_Base#incrementUnitsCompletedThisPhase(long)
     */
    public final void incrementUnitsCompletedThisPhase(final long numUnits) {
        super.incrementUnitsCompletedThisPhase(numUnits);
        SwingUtilities.invokeLater(new Runnable() { // NOPMD by Braids on 8/18/11 11:18 PM
            public void run() {
                for (int i = 0; i < numUnits; i++) {
                   increment();
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
                setString(message);
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


}
