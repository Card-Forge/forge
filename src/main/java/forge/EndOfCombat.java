package forge;

//handles "until end of combat" and "at end of combat" commands from cards
/**
 * <p>
 * EndOfCombat class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class EndOfCombat implements java.io.Serializable {

    /** Constant <code>serialVersionUID=3035250030566186842L</code>. */
    private static final long serialVersionUID = 3035250030566186842L;

    private CommandList at = new CommandList();
    private CommandList until = new CommandList();

    /**
     * <p>
     * addAt.
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void addAt(final Command c) {
        at.add(c);
    }

    /**
     * <p>
     * addUntil.
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void addUntil(final Command c) {
        until.add(c);
    }

    /**
     * <p>
     * executeAt.
     * </p>
     */
    public final void executeAt() {
        // AllZone.getStateBasedEffects().rePopulateStateBasedList();
        execute(at);
    } // executeAt()

    /**
     * <p>
     * executeUntil.
     * </p>
     */
    public final void executeUntil() {
        execute(until);
    }

    /**
     * <p>
     * sizeAt.
     * </p>
     * 
     * @return a int.
     */
    public final int sizeAt() {
        return at.size();
    }

    /**
     * <p>
     * sizeUntil.
     * </p>
     * 
     * @return a int.
     */
    public final int sizeUntil() {
        return until.size();
    }

    /**
     * <p>
     * execute.
     * </p>
     * 
     * @param c
     *            a {@link forge.CommandList} object.
     */
    private void execute(final CommandList c) {
        int length = c.size();

        for (int i = 0; i < length; i++) {
            c.remove(0).execute();
        }
    }

} // end class EndOfCombat
