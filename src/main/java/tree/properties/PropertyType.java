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
 * PropertyTypeHandler.java
 * 
 * Created on 19.08.2009
 *
 * @param <T> the generic type
 */

/**
 * The class PropertyType. A property type is used to process special, suffixed
 * entries in a {@link TreeProperties} ' properties-file
 * 
 * @author Clemens Koza
 * @version V0.0 19.08.2009
 * 
 * @param <T>
 * 
 */
public interface PropertyType<T> {
    /**
     * The suffix, not including "--", that identifies this content type.
     * 
     * @return a {@link java.lang.String} object.
     */
    String getSuffix();

    /**
     * The class that identifies this content type.
     * 
     * @return a {@link java.lang.Class} object.
     */
    Class<T> getType();

    /**
     * Returns an object for the specified value, in the context of a
     * TreeProperties.
     * 
     * @param p
     *            a {@link tree.properties.TreeProperties} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @return a T object.
     */
    T toObject(TreeProperties p, String s);
}
