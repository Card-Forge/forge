package forge;

/**
 * <p>UndoCommand interface.</p>
 *
 * @author Forge
 * @version $Id$
 */
public interface UndoCommand extends Command {
    /**
     * <p>execute.</p>
     */
    void execute();

    /**
     * <p>undo.</p>
     */
    void undo();
}
