package forge;

/**
 * <p>MyButton interface.</p>
 *
 * @author Forge
 * @version $Id$
 */
public interface MyButton {
    //  public MyButton(String buttonText, Command command)
    /**
     * <p>select.</p>
     */
    void select();

    /**
     * <p>setSelectable.</p>
     *
     * @param b a boolean.
     */
    void setSelectable(boolean b);

    /**
     * <p>isSelectable.</p>
     *
     * @return a boolean.
     */
    boolean isSelectable();

    /**
     * <p>getText.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getText();

    /**
     * <p>setText.</p>
     *
     * @param text a {@link java.lang.String} object.
     */
    void setText(String text);

    /**
     * <p>reset.</p>
     */
    void reset(); //resets the text and calls setSelectable(false)
}
