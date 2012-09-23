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


/**
 * The Class Lambda1.
 *
 * @param <R> the generic type
 */
public abstract class Lambda0<R> implements Lambda<R> {

    /**
     * Apply.
     *
     * @return the r
     */
    public abstract R apply();

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.slightlymagic.braids.util.lambda.Lambda#apply(java.lang.Object[])
     */

    // TODO @Override
    /**
     * Apply.
     * 
     * @param args
     *            Object[]
     * @return R
     */
    @Override
    public final R apply(final Object[] args) {
        return this.apply();
    }

}
