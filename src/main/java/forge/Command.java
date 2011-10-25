package forge;

/**
 * <p>Command interface.</p>
 *
 * @author Forge
 * @version $Id$
 */
public interface Command extends java.io.Serializable {
    /** Constant <code>Blank</code>. */
    Command Blank = new Command() {

        private static final long serialVersionUID = 2689172297036001710L;

        public void execute() {
        }
    };

    /**
     * <p>execute.</p>
     */
    void execute();
}
