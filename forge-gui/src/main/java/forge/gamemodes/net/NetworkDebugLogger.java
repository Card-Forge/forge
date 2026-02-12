package forge.gamemodes.net;

import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.time.format.DateTimeFormatter;

/**
 * Debug logger for network/delta sync operations.
 * Delegates to SLF4J/Logback for file management, formatting, and concurrency.
 *
 * Log output is routed to both console (INFO+) and per-instance log files (DEBUG+)
 * via the {@code forge.gamemodes.net} logger defined in logback.xml.
 *
 * Supports configurable verbosity levels for console vs file output:
 * - TRACE: Per-object/per-property detail (serialization, collection stats, tracker verification)
 * - DEBUG: Diagnostic events (state mismatches, creation tracking, summary stats)
 * - INFO: Normal operation (sync start/end, summaries, game events)
 * - WARN: Potential issues (missing objects, unexpected states)
 * - ERROR: Failures and exceptions
 *
 * Default file level is DEBUG; TRACE is only enabled when explicitly configured.
 */
public final class NetworkDebugLogger {

    // Set log directory as system property BEFORE logback initializes (triggered by getLogger below).
    // This allows logback.xml to reference ${forge.networklogs.dir} for the file path.
    static {
        System.setProperty("forge.networklogs.dir", ForgeConstants.NETWORK_LOGS_DIR);
    }

    private static final Logger logger = LoggerFactory.getLogger("forge.gamemodes.net");

    private static final String LOG_PREFIX = "network-debug";

    // Test mode flag - when true, log filenames include "-test" suffix via MDC
    private static volatile boolean testMode = false;

    // Batch ID for correlating logs from the same test run
    private static volatile String batchId = null;

    // Whether the logger is enabled (controlled via ForgePreferences)
    private static volatile boolean enabled = true;

    // Global instance suffix for single-JVM test mode — used as fallback when
    // a server thread has no MDC values set. This allows server threads to log
    // to the same per-game file as the test harness thread that called setInstanceSuffix().
    private static volatile String globalInstanceSuffix = null;

    // Track whether system info has been logged for this instance
    private static final ThreadLocal<Boolean> systemInfoLogged = new ThreadLocal<>();

    // Cached user home path for sanitization (computed once)
    private static final String USER_HOME = System.getProperty("user.home");

    private NetworkDebugLogger() {
        // Utility class
    }

    /**
     * Apply configuration from ForgePreferences.
     * Called to reload configuration at runtime.
     */
    public static void applyConfig() {
        try {
            if (FModel.getPreferences() == null) {
                return;
            }
            enabled = FModel.getPreferences().getPrefBoolean(FPref.NET_DEBUG_LOGGER_ENABLED);

            // Apply log level programmatically via reflection so we don't need
            // a compile-time dependency on logback-classic (avoids pulling it
            // into Android builds where it's unused).
            String fileLevel = FModel.getPreferences().getPref(FPref.NET_FILE_LOG_LEVEL).toUpperCase();
            Object factory = LoggerFactory.getILoggerFactory();
            java.lang.reflect.Method getLogger = factory.getClass().getMethod("getLogger", String.class);
            Object netLogger = getLogger.invoke(factory, "forge.gamemodes.net");
            Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
            java.lang.reflect.Method toLevel = levelClass.getMethod("toLevel", String.class, (Class<?>) levelClass);
            Object defaultLevel = levelClass.getField("TRACE").get(null);
            Object level = toLevel.invoke(null, fileLevel, defaultLevel);
            netLogger.getClass().getMethod("setLevel", levelClass).invoke(netLogger, level);
        } catch (Exception e) {
            // Logback not available (e.g. Android) or preferences not initialized — use defaults
        }
    }

    // --- Facade methods (public API unchanged) ---

    /**
     * Log an INFO level message. Normal operation information.
     */
    public static void log(String message) {
        if (!enabled) return;
        ensureMDC();
        logger.info(message);
    }

    /**
     * Log a formatted INFO level message.
     */
    public static void log(String format, Object... args) {
        if (!enabled) return;
        ensureMDC();
        logger.info(String.format(format, args));
    }

    /**
     * Log a DEBUG level message. Detailed tracing information.
     */
    public static void debug(String message) {
        if (!enabled || !logger.isDebugEnabled()) return;
        ensureMDC();
        logger.debug(message);
    }

    /**
     * Log a formatted DEBUG level message.
     */
    public static void debug(String format, Object... args) {
        if (!enabled || !logger.isDebugEnabled()) return;
        ensureMDC();
        logger.debug(String.format(format, args));
    }

    /**
     * Log a TRACE level message. Per-object/per-property detail.
     */
    public static void trace(String message) {
        if (!enabled || !logger.isTraceEnabled()) return;
        ensureMDC();
        logger.trace(message);
    }

    /**
     * Log a formatted TRACE level message.
     */
    public static void trace(String format, Object... args) {
        if (!enabled || !logger.isTraceEnabled()) return;
        ensureMDC();
        logger.trace(String.format(format, args));
    }

    /**
     * Log a WARN level message. Potential issues that don't prevent operation.
     */
    public static void warn(String message) {
        if (!enabled) return;
        ensureMDC();
        logger.warn(message);
    }

