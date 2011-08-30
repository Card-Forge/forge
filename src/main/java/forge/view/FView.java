package forge.view;

import net.slightlymagic.braids.util.progress_monitor.BraidsProgressMonitor;
import forge.model.FModel;

/**
 * Generic view (as in model-view-controller) interface for Forge.
 */
public interface FView {

    /**
     * Tell the view that the model has been bootstrapped, and its data is
     * ready for initial display.
     *
     * @param model  the model that has finished bootstrapping
     */
    void setModel(FModel model);

    /**
     * Get the progress monitor for loading all cards at once.
     * 
     * @return a progress monitor having only one phase; may be null
     */
    BraidsProgressMonitor getCardLoadingProgressMonitor();
}
