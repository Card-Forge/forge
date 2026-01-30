package forge.gamemodes.net;

import forge.localinstance.properties.ForgeConstants;

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

    // Per-instance log file support for parallel test execution
    // When instanceSuffix is set, all threads with that suffix share a log file
    // Otherwise, all threads share a single default log file
    private static PrintWriter sharedFileWriter;
    private static final java.util.concurrent.ConcurrentHashMap<String, PrintWriter> suffixWriters = new java.util.concurrent.ConcurrentHashMap<>();
    private static boolean sharedInitialized = false;
    private static boolean enabled = true;
    private static LogLevel consoleLevel = LogLevel.INFO;  // Default: show INFO and above on console
    private static LogLevel fileLevel = LogLevel.DEBUG;    // Default: show DEBUG and above in file
    private static final Object lock = new Object();

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    // Session identifier for correlating host/client logs
    private static String sessionId = null;

    // Test mode flag - when true, log filenames include "-test" suffix
    private static boolean testMode = false;

    // Batch ID for correlating logs from the same test run
    // Generated once by the parent process and passed to all child processes
    private static String batchId = null;

    // Instance suffix for parallel test execution - allows each game instance to have its own log file
    // This is stored per-thread so parallel games don't interfere with each other's logging
    private static final ThreadLocal<String> instanceSuffix = new ThreadLocal<>();

    // Global instance suffix for single-JVM test mode - used as fallback when thread has no suffix
    // This allows server threads to log to the same file as the test harness thread
    private static volatile String globalInstanceSuffix = null;

    // Apply configuration from NetworkDebug.config on class load
    static {
        applyConfig();
    }

    private NetworkDebugLogger() {
        // Utility class
    }

    // Cached user home path for sanitization (computed once)
    private static final String USER_HOME = System.getProperty("user.home");

    /**
     * Sanitize a file path to remove user-specific information.
     * Replaces the user home directory with "~" for privacy.
     *
     * @param path The path to sanitize
     * @return The sanitized path with user home replaced by "~"
     */
    private static String sanitizePath(String path) {
        if (path == null || USER_HOME == null) {
            return path;
        }
        // Replace user home with ~ (works for both Windows and Unix)
        // Use File.separator-aware replacement
        String normalizedHome = USER_HOME.replace('\\', '/');
        String normalizedPath = path.replace('\\', '/');
        if (normalizedPath.startsWith(normalizedHome)) {
            return "~" + normalizedPath.substring(normalizedHome.length());
        }
        return path;
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
     * Enable test mode. When enabled, log filenames include "-test" suffix
     * to distinguish test-generated logs from production logs.
     * Must be called BEFORE first log() call to affect filename.
     */
    public static void setTestMode(boolean enabled) {
        testMode = enabled;
    }

    /**
     * Check if test mode is enabled.
     */
    public static boolean isTestMode() {
        return testMode;
    }

    /**
     * Set the batch ID for correlating logs from the same test run.
     * The batch ID is generated once by the parent process and passed to all
     * child processes, ensuring all logs from the same batch share this ID.
     *
     * Format recommendation: "runYYYYMMDD-HHMMSS" (e.g., "run20260125-091914")
     *
     * Must be called BEFORE first log() call to affect filename.
     *
     * @param id The batch ID, or null to clear
     */
    public static void setBatchId(String id) {
        batchId = id;
    }

    /**
     * Get the current batch ID.
     * @return The batch ID, or null if not set
     */
    public static String getBatchId() {
        return batchId;
    }

    /**
     * Generate a new batch ID based on current timestamp.
     * Use this in the parent process before spawning child processes.
     *
     * @return The generated batch ID in format "runYYYYMMDD-HHMMSS"
     */
    public static String generateBatchId() {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        batchId = "run" + timestamp;
        return batchId;
    }

    /**
     * Set the instance suffix for parallel test execution.
     * Each game instance should call this with a unique suffix (e.g., "game0", "game1")
     * before logging starts. When set, the log filename will include this suffix.
     *
     * Must be called BEFORE first log() call on this thread to affect filename.
     *
     * In single-JVM test mode, also sets the global suffix so server threads
     * (which run on different threads) can log to the same file.
     *
     * @param suffix The instance suffix (e.g., "game0"), or null to clear
     */
    public static void setInstanceSuffix(String suffix) {
        instanceSuffix.set(suffix);
        // In test mode, also set global suffix for server threads within same JVM
        if (testMode) {
            globalInstanceSuffix = suffix;
        }
    }

    /**
     * Get the current instance suffix for this thread.
     * Falls back to global suffix if thread-local is not set (for server threads in test mode).
     *
     * @return The instance suffix, or null if not set
     */
    public static String getInstanceSuffix() {
        String suffix = instanceSuffix.get();
        if (suffix == null && testMode) {
            // Fallback to global suffix for server threads in test mode
            suffix = globalInstanceSuffix;
        }
        return suffix;
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
     * Generate a new session ID for correlating host/client logs.
     * Should be called by the host when starting a session, and the ID
     * should be shared with clients.
     * @return the generated session ID (6 hex characters)
     */
    public static String generateSessionId() {
        // Generate a short random hex string for easy identification
        sessionId = String.format("%06x", new java.util.Random().nextInt(0xFFFFFF));
        return sessionId;
    }

    /**
     * Set the session ID (used by clients receiving the ID from host).
     * @param id the session ID to set
     */
    public static void setSessionId(String id) {
        sessionId = id;
    }

    /**
     * Get the current session ID, or null if not set.
     * @return the session ID
     */
    public static String getSessionId() {
        return sessionId;
    }

    /**
     * Get list of existing log files in the log directory.
     * Returns files sorted by last modified time (oldest first).
     */
    private static List<File> getExistingLogFiles() {
        String logDirPath = ForgeConstants.NETWORK_LOGS_DIR;
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
     * Never deletes logs from the current batch run (when batch ID is set).
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
        String currentBatchId = batchId;  // Capture current batch ID

        // Delete oldest files, but respect grace period and current batch
        int deleted = 0;
        for (int i = 0; i < logFiles.size() && deleted < toDelete; i++) {
            File oldLog = logFiles.get(i);

            // Skip files modified within grace period (likely from other running instances)
            long age = now - oldLog.lastModified();
            if (age < GRACE_PERIOD_MS) {
                continue;
            }

            // Skip files from current batch run - never delete logs from the current batch
            if (currentBatchId != null && oldLog.getName().contains(currentBatchId)) {
                continue;
            }

            try {
                if (oldLog.delete()) {
                    deleted++;
                }
                // Silent operation - no console output
            } catch (SecurityException e) {
                // Silent failure - continue with other deletions
            }
        }
    }

    /**
     * Initialize the logger if not already done.
     * Creates the log directory and opens a unique log file.
     *
     * If instanceSuffix is set, uses/creates a log file for that suffix.
     * All threads with the same suffix share the same log file.
     * Otherwise, creates/uses a shared default log file.
     */
    private static void ensureInitialized() {
        String suffix = getInstanceSuffix();

        // If instance suffix is set, use suffix-specific log file
        if (suffix != null) {
            if (suffixWriters.containsKey(suffix)) {
                return;  // Already initialized for this suffix
            }
            initializeSuffixLogger(suffix);
            return;
        }

        // Otherwise, use shared log file
        if (sharedInitialized) {
            return;
        }
        synchronized (lock) {
            if (sharedInitialized) {
                return;
            }
            sharedFileWriter = initializeLogFile(null);
            sharedInitialized = true;
        }
    }

    /**
     * Initialize a log file for a specific instance suffix.
     * Uses ConcurrentHashMap for thread-safe lazy initialization.
     */
    private static void initializeSuffixLogger(String suffix) {
        suffixWriters.computeIfAbsent(suffix, s -> initializeLogFile(s));
    }

    /**
     * Create and initialize a log file with optional instance suffix.
     *
     * @param suffix Instance suffix (e.g., "game0"), or null for shared log
     * @return PrintWriter for the log file, or null on failure
     */
    private static PrintWriter initializeLogFile(String suffix) {
        try {
            // Get configurable log directory
            String logDirPath = ForgeConstants.NETWORK_LOGS_DIR;
            File logDir = new File(logDirPath);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            // Clean up old logs before creating new one (only for shared log)
            if (suffix == null) {
                cleanupOldLogs();
            }

            // Create unique filename
            // When batch ID is set, use it for grouping logs from the same test run
            // Otherwise, use timestamp and PID for uniqueness
            // Include "-test" suffix when running from test infrastructure
            // Include instance suffix for parallel test execution
            String testSuffix = testMode ? "-test" : "";
            String instancePart = (suffix != null) ? "-" + suffix : "";
            String filename;
            if (batchId != null) {
                // Batch mode: use batch ID for grouping, instance suffix for uniqueness
                filename = String.format("%s-%s%s%s.log", LOG_PREFIX, batchId, instancePart, testSuffix);
            } else {
                // Legacy mode: use timestamp and PID for uniqueness
                String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                long pid = ProcessHandle.current().pid();
                filename = String.format("%s-%s-%d%s%s.log", LOG_PREFIX, timestamp, pid, instancePart, testSuffix);
            }
            File logFile = new File(logDir, filename);

            PrintWriter writer = new PrintWriter(new FileWriter(logFile, true), true);

            // Log header with system information
            Runtime runtime = Runtime.getRuntime();
            long pid = ProcessHandle.current().pid();
            writer.println("=".repeat(80));
            writer.println("Network Debug Log Started: " + new Date());
            if (batchId != null) {
                writer.println("Batch ID: " + batchId);
            }
            writer.println("PID: " + pid);
            if (suffix != null) {
                writer.println("Instance: " + suffix);
            }
            writer.println("Log file: " + sanitizePath(logFile.getAbsolutePath()));
            writer.println();
            writer.println("System Information:");
            writer.println("  Java Version: " + System.getProperty("java.version"));
            writer.println("  Java Vendor: " + System.getProperty("java.vendor"));
            writer.println("  Max Memory: " + (runtime.maxMemory() / 1024 / 1024) + " MB");
            writer.println("  Available Processors: " + runtime.availableProcessors());
            writer.println("  OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            writer.println("  OS Arch: " + System.getProperty("os.arch"));
            writer.println("=".repeat(80));
            writer.println();

            System.out.println("[NetworkDebugLogger] Logging to: " + sanitizePath(logFile.getAbsolutePath()));

            return writer;
        } catch (IOException e) {
            System.err.println("[NetworkDebugLogger] Failed to initialize log file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the active file writer for the current context.
     * Returns suffix-specific writer if instance suffix is set, otherwise shared writer.
     */
    private static PrintWriter getFileWriter() {
        String suffix = getInstanceSuffix();
        if (suffix != null) {
            return suffixWriters.get(suffix);
        }
        return sharedFileWriter;
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
            PrintWriter writer = getFileWriter();
            if (writer != null) {
                synchronized (lock) {
                    t.printStackTrace(writer);
                    writer.flush();
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
        PrintWriter writer = getFileWriter();
        if (writer != null) {
            synchronized (lock) {
                writer.println(message);
                writer.flush();
            }
        }
    }

    /**
     * Close the log file. Call on application shutdown.
     * Closes all suffix-specific writers and the shared writer.
     */
    public static void close() {
        synchronized (lock) {
            // Close all suffix-specific writers
            for (PrintWriter writer : suffixWriters.values()) {
                if (writer != null) {
                    writer.println();
                    writer.println("=".repeat(80));
                    writer.println("Network Debug Log Ended: " + new Date());
                    writer.println("=".repeat(80));
                    writer.close();
                }
            }
            suffixWriters.clear();
            instanceSuffix.remove();
            globalInstanceSuffix = null;
            batchId = null;

            // Close shared writer
            if (sharedFileWriter != null) {
                sharedFileWriter.println();
                sharedFileWriter.println("=".repeat(80));
                sharedFileWriter.println("Network Debug Log Ended: " + new Date());
                sharedFileWriter.println("=".repeat(80));
                sharedFileWriter.close();
                sharedFileWriter = null;
                sharedInitialized = false;
            }
        }
    }

    /**
     * Close the log file for the current instance suffix.
     * Use this when a game instance completes.
     */
    public static void closeThreadLogger() {
        synchronized (lock) {
            String suffix = instanceSuffix.get();
            if (suffix != null) {
                PrintWriter writer = suffixWriters.remove(suffix);
                if (writer != null) {
                    writer.println();
                    writer.println("=".repeat(80));
                    writer.println("Network Debug Log Ended: " + new Date());
                    writer.println("=".repeat(80));
                    writer.close();
                }
                instanceSuffix.remove();
                // Clear global suffix if it matches
                if (suffix.equals(globalInstanceSuffix)) {
                    globalInstanceSuffix = null;
                }
            }
        }
    }

    /**
     * Get the path to the current log file, or null if not initialized.
     */
    public static String getLogFilePath() {
        ensureInitialized();
        // We don't store the path, so just return the directory
        String logDirPath = ForgeConstants.NETWORK_LOGS_DIR;
        return new File(logDirPath).getAbsolutePath();
    }
}
