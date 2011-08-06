package forge;
public interface UndoCommand extends Command
{
    public void execute();
    public void undo();
}
