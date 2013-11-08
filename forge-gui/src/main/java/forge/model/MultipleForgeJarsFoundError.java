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
package forge.model;

//import java.io.IOException;

/**
 * Exception thrown by model when it is trying to find a single forge jar, but
 * it finds more than one.
 */
public class MultipleForgeJarsFoundError extends RuntimeException {
    /** Automatically generated. */
    private static final long serialVersionUID = 8899307272033517172L;

    /**
     * Create an exception with a message.
     * 
     * @param message
     *            the message, which could be the System's class path.
     */
    public MultipleForgeJarsFoundError(final String message) {
        super(message);
    }

}
