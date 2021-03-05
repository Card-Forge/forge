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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;

import com.esotericsoftware.minlog.Log;

import forge.FTrace;
import forge.gui.error.BugReporter;
import forge.localinstance.properties.ForgeConstants;
import forge.util.MultiplexOutputStream;

/**
 * This class handles all exceptions that weren't caught by showing the error to
 * the user.
 * 
 * @author Forge
 * @version $Id$
 */
public class ExceptionHandler implements UncaughtExceptionHandler {
    static {
        // Tells Java to let this class handle any uncaught exception
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        // Tells AWT to let this class handle any uncaught exception
        System.setProperty("sun.awt.exception.handler", ExceptionHandler.class.getName());
    }

    private static PrintStream oldSystemOut;
    private static PrintStream oldSystemErr;
    private static OutputStream logFileStream;

    /**
     * Call this at the beginning to make sure that the class is loaded and the
     * static initializer has run.
     */
    public static void registerErrorHandling() {
        //initialize log file
        File logFile = new File(ForgeConstants.LOG_FILE);

        int i = 0;
        while (logFile.exists() && !logFile.delete()) {
            String pathname = logFile.getPath().replaceAll("[0-9]{0,2}.log$", i++ + ".log");
            logFile = new File(pathname);
        }
        
        if (!logFile.exists()) {
            try {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            logFileStream = new FileOutputStream(logFile);
        }
        catch (final FileNotFoundException e) {
            e.printStackTrace();
        }

        oldSystemOut = System.out;
        System.setOut(new PrintStream(new MultiplexOutputStream(System.out, logFileStream), true));
        oldSystemErr = System.err;
        System.setErr(new PrintStream(new MultiplexOutputStream(System.err, logFileStream), true));

        Log.debug("Error handling registered!");
        FTrace.initialize();
    }

    /**
     * Finalizer, generally should be avoided, but here closes the log file
     * stream and resets the system output streams.
     */
    public static void unregisterErrorHandling() throws IOException {
        FTrace.dump(); //dump trace before unregistering error handling
        System.setOut(oldSystemOut);
        System.setErr(oldSystemErr);
        logFileStream.close();
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
