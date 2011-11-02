package forge;

/**
 * Contains a main delegate; this class used to be Forge's entry point.
 * 
 * The class that used to be here is presently at
 * {@link forge.view.swing.OldGuiNewGame}, which is slowly being refactored out
 * of existence.
 */
public final class GuiNewGame {

    /**
     * Do not instantiate.
     */
    private GuiNewGame() {
        // blank
    }

    /**
     * This is a delegate.
     * 
     * @param args
     *            from the OS or command line
     * 
     * @deprecated use {@link forge.view.swing.Main}
     */
    public static void main(final String[] args) {
        forge.view.swing.Main.main(args);
    }

}
