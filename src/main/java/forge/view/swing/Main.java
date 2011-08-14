package forge.view.swing;

import forge.Singletons;
import forge.error.ErrorViewer;
import forge.error.ExceptionHandler;
import forge.model.FModel;
import forge.view.FView;

/**
 * Main class for Forge's swing application view.
 */
public final class Main {

    /**
     * Do not instantiate.
     */
    private Main() {
        // intentionally blank
    }

    /**
     * main method for Forge's swing application view.
     *
     * @param args
     *            an array of {@link java.lang.String} objects.
     */
    public static void main(final String[] args) {
        ExceptionHandler.registerErrorHandling();
        try {
            final FView view = new ApplicationView();
            Singletons.setView(view);
            final FModel model = new FModel(null);
            Singletons.setModel(model);

            view.setModel(model);

        }
        catch (Throwable exn) { // NOPMD by Braids on 8/7/11 1:07 PM: must catch all throwables here.
            ErrorViewer.showError(exn);
        }
    }
}
