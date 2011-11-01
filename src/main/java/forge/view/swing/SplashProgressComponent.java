package forge.view.swing;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor;
import net.slightlymagic.braids.util.progress_monitor.BraidsProgressMonitor;

/**
 * Swing component view, to be used with BaseProgressMonitor.
 * 
 */
@SuppressWarnings("serial")
public class SplashProgressComponent extends JProgressBar {
    private BaseProgressMonitor currentModel;
    private double completed;
    private double total;

    /**
     * Constructor: Must be called from an event dispatch thread.
     * 
     */
    public SplashProgressComponent() {
        super();

        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("must be called from within an event dispatch thread");
        }

        this.setString("");
        this.setStringPainted(true);
    }

    /**
     * Updates progress bar stripe and text with current state of model.
     * 
     */
    public final void updateProgressBar() {
        // Update bar "stripe"
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SplashProgressComponent.this.stripeUpdate();
            }
        });

        // Update bar message
        // Note: shouldUpdateUI() severely retards motion
        // of the preloader, so is temporarily disabled.
        // if (getCurrentModel().shouldUpdateUI()) {
        if ((this.getCurrentModel().getNumPhases() > 1)) {
            this.displayUpdate("Phase " + this.getCurrentModel().getCurrentPhase() + ". "
            // + getUnitsCompletedSoFarThisPhase() + " units processed. "
            // + "Overall: " + getTotalPercentCompleteAsString() +
            // "% complete, "
                    + "Overall ETA in " + this.getCurrentModel().getRelativeETAAsString(true) + ".");
        } else {
            this.displayUpdate(
            // "Overall: " +
            this.getCurrentModel().getUnitsCompletedSoFarThisPhase() + " units processed; "
            // + "(" + getTotalPercentCompleteAsString() + "%); "
                    + "ETA in " + this.getCurrentModel().getRelativeETAAsString(true) + ".");
        }
        // getCurrentModel().justUpdatedUI();
        // }

        if ((this.getCurrentModel().getCurrentPhase() == this.getCurrentModel().getNumPhases())
                && (this.getCurrentModel().getUnitsCompletedSoFarThisPhase() >= this.getCurrentModel()
                        .getTotalUnitsThisPhase())) {
            this.displayUpdate("Done! Firing up the GUI...");
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
        final Runnable proc = new Runnable() {
            @Override
            public void run() {
                SplashProgressComponent.this.setString(message);
                SplashProgressComponent.this.getCurrentModel().justUpdatedUI();
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            proc.run();
        } else {
            SwingUtilities.invokeLater(proc);
        }
    }

    /**
     * Moves the stripe inside the progress dialog.
     * 
     */
    public final void stripeUpdate() {
        this.completed = this.getCurrentModel().getUnitsCompletedSoFarThisPhase();
        this.total = this.getCurrentModel().getTotalUnitsThisPhase();

        this.setValue((int) Math.round(((int) this.completed / this.total) * 100));
    }

    /**
     * Resets the stripe inside the progress dialog back to zero.
     * 
     */
    public final void stripeReset() {
        this.setValue(0);
    }

    /**
     * Retrieves the model from which this component uses data.
     * 
     * @param neoModel
     *            the new current model
     */
    public final void setCurrentModel(final BaseProgressMonitor neoModel) {
        this.currentModel = neoModel;
    }

    /**
     * Sets model from which this component uses data.
     * 
     * @return the current model
     */
    public final BraidsProgressMonitor getCurrentModel() {
        return this.currentModel;
    }
}
