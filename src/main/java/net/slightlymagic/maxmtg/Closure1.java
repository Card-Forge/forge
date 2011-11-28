/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  MaxMtg
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
package net.slightlymagic.maxmtg;

import net.slightlymagic.braids.util.lambda.Lambda1;

/**
 * This class represents an action (lambda) and some arguments to make a call at
 * a later time.
 * 
 * @param <R>
 *            the generic type
 * @param <A1>
 *            the generic type
 */
public class Closure1<R, A1> {
    private final Lambda1<R, A1> method;
    private final A1 argument;

    /**
     * Instantiates a new closure1.
     * 
     * @param lambda
     *            the lambda
     * @param object
     *            the object
     */
    public Closure1(final Lambda1<R, A1> lambda, final A1 object) {
        this.method = lambda;
        this.argument = object;
    }

    /**
     * Apply.
     * 
     * @return the r
     */
    public R apply() {
        return this.method.apply(this.argument);
    }
}