    /**
     * Log a formatted WARN level message.
     */
    public static void warn(String format, Object... args) {
        if (!enabled) return;
        ensureMDC();
        logger.warn(String.format(format, args));
    }

    /**
     * Log an ERROR level message.
     * Error messages are logged even when logging is disabled.
     */
    public static void error(String message) {
        ensureMDC();
        logger.error(message);
    }

    /**
     * Log a formatted ERROR level message.
     */
    public static void error(String format, Object... args) {
        ensureMDC();
        logger.error(String.format(format, args));
    }

    /**
     * Log an ERROR with exception.
     */
    public static void error(String message, Throwable t) {
        ensureMDC();
        logger.error(message, t);
    }

    // --- Domain-specific utilities (no SLF4J equivalent) ---

    /**
     * Log a hex dump of bytes at DEBUG level. Useful for debugging serialization issues.
     */
    public static void hexDump(String label, byte[] bytes, int errorPosition) {
        if (!enabled || !logger.isDebugEnabled()) return;
        if (bytes == null || bytes.length == 0) return;
        ensureMDC();

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

        logger.debug("HEXDUMP: {}\n{}", label, sb);
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
        String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
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
            MDC.put("instanceSuffix", suffix);
            // In test mode, also set global suffix so server threads within
            // the same JVM inherit it via ensureMDC()
            if (testMode) {
                globalInstanceSuffix = suffix;
            }
            updateLogfileKey();
            // Log system info header on first suffix set for this thread
            if (systemInfoLogged.get() == null) {
                logSystemInfo();
                systemInfoLogged.set(Boolean.TRUE);
            }
        } else {
            MDC.remove("instanceSuffix");
            MDC.remove("logfileKey");
        }
    }

    /**
     * Get the current instance suffix for this thread.
     * Falls back to the global suffix in test mode for server threads.
     * @return The instance suffix, or null if not set
     */
    public static String getInstanceSuffix() {
        String suffix = MDC.get("instanceSuffix");
        if (suffix == null && testMode) {
            suffix = globalInstanceSuffix;
        }
        return suffix;
    }

    /**
     * Close the log context for the current thread.
     */
    public static void closeThreadLogger() {
        String suffix = MDC.get("instanceSuffix");
        if (suffix != null && suffix.equals(globalInstanceSuffix)) {
            globalInstanceSuffix = null;
        }
        systemInfoLogged.remove();
        MDC.clear();
    }

    /**
     * Get the current log file for the active context.
     * @return The current log file, or null if logging not initialized
     */
    public static File getCurrentLogFile() {
        String logDirPath = ForgeConstants.NETWORK_LOGS_DIR;
        File logDir = new File(logDirPath);
        if (!logDir.exists()) return null;

        // Must match the SiftingAppender file pattern in logback.xml
        String logfileKey = computeLogfileKey();
        String filename = LOG_PREFIX + "-" + logfileKey + ".log";

        File logFile = new File(logDir, filename);
        return logFile.exists() ? logFile : null;
    }

    /**
     * Compute the composite logfile key from batch ID, instance suffix, and test mode.
     * Uses global fallback for instance suffix in test mode.
     * Must match the SiftingAppender discriminator key in logback.xml.
     */
    private static String computeLogfileKey() {
        String batchPart = batchId != null ? batchId : "nobatch";
        String suffix = MDC.get("instanceSuffix");
        if (suffix == null && testMode) {
            suffix = globalInstanceSuffix;
        }
        String instancePart = suffix != null ? suffix : "default";
        String testPart = testMode ? "-test" : "";
        return batchPart + "-" + instancePart + testPart;
    }

    /**
     * Update the logfileKey MDC value used by the SiftingAppender discriminator.
     */
    private static void updateLogfileKey() {
        MDC.put("logfileKey", computeLogfileKey());
    }

    /**
     * Ensure MDC is populated on the current thread. In test mode, server threads
     * that never called setInstanceSuffix() inherit the global suffix so their
     * log output goes to the correct per-game log file.
     */
    private static void ensureMDC() {
        if (testMode && MDC.get("logfileKey") == null && globalInstanceSuffix != null) {
            MDC.put("instanceSuffix", globalInstanceSuffix);
            MDC.put("logfileKey", computeLogfileKey());
        }
    }

    /**
     * Log system information header as a DEBUG message.
     */
    private static void logSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        long pid = ProcessHandle.current().pid();
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(80)).append("\n");
        sb.append("Network Debug Log Started\n");
        if (batchId != null) {
            sb.append("Batch ID: ").append(batchId).append("\n");
        }
        sb.append("PID: ").append(pid).append("\n");
        String suffix = MDC.get("instanceSuffix");
        if (suffix != null) {
            sb.append("Instance: ").append(suffix).append("\n");
        }
        sb.append("\nSystem Information:\n");
        sb.append("  Java Version: ").append(System.getProperty("java.version")).append("\n");
        sb.append("  Java Vendor: ").append(System.getProperty("java.vendor")).append("\n");
        sb.append("  Max Memory: ").append(runtime.maxMemory() / 1024 / 1024).append(" MB\n");
        sb.append("  Available Processors: ").append(runtime.availableProcessors()).append("\n");
        sb.append("  OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append("\n");
        sb.append("  OS Arch: ").append(System.getProperty("os.arch")).append("\n");
        sb.append("=".repeat(80));
        logger.debug(sb.toString());
    }
}
