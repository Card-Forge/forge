package forge.gamemodes.net;

import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgeNetPreferences.FNetPref;
import forge.model.FModel;
import forge.util.IHasForgeLog;
import org.tinylog.ThreadContext;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Configuration and utilities for network debug logging.
 * Manages log file routing, cleanup, and helper functions.
 *
 * <p>Call sites log directly via tinylog's {@code TaggedLogger} with the NETWORK tag,
 * which gives accurate {@code {class-name}} resolution in log output.
 * This class handles the log file routing setup (thread context keys) and
 * provides utility methods like {@link #hexDump}, {@link #sanitizePath}, etc.
 *
 * <p>Log output is routed to both console (INFO+) and per-instance log files (TRACE+)
 * via the NETWORK-tagged writers defined in tinylog.properties.
 * The {@link NetworkLogWriter} routes file output to per-instance files based on
 * the {@code logfileKey} thread context value.
 */
public final class NetworkLogConfig implements IHasForgeLog {

    private static final String LOG_PREFIX = "network-debug";

    // Test mode flag - when true, log filenames include "-test" suffix via ThreadContext
    private static volatile boolean testMode = false;

    // Batch ID for correlating logs from the same test run
    private static volatile String batchId = null;

    // Global instance suffix for single-JVM test mode — used as fallback when
    // a server thread has no ThreadContext values set. This allows server threads to log
    // to the same per-game file as the test harness thread that called setInstanceSuffix().
    private static volatile String globalInstanceSuffix = null;

    // Timestamp key for normal (non-test) mode, set once on first use
    private static volatile String normalModeKey = null;

    // Cached user home path for sanitization
    private static final String USER_HOME = System.getProperty("user.home");

    private NetworkLogConfig() {
        // Utility class
    }

    // --- Domain-specific utilities ---

    /**
     * Log a hex dump of bytes at DEBUG level. Useful for debugging serialization issues.
     */
    public static void hexDump(String label, byte[] bytes, int errorPosition) {
        if (bytes == null || bytes.length == 0) return;

        StringBuilder sb = new StringBuilder();
        sb.append(label).append("\n");

        int safeErrorPosition = Math.max(0, Math.min(errorPosition, bytes.length - 1));
        int start = Math.max(0, safeErrorPosition - 32);
        int end = Math.min(bytes.length, safeErrorPosition + 64);

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

        netLog.debug("HEXDUMP: " + label + "\n" + sb);
    }

    /**
     * Sanitize a file path to remove user-specific information.
     * Replaces the user home directory with "~" for privacy.
     */
    public static String sanitizePath(String path) {
        if (path == null || USER_HOME == null) return path;
        String normalizedHome = USER_HOME.replace('\\', '/');
        String normalizedPath = path.replace('\\', '/');
        if (normalizedPath.startsWith(normalizedHome)) {
            return "~" + normalizedPath.substring(normalizedHome.length());
        }
        return path;
    }

    /**
     * Generate a new session ID for correlating host/client logs.
     * @return the generated session ID (6 hex characters)
     */
    public static String generateSessionId() {
        return String.format("%06x", new java.util.Random().nextInt(0xFFFFFF));
    }

    /**
     * Generate a new batch ID based on current timestamp.
     * @return The generated batch ID in format "runYYYYMMDD-HHMMSS"
     */
    public static String generateBatchId() {
        if (batchId != null) {
            return batchId;
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        batchId = "run" + timestamp;
        updateLogfileKey();
        return batchId;
    }

    /**
     * Get the current batch ID.
     * @return The batch ID, or null if not set
     */
    public static String getBatchId() {
        return batchId;
    }

    /**
     * Set the batch ID for correlating logs from the same test run.
     * @param id The batch ID, or null to clear
     */
    public static void setBatchId(String id) {
        batchId = id;
        updateLogfileKey();
    }

    /**
     * Enable test mode. When enabled, log filenames include "-test" suffix.
     */
    public static void setTestMode(boolean enabled) {
        testMode = enabled;
        updateLogfileKey();
    }

    /**
     * Set the instance suffix for parallel test execution.
     * Each game instance should call this with a unique suffix (e.g., "game0", "game1").
     * @param suffix The instance suffix, or null to clear
     */
    public static void setInstanceSuffix(String suffix) {
        if (suffix != null) {
            ThreadContext.put("instanceSuffix", suffix);
            // In test mode, also set global suffix so server threads within
            // the same JVM inherit it via NetworkLogWriter's fallback
            if (testMode) {
                globalInstanceSuffix = suffix;
            }
            updateLogfileKey();
            cleanupOldLogs();
        } else {
            ThreadContext.remove("instanceSuffix");
            ThreadContext.remove("logfileKey");
        }
    }

    /**
     * Get the current instance suffix for this thread.
     * Falls back to the global suffix in test mode for server threads.
     * @return The instance suffix, or null if not set
     */
    public static String getInstanceSuffix() {
        String suffix = ThreadContext.get("instanceSuffix");
        if (suffix == null && testMode) {
            suffix = globalInstanceSuffix;
        }
        return suffix;
    }

    /**
     * Close the log context for the current thread.
     */
    public static void closeThreadLogger() {
        String suffix = ThreadContext.get("instanceSuffix");
        if (suffix != null && suffix.equals(globalInstanceSuffix)) {
            globalInstanceSuffix = null;
        }
        ThreadContext.clear();
    }

    /**
     * Get the log subdirectory for the current test run.
     * In test mode with a batchId, logs go into a subdirectory named after the batchId.
     * In normal mode, logs go directly into the network logs directory.
     * @return The directory where log files should be written
     */
    public static File getLogDirectory() {
        File baseDir = new File(ForgeConstants.NETWORK_LOGS_DIR);
        if (testMode && batchId != null) {
            return new File(baseDir, batchId);
        }
        return baseDir;
    }

    /**
     * Get the current log file for the active context.
     * @return The current log file, or null if logging not initialized
     */
    public static File getCurrentLogFile() {
        File logDir = getLogDirectory();
        if (!logDir.exists()) return null;

        // Must match the NetworkLogWriter file naming pattern
        String logfileKey = computeLogfileKey();
        String filename = LOG_PREFIX + "-" + logfileKey + ".log";

        File logFile = new File(logDir, filename);
        return logFile.exists() ? logFile : null;
    }

    /**
     * Get the global logfile key for threads that have no ThreadContext set.
     * Used by {@link NetworkLogWriter} as a fallback.
     * @return the computed logfile key, or null if no routing is configured
     */
    static String getGlobalLogfileKey() {
        // In test mode, use the batch/instance machinery
        if (testMode && (batchId != null || globalInstanceSuffix != null)) {
            return computeLogfileKey();
        }
        // In normal mode, use timestamp-based key
        if (normalModeKey == null) {
            normalModeKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        }
        return normalModeKey;
    }

    /**
     * Compute the composite logfile key from instance suffix and test mode.
     * In test mode with a batchId, the batchId is used as the subdirectory name
     * rather than being part of the filename.
     * Uses global fallback for instance suffix in test mode.
     * Must match the key read by {@link NetworkLogWriter}.
     */
    private static String computeLogfileKey() {
        if (!testMode) {
            // Normal mode: timestamp-based key
            if (normalModeKey == null) {
                normalModeKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            }
            return normalModeKey;
        }
        // Test mode: instance suffix + test marker
        // The batchId is now the subdirectory name, not part of the filename
        String suffix = ThreadContext.get("instanceSuffix");
        if (suffix == null) {
            suffix = globalInstanceSuffix;
        }
        String instancePart = suffix != null ? suffix : "default";
        return instancePart + "-test";
    }

    /**
     * Update the logfileKey thread context value used by the NetworkLogWriter.
     */
    private static void updateLogfileKey() {
        ThreadContext.put("logfileKey", computeLogfileKey());
    }

    /**
     * Delete old log subdirectories from the network logs directory, respecting:
     * - Grace period: skip directories modified within the last 5 minutes
     * - Current batch protection: skip the current batchId subdirectory
     * - Max directory limit from NET_MAX_LOG_FILES preference
     * - Cleanup can be disabled via NET_LOG_CLEANUP_ENABLED preference
     * - Also cleans up legacy flat log files (network-debug-*.log)
     */
    private static void cleanupOldLogs() {
        try {
            if (FModel.getNetPreferences() == null) {
                return;
            }
            if (!FModel.getNetPreferences().getPrefBoolean(FNetPref.NET_LOG_CLEANUP_ENABLED)) {
                return;
            }
            int maxEntries = FModel.getNetPreferences().getPrefInt(FNetPref.NET_MAX_LOG_FILES);
            if (maxEntries <= 0) {
                return;
            }

            File logDir = new File(ForgeConstants.NETWORK_LOGS_DIR);
            if (!logDir.exists() || !logDir.isDirectory()) {
                return;
            }

            // Collect log subdirectories (run* pattern) and legacy flat log files
            File[] entries = logDir.listFiles(f ->
                    (f.isDirectory() && f.getName().startsWith("run")) ||
                    (f.isFile() && f.getName().startsWith(LOG_PREFIX) && f.getName().endsWith(".log")));
            if (entries == null || entries.length <= maxEntries) {
                return;
            }

            // Sort oldest first
            java.util.Arrays.sort(entries, java.util.Comparator.comparingLong(File::lastModified));

            long gracePeriodMs = 5 * 60 * 1000L;
            long now = System.currentTimeMillis();
            String currentBatch = batchId;
            int deleted = 0;

            for (int i = 0; i < entries.length - maxEntries; i++) {
                File f = entries[i];
                // Grace period: skip recently modified entries
                if (now - f.lastModified() < gracePeriodMs) {
                    continue;
                }
                // Batch protection: skip current batch directory
                if (currentBatch != null && f.getName().equals(currentBatch)) {
                    continue;
                }
                if (f.isDirectory()) {
                    deleteDirectory(f);
                    deleted++;
                } else if (f.delete()) {
                    deleted++;
                }
            }

            if (deleted > 0) {
                netLog.debug("Log cleanup: deleted " + deleted + " old log entries");
            }
        } catch (Exception e) {
            // Non-critical — don't let cleanup failures affect logging
        }
    }

    /**
     * Recursively delete a directory and its contents.
     */
    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }
}
