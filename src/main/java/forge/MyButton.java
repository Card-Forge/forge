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
package forge;

/**
 * <p>
 * MyButton interface.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public interface MyButton {
    // public MyButton(String buttonText, Command command)
    /**
     * <p>
     * select.
     * </p>
     */
    void select();

    /**
     * <p>
     * setSelectable.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    void setSelectable(boolean b);

    /**
     * <p>
     * isSelectable.
     * </p>
     * 
     * @return a boolean.
     */
    boolean isSelectable();

    /**
     * <p>
     * getText.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    String getText();

    /**
     * <p>
     * setText.
     * </p>
     * 
     * @param text
     *            a {@link java.lang.String} object.
     */
    void setText(String text);

    /**
     * <p>
     * reset.
     * </p>
     */
    void reset(); // resets the text and calls setSelectable(false)
}
