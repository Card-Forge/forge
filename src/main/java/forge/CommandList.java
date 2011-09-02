package forge;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * <p>CommandList class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class CommandList implements java.io.Serializable, Command, Iterable<Command> {
    /** Constant <code>serialVersionUID=-1532687201812613302L</code>. */
    private static final long serialVersionUID = -1532687201812613302L;

    private ArrayList<Command> a = new ArrayList<Command>();

    /**
     * default constructor
     * TODO Write javadoc for Constructor.
     */
    public CommandList() {
        //nothing to do
    }

    /**
     * constructor
     * TODO Write javadoc for Constructor.
     * @param c a Command
     */
    public CommandList(final Command c) {
        a.add(c);
    }

    /**
     * <p>iterator.</p>
     *
     * @return a {@link java.util.Iterator} object.
     */
    public final Iterator<Command> iterator() {
        return a.iterator();
    }

    //bug fix, when token is pumped up like with Giant Growth
    //and Sorceress Queen targets token, the effects need to be done
    //in this order, weird I know, DO NOT CHANGE THIS
    /**
     * <p>add.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void add(final Command c) {
        a.add(0, c);
    }


    /**
     * <p>get.</p>
     *
     * @param i a int.
     * @return a {@link forge.Command} object.
     */
    public final Command get(final int i) {
        return (Command) a.get(i);
    }

    /**
     * <p>remove.</p>
     *
     * @param i a int.
     * @return a {@link forge.Command} object.
     */
    public final Command remove(final int i) {
        return (Command) a.remove(i);
    }

    /**
     * <p>size.</p>
     *
     * @return a int.
     */
    public final int size() {
        return a.size();
    }

    /**
     * <p>clear.</p>
     */
    public final void clear() {
        a.clear();
    }

    /**
     * <p>execute.</p>
     */
    public final void execute() {
        for (int i = 0; i < size(); i++) {
            get(i).execute();
        }
    }

}
