package forge.gui;

/**
 * ForgeAction.java
 *
 * Created on 02.09.2009
 */

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JMenuItem;

import forge.properties.ForgeProps;

/**
 * The class ForgeAction.
 * 
 * @author Clemens Koza
 * @version V0.0 02.09.2009
 */
public abstract class ForgeAction extends AbstractAction {

    /** Constant <code>serialVersionUID=-1881183151063146955L</code>. */
    private static final long serialVersionUID = -1881183151063146955L;
    private final String property;

    /**
     * <p>
     * Constructor for ForgeAction.
     * </p>
     * 
     * @param property
     *            A Property key containing the keys "/button" and "/menu".
     */
    public ForgeAction(final String property) {
        super(ForgeProps.getLocalized(property + "/button"));
        this.property = property;
        this.putValue("buttonText", ForgeProps.getLocalized(property + "/button"));
        this.putValue("menuText", ForgeProps.getLocalized(property + "/menu"));
    }

    /**
     * <p>
     * Getter for the field <code>property</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    protected String getProperty() {
        return this.property;
    }

    /**
     * <p>
     * setupButton.
     * </p>
     *
     * @param <T> a T object.
     * @param button a T object.
     * @return a T object.
     */
    public <T extends AbstractButton> T setupButton(final T button) {
        button.setAction(this);
        button.setText((String) this.getValue(button instanceof JMenuItem ? "menuText" : "buttonText"));
        return button;
    }
}
