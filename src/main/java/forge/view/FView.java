package forge.view;

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

}
