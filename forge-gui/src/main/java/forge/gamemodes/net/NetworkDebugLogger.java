package forge.gamemodes.net;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Debug logger for network/delta sync operations.
 * Writes to both console and a log file for easier debugging.
 *
 * Log files are created in the logs/ directory with unique names
 * based on timestamp and process ID to handle multiple instances.
 *
 * Supports configurable verbosity levels for console vs file output:
 * - DEBUG: Detailed tracing (hex dumps, property details, collection contents)
 * - INFO: Normal operation (sync start/end, summaries)
 * - WARN: Potential issues (missing objects, unexpected states)
 * - ERROR: Failures and exceptions
 *
 * By default, console shows INFO and above, file shows DEBUG and above.
 */
public final class NetworkDebugLogger {

    /**
     * Log levels from most to least verbose.
     */
    public enum LogLevel {
        DEBUG(0),   // Detailed tracing
        INFO(1),    // Normal operation
        WARN(2),    // Potential issues
        ERROR(3);   // Failures

        private final int priority;

        LogLevel(int priority) {
            this.priority = priority;
        }

        /**
         * Check if this level should be logged when the threshold is set to the given level.
         */
        public boolean isLoggable(LogLevel threshold) {
            return this.priority >= threshold.priority;
        }
    }

    private static final String LOG_PREFIX = "network-debug";
    private static final long GRACE_PERIOD_MS = 5 * 60 * 1000; // 5 minutes

    private static PrintWriter fileWriter;
    private static boolean initialized = false;
    private static boolean enabled = true;
    private static LogLevel consoleLevel = LogLevel.INFO;  // Default: show INFO and above on console
    private static LogLevel fileLevel = LogLevel.DEBUG;    // Default: show DEBUG and above in file
    private static final Object lock = new Object();

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    // Apply configuration from NetworkDebug.config on class load
    static {
        applyConfig();
    }

    private NetworkDebugLogger() {
        // Utility class
    }

