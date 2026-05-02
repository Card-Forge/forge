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

import forge.FTrace;
import forge.gui.error.BugReporter;
import forge.localinstance.properties.ForgeConstants;
import forge.util.MultiplexOutputStream;

import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Pattern;

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

    private static final String ROTATED_PREFIX = "forge.";
    private static final String ROTATED_SUFFIX = ".log";
    private static final String TIMESTAMP_FORMAT = "yyyyMMdd-HHmmss";
    // Matches forge.<ts>.log and collision-fallback forge.<ts>.<N>.log; excludes per-process forge.<ts>-<N>.log
    private static final Pattern ROTATED_PATTERN = Pattern.compile("forge\\.\\d{8}-\\d{6}(\\.\\d+)?\\.log");

    private static PrintStream oldSystemOut;
    private static PrintStream oldSystemErr;
    private static OutputStream logFileStream;
    private static File activeLogFile;
    private static FileChannel logLockChannel;
    private static FileLock logLock;

    /** The log file this JVM is writing to, or null if registration hasn't run. */
    public static File getActiveLogFile() {
        return activeLogFile;
    }

    /**
     * Call this at the beginning to make sure that the class is loaded and the
     * static initializer has run.
     */
    public static void registerErrorHandling() {
        File primaryLog = new File(ForgeConstants.LOG_FILE);
        primaryLog.getParentFile().mkdirs();

        // Per-JVM advisory lock; first instance owns forge.log, others write to a per-process file.
        File lockFile = new File(primaryLog.getPath() + ".lock");
        try {
            logLockChannel = FileChannel.open(lockFile.toPath(),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            logLock = logLockChannel.tryLock();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File logFile;
        if (logLock != null) {
            if (primaryLog.exists() && primaryLog.length() > 0) {
                primaryLog.renameTo(nextAvailable(rotatedTargetFor(primaryLog)));
            }
            logFile = primaryLog;
        } else {
            if (logLockChannel != null) {
                try { logLockChannel.close(); } catch (IOException ignored) {}
                logLockChannel = null;
            }
            logFile = claimProcessUniqueLog(primaryLog);
        }
        activeLogFile = logFile;

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

        // no logger here, if it ever fails we'll know at least we passed through here
        System.out.println("Error handling registered!");
        FTrace.initialize();
    }

    private static File rotatedTargetFor(File previousLog) {
        String stamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date(previousLog.lastModified()));
        return new File(previousLog.getParentFile(), ROTATED_PREFIX + stamp + ROTATED_SUFFIX);
    }

    // Same-second collisions: append .1, .2, ... until unused; nanoTime as final fallback. Uses dot
    // (not dash) so collision-rotated files stay distinguishable from per-process forge.<ts>-<N>.log.
    private static File nextAvailable(File target) {
        if (!target.exists()) return target;
        String base = target.getPath().substring(0, target.getPath().length() - ROTATED_SUFFIX.length());
        for (int i = 1; i < 1000; i++) {
            File alt = new File(base + "." + i + ROTATED_SUFFIX);
            if (!alt.exists()) return alt;
        }
        return new File(base + "." + System.nanoTime() + ROTATED_SUFFIX);
    }

    // Atomic slot claim via createNewFile; nanoTime as final fallback to guarantee uniqueness.
    private static File claimProcessUniqueLog(File primaryLog) {
        String stamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
        File parent = primaryLog.getParentFile();
        for (int i = 1; i < 1000; i++) {
            File candidate = new File(parent, ROTATED_PREFIX + stamp + "-" + i + ROTATED_SUFFIX);
            try {
                if (candidate.createNewFile()) return candidate;
            } catch (IOException ignored) {}
        }
        return new File(parent, ROTATED_PREFIX + stamp + "-" + System.nanoTime() + ROTATED_SUFFIX);
    }

    /** Delete oldest forge.<ts>.log backups until at most maxFiles remain. Per-process logs are not pruned. */
    public static void pruneForgeLogs(int maxFiles) {
        if (maxFiles <= 0) return;
        try {
            File dir = new File(ForgeConstants.LOG_FILE).getParentFile();
            if (dir == null || !dir.isDirectory()) return;
            File[] rotated = dir.listFiles(f -> f.isFile() && ROTATED_PATTERN.matcher(f.getName()).matches());
            if (rotated == null || rotated.length <= maxFiles) return;
            Arrays.sort(rotated, Comparator.comparingLong(File::lastModified));
            int toDelete = rotated.length - maxFiles;
            for (int i = 0; i < toDelete; i++) rotated[i].delete();
        } catch (Exception ignored) {
            // non-critical — never fail the app over log cleanup
        }
    }

    /**
     * Finalizer, generally should be avoided, but here closes the log file
     * stream and resets the system output streams.
     */
    public static void unregisterErrorHandling() throws IOException {
        FTrace.dump(); //dump trace before unregistering error handling
        System.setOut(oldSystemOut);
        System.setErr(oldSystemErr);
        try {
            logFileStream.close();
        } finally {
            if (logLock != null) {
                try { logLock.release(); } catch (IOException ignored) {}
                logLock = null;
            }
            if (logLockChannel != null) {
                try { logLockChannel.close(); } catch (IOException ignored) {}
                logLockChannel = null;
            }
        }
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
