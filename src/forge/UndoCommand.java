package forge;

/**
 * <p>UndoCommand interface.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public interface UndoCommand extends Command {
    /**
     * <p>execute.</p>
     */
    public void execute();

    /**
     * <p>undo.</p>
     */
    public void undo();
}
