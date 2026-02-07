package forge.gamemodes.net;

/**
 * Configuration reader for network debugging and bandwidth logging.
 * Uses PreferencesStore pattern for consistency with other Forge preferences.
 *
 * Settings are stored in ~/.forge/preferences/network.preferences (Linux)
 * or %APPDATA%\Forge\preferences\network.preferences (Windows).
 *
 * Changes to the preferences file require restarting Forge.
 */
public final class NetworkDebugConfig {

    private static NetworkDebugPreferences prefs;

    private NetworkDebugConfig() {
        // Utility class
    }

    /**
     * Get or create the preferences instance.
     */
    private static NetworkDebugPreferences getPrefs() {
        if (prefs == null) {
            prefs = new NetworkDebugPreferences();
        }
        return prefs;
    }

    /**
     * Check if bandwidth logging is enabled.
     * When enabled, delta sync comparison logs are shown and NetworkByteTracker is active.
     *
     * @return true if bandwidth logging should be enabled
     */
    public static boolean isBandwidthLoggingEnabled() {
        return getPrefs().getPrefBoolean(NetworkDebugPreferences.NDPref.BANDWIDTH_LOGGING_ENABLED);
    }

    /**
     * Check if the debug logger is enabled.
     * When disabled, no log files are created and no console output is shown.
     *
     * @return true if debug logger should be enabled
     */
    public static boolean isDebugLoggerEnabled() {
        return getPrefs().getPrefBoolean(NetworkDebugPreferences.NDPref.DEBUG_LOGGER_ENABLED);
    }

    /**
     * Get the console log level.
     *
     * @return LogLevel enum value (DEBUG, INFO, WARN, ERROR)
     */
    public static NetworkDebugLogger.LogLevel getConsoleLogLevel() {
        String value = getPrefs().getPref(NetworkDebugPreferences.NDPref.CONSOLE_LOG_LEVEL).toUpperCase();
        try {
            return NetworkDebugLogger.LogLevel.valueOf(value);
        } catch (IllegalArgumentException e) {
            System.err.println("[NetworkDebugConfig] Invalid console log level: " + value + ", using INFO");
            return NetworkDebugLogger.LogLevel.INFO;
        }
    }

    /**
     * Get the file log level.
     *
     * @return LogLevel enum value (DEBUG, INFO, WARN, ERROR)
     */
    public static NetworkDebugLogger.LogLevel getFileLogLevel() {
        String value = getPrefs().getPref(NetworkDebugPreferences.NDPref.FILE_LOG_LEVEL).toUpperCase();
        try {
            return NetworkDebugLogger.LogLevel.valueOf(value);
        } catch (IllegalArgumentException e) {
            System.err.println("[NetworkDebugConfig] Invalid file log level: " + value + ", using DEBUG");
            return NetworkDebugLogger.LogLevel.DEBUG;
        }
    }

    /**
     * Get the maximum number of log files to retain.
     * When log cleanup is enabled, oldest log files are deleted when this limit is exceeded.
     * Set to 0 to disable limit (keep all logs).
     *
     * @return maximum number of log files, or 0 for no limit
     */
    public static int getMaxLogFiles() {
        int max = getPrefs().getPrefInt(NetworkDebugPreferences.NDPref.MAX_LOG_FILES);
        return Math.max(0, max); // Ensure non-negative
    }

    /**
     * Check if automatic log cleanup is enabled.
     * When enabled, old log files are deleted when the max log limit is exceeded.
     *
     * @return true if log cleanup should be performed
     */
    public static boolean isLogCleanupEnabled() {
        return getPrefs().getPrefBoolean(NetworkDebugPreferences.NDPref.LOG_CLEANUP_ENABLED);
    }

}
