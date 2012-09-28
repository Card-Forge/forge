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
package forge.util.closures;

import com.google.common.base.Function;


/**
 * The Class Lambda1.
 * 
 * @param <R>
 *            the generic type
 * @param <A1>
 *            the generic type
 */
public abstract class Lambda1<R, A1> implements Function<A1, R> {

    /**
     * Apply.
     * 
     * @param arg1
     *            the arg1
     * @return the r
     */
    public abstract R apply(A1 arg1);
}
