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

import java.util.ArrayList;
import java.util.List;


/**
 * The Class Lambda1.
 * 
 * @param <R>
 *            the generic type
 * @param <A1>
 *            the generic type
 */
public abstract class Lambda1<R, A1> implements Lambda<R> {

    /**
     * Apply.
     * 
     * @param arg1
     *            the arg1
     * @return the r
     */
    public abstract R apply(A1 arg1);

    public List<R> applyToIterable(Iterable<A1> arg1) {
        List<R> result = new ArrayList<R>();
        for(A1 a : arg1) {
            result.add(this.apply(a));
        }
        return result;
    }
    
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
     * @param args Object[]
     * @return R
     */
    @Override
    @SuppressWarnings("unchecked")
    public final R apply(final Object[] args) {
        return apply((A1) args[0]);
    }

}
