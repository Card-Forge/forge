package forge;

/**
 * <p>
 * Command interface, just like Guava Function but return type is void.
 * </p>
 * 
 * @author Forge
 * @version $Id: Command.java 12297 2011-11-28 19:56:47Z jendave $
 */
public interface Action<T> {
    /**
     * <p>
     * execute.
     * </p>
     */
    void perform(T argument);
}
