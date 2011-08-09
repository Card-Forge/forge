package forge.gui;
/**
 * ForgeAction.java
 *
 * Created on 02.09.2009
 */


import forge.properties.ForgeProps;

import javax.swing.*;


/**
 * The class ForgeAction.
 *
 * @author Clemens Koza
 * @version V0.0 02.09.2009
 */
public abstract class ForgeAction extends AbstractAction {

    /** Constant <code>serialVersionUID=-1881183151063146955L</code> */
    private static final long serialVersionUID = -1881183151063146955L;
    private String property;

    /**
     * <p>Constructor for ForgeAction.</p>
     *
     * @param property A Property key containing the keys "/button" and "/menu".
     */
    public ForgeAction(String property) {
        super(ForgeProps.getLocalized(property + "/button"));
        this.property = property;
        putValue("buttonText", ForgeProps.getLocalized(property + "/button"));
        putValue("menuText", ForgeProps.getLocalized(property + "/menu"));
    }

    /**
     * <p>Getter for the field <code>property</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    protected String getProperty() {
        return property;
    }

    /**
     * <p>setupButton.</p>
     *
     * @param button a T object.
     * @return a T object.
     * @param <T> a T object.
     */
    public <T extends AbstractButton> T setupButton(T button) {
        button.setAction(this);
        button.setText((String) getValue(button instanceof JMenuItem ? "menuText" : "buttonText"));
        return button;
    }
}
