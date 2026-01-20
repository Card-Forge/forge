package forge.gamemodes.net;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Debug logger for network/delta sync operations.
 * Writes to both console and a log file for easier debugging.
 *
 * Log files are created in the logs/ directory with unique names
 * based on timestamp and process ID to handle multiple instances.
 */
public final class NetworkDebugLogger {

    private static final String LOG_DIR = "logs";
    private static final String LOG_PREFIX = "network-debug";

    private static PrintWriter fileWriter;
    private static boolean initialized = false;
    private static boolean enabled = true;
    private static final Object lock = new Object();

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    private NetworkDebugLogger() {
        // Utility class
    }

    /**
     * Enable or disable logging. When disabled, log() calls are no-ops
     * but error() calls still go to System.err (not to file).
     */
    public static void setEnabled(boolean enabled) {
        NetworkDebugLogger.enabled = enabled;
    }

    /**
     * Check if logging is enabled.
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Initialize the logger if not already done.
     * Creates the logs/ directory and opens a unique log file.
     */
    private static void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (lock) {
            if (initialized) {
                return;
            }
            try {
                // Create logs directory
                File logDir = new File(LOG_DIR);
                if (!logDir.exists()) {
                    logDir.mkdirs();
                }

                // Create unique filename with timestamp and PID
                String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                long pid = ProcessHandle.current().pid();
                String filename = String.format("%s-%s-%d.log", LOG_PREFIX, timestamp, pid);
                File logFile = new File(logDir, filename);

                fileWriter = new PrintWriter(new FileWriter(logFile, true), true);

                // Log header
                fileWriter.println("=".repeat(80));
                fileWriter.println("Network Debug Log Started: " + new Date());
                fileWriter.println("PID: " + pid);
                fileWriter.println("Log file: " + logFile.getAbsolutePath());
                fileWriter.println("=".repeat(80));
                fileWriter.println();

                System.out.println("[NetworkDebugLogger] Logging to: " + logFile.getAbsolutePath());

                initialized = true;
            } catch (IOException e) {
                System.err.println("[NetworkDebugLogger] Failed to initialize log file: " + e.getMessage());
                // Continue without file logging
                initialized = true;
            }
        }
    }

    /**
     * Log a debug message. Writes to both console and file.
     */
    public static void log(String message) {
        if (!enabled) {
            return;
        }
        ensureInitialized();

        String timestamped = formatMessage("INFO", message);
        System.out.println(timestamped);
        writeToFile(timestamped);
    }

    /**
     * Log a formatted debug message. Writes to both console and file.
     */
    public static void log(String format, Object... args) {
        if (!enabled) {
            return;
        }
        log(String.format(format, args));
    }

    /**
     * Log an error message. Writes to both console (stderr) and file.
     * Error messages are logged even when logging is disabled.
     */
    public static void error(String message) {
        ensureInitialized();

        String timestamped = formatMessage("ERROR", message);
        System.err.println(timestamped);
        writeToFile(timestamped);
    }

    /**
     * Log a formatted error message. Writes to both console (stderr) and file.
     */
    public static void error(String format, Object... args) {
        error(String.format(format, args));
    }

    /**
     * Log an error with exception. Writes to both console (stderr) and file.
     */
    public static void error(String message, Throwable t) {
        ensureInitialized();

        String timestamped = formatMessage("ERROR", message);
        System.err.println(timestamped);
        t.printStackTrace(System.err);

        writeToFile(timestamped);
        if (fileWriter != null) {
            synchronized (lock) {
                t.printStackTrace(fileWriter);
                fileWriter.flush();
            }
        }
    }

    /**
     * Log a hex dump of bytes. Useful for debugging serialization issues.
     */
    public static void hexDump(String label, byte[] bytes, int errorPosition) {
        ensureInitialized();

        StringBuilder sb = new StringBuilder();
        sb.append(label).append("\n");

        int start = Math.max(0, errorPosition - 32);
        int end = Math.min(bytes.length, errorPosition + 64);

        sb.append(String.format("Bytes %d-%d (error at %d):%n", start, end - 1, errorPosition));

        for (int i = start; i < end; i += 16) {
            sb.append(String.format("%04d: ", i));

            // Hex values
            for (int j = 0; j < 16 && i + j < end; j++) {
                if (i + j == errorPosition) {
                    sb.append("[");
                }
                sb.append(String.format("%02X", bytes[i + j] & 0xFF));
                if (i + j == errorPosition) {
                    sb.append("]");
                } else {
                    sb.append(" ");
                }
            }

            // Padding if needed
            for (int j = Math.min(16, end - i); j < 16; j++) {
                sb.append("   ");
            }

            sb.append(" | ");

            // ASCII representation
            for (int j = 0; j < 16 && i + j < end; j++) {
                byte b = bytes[i + j];
                if (b >= 32 && b < 127) {
                    sb.append((char) b);
                } else {
                    sb.append('.');
                }
            }

            sb.append("\n");
        }

        String output = sb.toString();
        System.err.print(output);
        writeToFile(formatMessage("HEXDUMP", label));
        writeToFile(output);
    }

    /**
     * Format a message with timestamp and level.
     */
    private static String formatMessage(String level, String message) {
        String timestamp = TIMESTAMP_FORMAT.format(new Date());
        return String.format("[%s] [%s] %s", timestamp, level, message);
    }

    /**
     * Write to log file if available.
     */
    private static void writeToFile(String message) {
        if (fileWriter != null) {
            synchronized (lock) {
                fileWriter.println(message);
                fileWriter.flush();
            }
        }
    }

    /**
     * Close the log file. Call on application shutdown.
     */
    public static void close() {
        synchronized (lock) {
            if (fileWriter != null) {
                fileWriter.println();
                fileWriter.println("=".repeat(80));
                fileWriter.println("Network Debug Log Ended: " + new Date());
                fileWriter.println("=".repeat(80));
                fileWriter.close();
                fileWriter = null;
                initialized = false;
            }
        }
    }

    /**
     * Get the path to the current log file, or null if not initialized.
     */
    public static String getLogFilePath() {
        ensureInitialized();
        // We don't store the path, so just return the directory
        return new File(LOG_DIR).getAbsolutePath();
    }
}