    /**
     * Apply configuration from NetworkDebug.config.
     * Called automatically on class initialization, but can also be called
     * manually to reload configuration.
     */
    public static void applyConfig() {
        enabled = NetworkDebugConfig.isDebugLoggerEnabled();
        consoleLevel = NetworkDebugConfig.getConsoleLogLevel();
        fileLevel = NetworkDebugConfig.getFileLogLevel();
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
     * Set the minimum log level for console output.
     * Messages below this level will not be printed to console.
     * Default: INFO
     */
    public static void setConsoleLevel(LogLevel level) {
        consoleLevel = level;
    }

    /**
     * Get the current console log level.
     */
    public static LogLevel getConsoleLevel() {
        return consoleLevel;
    }

    /**
     * Set the minimum log level for file output.
     * Messages below this level will not be written to the log file.
     * Default: DEBUG
     */
    public static void setFileLevel(LogLevel level) {
        fileLevel = level;
    }

    /**
     * Get the current file log level.
     */
    public static LogLevel getFileLevel() {
        return fileLevel;
    }

    /**
     * Get list of existing log files in the log directory.
     * Returns files sorted by last modified time (oldest first).
     */
    private static List<File> getExistingLogFiles() {
        String logDirPath = NetworkDebugConfig.getLogDirectory();
        File logsDir = new File(logDirPath);

        if (!logsDir.exists() || !logsDir.isDirectory()) {
            return Collections.emptyList();
        }

        File[] files = logsDir.listFiles((dir, name) ->
            name.startsWith(LOG_PREFIX + "-") && name.endsWith(".log")
        );

        if (files == null) {
            return Collections.emptyList();
        }

        // Sort by last modified time (oldest first)
        return Arrays.stream(files)
            .sorted(Comparator.comparing(File::lastModified))
            .collect(Collectors.toList());
    }

    /**
     * Clean up old log files if cleanup is enabled and max limit is exceeded.
     * Respects grace period to avoid deleting logs from other running instances.
     */
    private static void cleanupOldLogs() {
        if (!NetworkDebugConfig.isLogCleanupEnabled()) {
            return;
        }

        int maxLogs = NetworkDebugConfig.getMaxLogFiles();
        if (maxLogs <= 0) {
            // 0 or negative means no limit
            return;
        }

        List<File> logFiles = getExistingLogFiles();

        // Account for the new log file we're about to create
        int toDelete = logFiles.size() - maxLogs + 1;
        if (toDelete <= 0) {
            return;
        }

        long now = System.currentTimeMillis();

        // Delete oldest files, but respect grace period
        for (int i = 0; i < toDelete && i < logFiles.size(); i++) {
            File oldLog = logFiles.get(i);

            // Skip files modified within grace period (likely from other running instances)
            long age = now - oldLog.lastModified();
            if (age < GRACE_PERIOD_MS) {
                continue;
            }

            try {
                oldLog.delete();
                // Silent operation - no console output
            } catch (SecurityException e) {
                // Silent failure - continue with other deletions
            }
        }
    }

    /**
     * Initialize the logger if not already done.
     * Creates the log directory and opens a unique log file.
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
                // Get configurable log directory
                String logDirPath = NetworkDebugConfig.getLogDirectory();
                File logDir = new File(logDirPath);
                if (!logDir.exists()) {
                    logDir.mkdirs();
                }

                // Clean up old logs before creating new one
                cleanupOldLogs();

                // Create unique filename with timestamp and PID
                String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                long pid = ProcessHandle.current().pid();
                String filename = String.format("%s-%s-%d.log", LOG_PREFIX, timestamp, pid);
                File logFile = new File(logDir, filename);

                fileWriter = new PrintWriter(new FileWriter(logFile, true), true);

                // Log header with system information
                Runtime runtime = Runtime.getRuntime();
                fileWriter.println("=".repeat(80));
                fileWriter.println("Network Debug Log Started: " + new Date());
                fileWriter.println("PID: " + pid);
                fileWriter.println("Log file: " + logFile.getAbsolutePath());
                fileWriter.println();
                fileWriter.println("System Information:");
                fileWriter.println("  Java Version: " + System.getProperty("java.version"));
                fileWriter.println("  Java Vendor: " + System.getProperty("java.vendor"));
                fileWriter.println("  Max Memory: " + (runtime.maxMemory() / 1024 / 1024) + " MB");
                fileWriter.println("  Available Processors: " + runtime.availableProcessors());
                fileWriter.println("  OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
                fileWriter.println("  OS Arch: " + System.getProperty("os.arch"));
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
     * Internal method to log a message at a specific level.
     * Respects console and file level thresholds.
     */
    private static void logAtLevel(LogLevel level, String message) {
        ensureInitialized();

        String timestamped = formatMessage(level.name(), message);

        // Write to console if level meets threshold
        if (level.isLoggable(consoleLevel)) {
            if (level == LogLevel.ERROR) {
                System.err.println(timestamped);
            } else {
                System.out.println(timestamped);
            }
        }

        // Write to file if level meets threshold
        if (level.isLoggable(fileLevel)) {
            writeToFile(timestamped);
        }
    }

    /**
     * Log a DEBUG level message. Detailed tracing information.
     * By default, DEBUG messages go to file only, not console.
     */
    public static void debug(String message) {
        if (!enabled) {
            return;
        }
        logAtLevel(LogLevel.DEBUG, message);
    }

    /**
     * Log a formatted DEBUG level message.
     */
    public static void debug(String format, Object... args) {
        if (!enabled) {
            return;
        }
        debug(String.format(format, args));
    }

    /**
     * Log an INFO level message. Normal operation information.
     * By default, INFO messages go to both console and file.
     */
    public static void log(String message) {
        if (!enabled) {
            return;
        }
        logAtLevel(LogLevel.INFO, message);
    }

    /**
     * Log a formatted INFO level message.
     */
    public static void log(String format, Object... args) {
        if (!enabled) {
            return;
        }
        log(String.format(format, args));
    }

    /**
     * Log a WARN level message. Potential issues that don't prevent operation.
     * By default, WARN messages go to both console and file.
     */
    public static void warn(String message) {
        if (!enabled) {
            return;
        }
        logAtLevel(LogLevel.WARN, message);
    }

    /**
     * Log a formatted WARN level message.
     */
    public static void warn(String format, Object... args) {
        if (!enabled) {
            return;
        }
        warn(String.format(format, args));
    }

    /**
     * Log an ERROR level message. Writes to both console (stderr) and file.
     * Error messages are logged even when logging is disabled.
     */
    public static void error(String message) {
        logAtLevel(LogLevel.ERROR, message);
    }

    /**
     * Log a formatted ERROR level message.
     */
    public static void error(String format, Object... args) {
        error(String.format(format, args));
    }

    /**
     * Log an ERROR with exception. Writes to both console (stderr) and file.
     */
    public static void error(String message, Throwable t) {
        ensureInitialized();

        String timestamped = formatMessage("ERROR", message);

        // Errors always go to console
        if (LogLevel.ERROR.isLoggable(consoleLevel)) {
            System.err.println(timestamped);
            t.printStackTrace(System.err);
        }

        // Write to file if level meets threshold
        if (LogLevel.ERROR.isLoggable(fileLevel)) {
            writeToFile(timestamped);
            if (fileWriter != null) {
                synchronized (lock) {
                    t.printStackTrace(fileWriter);
                    fileWriter.flush();
                }
            }
        }
    }

    /**
     * Log a hex dump of bytes at DEBUG level. Useful for debugging serialization issues.
     * By default, hex dumps go to file only, not console.
     */
    public static void hexDump(String label, byte[] bytes, int errorPosition) {
        if (!enabled) {
            return;
        }
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

        // Hex dumps are DEBUG level - respect console level threshold
        if (LogLevel.DEBUG.isLoggable(consoleLevel)) {
            System.err.print(output);
        }

        // Write to file if DEBUG meets file threshold
        if (LogLevel.DEBUG.isLoggable(fileLevel)) {
            writeToFile(formatMessage("DEBUG", "HEXDUMP: " + label));
            writeToFile(output);
        }
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
        String logDirPath = NetworkDebugConfig.getLogDirectory();
        return new File(logDirPath).getAbsolutePath();
    }
}
