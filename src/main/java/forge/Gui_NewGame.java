package forge;

/**
 * Contains a main delegate; this class used to be Forge's entry point.
 */
public class Gui_NewGame {

    /**
     * Do not instantiate.
     */
    private Gui_NewGame() {
        // blank
    }

    /**
     * This is a delegate.
     * 
     * @deprecated use forge.view.swing.Main.main
     * 
     * @see forge.view.swing.Main.main
     * 
     * @param args from the OS or command line
     */
    public static void main(final String[] args) {
        forge.view.swing.Main.main(args);
    }

}
