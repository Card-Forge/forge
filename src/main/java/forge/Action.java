package forge;

/**
 * <p>
 * Command interface.
 * </p>
 * 
 * @author Forge
 * @version $Id: Command.java 12297 2011-11-28 19:56:47Z jendave $
 */
public interface Action<T> extends java.io.Serializable {
    /**
     * <p>
     * execute.
     * </p>
     */
    void perform(T argument);
}