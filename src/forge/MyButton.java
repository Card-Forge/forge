package forge;

/**
 * <p>MyButton interface.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public interface MyButton {
    //  public MyButton(String buttonText, Command command)
    /**
     * <p>select.</p>
     */
    public void select();

    /**
     * <p>setSelectable.</p>
     *
     * @param b a boolean.
     */
    public void setSelectable(boolean b);

    /**
     * <p>isSelectable.</p>
     *
     * @return a boolean.
     */
    public boolean isSelectable();

    /**
     * <p>getText.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getText();

    /**
     * <p>setText.</p>
     *
     * @param text a {@link java.lang.String} object.
     */
    public void setText(String text);

    /**
     * <p>reset.</p>
     */
    public void reset(); //resets the text and calls setSelectable(false)
}
