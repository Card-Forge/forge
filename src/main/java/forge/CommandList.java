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
    /** Constant <code>serialVersionUID=-1532687201812613302L</code> */
    private static final long serialVersionUID = -1532687201812613302L;

    private ArrayList<Command> a = new ArrayList<Command>();
    
    public CommandList() {
    	;
    }
    
    public CommandList(Command c) {
    	a.add(c);
    }

    /**
     * <p>iterator.</p>
     *
     * @return a {@link java.util.Iterator} object.
     */
    public Iterator<Command> iterator() {
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
    public void add(Command c) {
        a.add(0, c);
    }


    /**
     * <p>get.</p>
     *
     * @param i a int.
     * @return a {@link forge.Command} object.
     */
    public Command get(int i) {
        return (Command) a.get(i);
    }

    /**
     * <p>remove.</p>
     *
     * @param i a int.
     * @return a {@link forge.Command} object.
     */
    public Command remove(int i) {
        return (Command) a.remove(i);
    }

    /**
     * <p>size.</p>
     *
     * @return a int.
     */
    public int size() {
        return a.size();
    }

    /**
     * <p>clear.</p>
     */
    public void clear() {
        a.clear();
    }

    /**
     * <p>execute.</p>
     */
    public void execute() {
        for (int i = 0; i < size(); i++)
            get(i).execute();
    }

}
