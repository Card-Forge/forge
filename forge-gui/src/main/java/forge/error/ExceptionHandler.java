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
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
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

    private static final String LOG_SUFFIX = ".log";
    private static final String TIMESTAMP_FORMAT = "yyyyMMdd-HHmmss";
    // Each running instance owns one forge<N>.log "slot" for its lifetime (forge.log = slot 0,
    // forge1.log = slot 1, ...); after the owner exits, the next startup archives the file to forge.<ts>.log
    private static final Pattern SLOT_PATTERN = Pattern.compile("forge\\d*\\.log");
    // Slot files deliberately don't match — they may be live
    private static final Pattern ARCHIVE_PATTERN = Pattern.compile("forge\\.\\d{8}-\\d{6}(\\.\\d+)?\\.log");

    private static PrintStream oldSystemOut;
    private static PrintStream oldSystemErr;
    private static OutputStream logFileStream;
    private static File activeLogFile;
    private static FileChannel logChannel;
    private static FileLock logLock;

    /** The log file this JVM is writing to, null until {@link #registerErrorHandling()} has run */
    public static File getActiveLogFile() {
        return activeLogFile;
    }

    /**
     * Call this at the beginning to make sure that the class is loaded and the
     * static initializer has run.
     */
    public static void registerErrorHandling() {
        File parent = new File(ForgeConstants.LOG_FILE).getParentFile();
        parent.mkdirs();

        // Archive slot files whose JVM has exited. FileLock probes liveness:
        // POSIX rename succeeds even when another process has the file open, so
        // rename-success isn't a liveness signal. FileLock is honored cross-process.
        File[] existingSlots = parent.listFiles(f -> f.isFile() && SLOT_PATTERN.matcher(f.getName()).matches());
        if (existingSlots != null) {
            SimpleDateFormat ts = new SimpleDateFormat(TIMESTAMP_FORMAT);
            for (File slot : existingSlots) {
                boolean unowned = false;
                try (FileChannel probe = FileChannel.open(slot.toPath(), StandardOpenOption.WRITE)) {
                    FileLock lock = probe.tryLock();
                    if (lock != null) {
                        lock.release();
                        unowned = true;
                    }
                } catch (IOException ignored) {}
                if (unowned) {
                    File archive = nextAvailable(new File(parent,
                            "forge." + ts.format(new Date(slot.lastModified())) + LOG_SUFFIX));
                    slot.renameTo(archive);
                }
            }
        }

        // CREATE_NEW is atomic, so concurrent startups can't both claim the same slot
        for (int n = 0; ; n++) {
            File slot = slotFile(parent, n);
            try {
                FileChannel ch = FileChannel.open(slot.toPath(),
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                FileLock lock = ch.tryLock();
                if (lock != null) {
                    logChannel = ch;
                    logLock = lock;
                    logFileStream = Channels.newOutputStream(ch);
                    activeLogFile = slot;
                    break;
                }
                ch.close();
            } catch (FileAlreadyExistsException e) {
                // slot owned by another live instance; try next
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        if (logFileStream != null) {
            oldSystemOut = System.out;
            System.setOut(new PrintStream(new MultiplexOutputStream(System.out, logFileStream), true));
            oldSystemErr = System.err;
            System.setErr(new PrintStream(new MultiplexOutputStream(System.err, logFileStream), true));
            // no logger here, if it ever fails we'll know at least we passed through here
            System.out.println("Error handling registered!");
        }
        FTrace.initialize();
    }

    private static File slotFile(File parent, int n) {
        return new File(parent, n == 0 ? "forge.log" : "forge" + n + ".log");
    }

    // Same-second collisions: append .1, .2, ... until unused
    private static File nextAvailable(File target) {
        if (!target.exists()) return target;
        String base = target.getPath().substring(0, target.getPath().length() - LOG_SUFFIX.length());
        for (int i = 1; ; i++) {
            File alt = new File(base + "." + i + LOG_SUFFIX);
            if (!alt.exists()) return alt;
        }
    }

    /** Delete oldest forge.<ts>.log backups until at most maxFiles remain. Per-process logs are not pruned. */
    public static void pruneForgeLogs(int maxFiles) {
        if (maxFiles <= 0) return;
        try {
            File dir = new File(ForgeConstants.LOG_FILE).getParentFile();
            if (dir == null || !dir.isDirectory()) return;
            File[] archives = dir.listFiles(f -> f.isFile() && ARCHIVE_PATTERN.matcher(f.getName()).matches());
            if (archives == null || archives.length <= maxFiles) return;
            Arrays.sort(archives, Comparator.comparingLong(File::lastModified));
            int toDelete = archives.length - maxFiles;
            for (int i = 0; i < toDelete; i++) archives[i].delete();
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
        if (oldSystemOut != null) System.setOut(oldSystemOut);
        if (oldSystemErr != null) System.setErr(oldSystemErr);
        try {
            if (logFileStream != null) logFileStream.close();
        } finally {
            if (logChannel != null) {
                try { logChannel.close(); } catch (IOException ignored) {}
                logChannel = null;
            }
            // lock is auto-released when its channel closes
            logLock = null;
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
