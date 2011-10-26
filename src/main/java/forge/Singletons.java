package forge;

import forge.model.FModel;
import forge.view.FView;

/**
 * Provides global/static access to singleton instances.
 */
public final class Singletons {

    private static FModel model = null;

    private static FView view = null;

    /**
     * Do not instantiate.
     */
    private Singletons() {
        // This line intentionally left blank.
    }

    /**
     * Gets the model.
     * 
     * @return the model
     */
    public static FModel getModel() {
        return model;
    }

    /**
     * Sets the model.
     * 
     * @param theModel
     *            the model to set
     */
    public static void setModel(final FModel theModel) {
        Singletons.model = theModel;
    }

    /**
     * Gets the view.
     * 
     * @return the view
     */
    public static FView getView() {
        return view;
    }

    /**
     * Sets the view.
     * 
     * @param theView
     *            the view to set
     */
    public static void setView(final FView theView) {
        Singletons.view = theView;
    }

}
