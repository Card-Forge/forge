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
package forge.error;

import java.lang.Thread.UncaughtExceptionHandler;

public class ExceptionHandler implements UncaughtExceptionHandler {
    static {
        // Tells Java to let this class handle any uncaught exception
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        // Tells AWT to let this class handle any uncaught exception
        System.setProperty("sun.awt.exception.handler", ExceptionHandler.class.getName());
    }

    /** {@inheritDoc} */
    @Override
    public final void uncaughtException(final Thread t, final Throwable ex) {
        BugReporter.reportException(ex);
    }

    /**
     * This Method is called by AWT when an error is thrown in the event
     * dispatching thread and not caught.
     * 
     * @param ex
     *            a {@link java.lang.Throwable} object.
     */
    public final void handle(final Throwable ex) {
        BugReporter.reportException(ex);
    }
}
