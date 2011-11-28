/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
     * @param <T>
     *            a T object.
     * @param button
     *            a T object.
     * @return a T object.
     */
    public <T extends AbstractButton> T setupButton(final T button) {
        button.setAction(this);
        button.setText((String) this.getValue(button instanceof JMenuItem ? "menuText" : "buttonText"));
        return button;
    }
}
