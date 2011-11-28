/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2009  Clemens Koza
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
package tree.properties;

/**
 * The class PropertyElement.
 * 
 * @author Clemens Koza
 * @version V0.0 19.08.2009
 */
public interface PropertyElement {
    /**
     * Returns the key of the property in the TreeProperties.
     * 
     * @return a {@link java.lang.String} object.
     */
    String getKey();

    /**
     * Returns the type of the element.
     * 
     * @return a {@link java.lang.Class} object.
     */
    Class<?> getType();

    /**
     * Returns the value of the element.
     * 
     * @return a {@link java.lang.Object} object.
     */
    Object getValue();

    /**
     * Sets the property value as a string.
     * 
     * @param value
     *            a {@link java.lang.String} object.
     */
    void setValue(String value);
}
