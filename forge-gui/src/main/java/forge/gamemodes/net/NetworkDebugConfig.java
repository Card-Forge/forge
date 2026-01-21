package forge.gamemodes.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration reader for network debugging and bandwidth logging.
 * Reads settings from NetworkDebug.config file.
 *
 * Configuration is loaded once at startup and cached.
 * Changes to the config file require restarting Forge.
 */
public final class NetworkDebugConfig {

    private static final String CONFIG_FILE = "NetworkDebug.config";
    private static final Properties config = new Properties();
    private static boolean loaded = false;

    // Default values (used if config file is missing or has errors)
    private static final boolean DEFAULT_BANDWIDTH_LOGGING = true;
    private static final boolean DEFAULT_DEBUG_LOGGER_ENABLED = true;
    private static final String DEFAULT_CONSOLE_LEVEL = "INFO";
    private static final String DEFAULT_FILE_LEVEL = "DEBUG";

    private NetworkDebugConfig() {
        // Utility class
    }

    /**
     * Load configuration from file. Called automatically on first access.
     * If the config file doesn't exist or can't be read, defaults are used.
     */
    private static void loadConfig() {
        if (loaded) {
            return;
        }

        loaded = true;

        // Try multiple locations for the config file
        File configFile = findConfigFile();

        if (configFile == null || !configFile.exists()) {
            System.out.println("[NetworkDebugConfig] Config file not found, using defaults");
            System.out.println("[NetworkDebugConfig]   Searched: " + CONFIG_FILE);
            System.out.println("[NetworkDebugConfig]   Defaults: bandwidth.logging=true, debug.logger=true");
            return;
        }

        try (InputStream input = new FileInputStream(configFile)) {
            config.load(input);
            System.out.println("[NetworkDebugConfig] Loaded configuration from: " + configFile.getAbsolutePath());
            System.out.println("[NetworkDebugConfig]   bandwidth.logging.enabled=" + isBandwidthLoggingEnabled());
            System.out.println("[NetworkDebugConfig]   debug.logger.enabled=" + isDebugLoggerEnabled());
            System.out.println("[NetworkDebugConfig]   debug.logger.console.level=" + getConsoleLogLevel());
            System.out.println("[NetworkDebugConfig]   debug.logger.file.level=" + getFileLogLevel());
        } catch (IOException e) {
            System.err.println("[NetworkDebugConfig] Error reading config file: " + e.getMessage());
            System.err.println("[NetworkDebugConfig] Using default settings");
        }
    }

    /**
     * Find the config file in various possible locations.
     * Tries:
     *   1. Current working directory
     *   2. forge-gui directory (for development)
     *   3. Parent directory (for packaged releases)
     */
    private static File findConfigFile() {
        // Try current directory
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            return file;
        }

        // Try forge-gui directory (development environment)
        file = new File("forge-gui/" + CONFIG_FILE);
        if (file.exists()) {
            return file;
        }

        // Try parent directory (some packaging scenarios)
        file = new File("../" + CONFIG_FILE);
        if (file.exists()) {
            return file;
        }

        return null;
    }

    /**
     * Check if bandwidth logging is enabled.
     * When enabled, delta sync comparison logs are shown and NetworkByteTracker is active.
     *
     * @return true if bandwidth logging should be enabled
     */
    public static boolean isBandwidthLoggingEnabled() {
        loadConfig();
        String value = config.getProperty("bandwidth.logging.enabled", String.valueOf(DEFAULT_BANDWIDTH_LOGGING));
        return Boolean.parseBoolean(value);
    }

    /**
     * Check if the debug logger is enabled.
     * When disabled, no log files are created and no console output is shown.
     *
     * @return true if debug logger should be enabled
     */
    public static boolean isDebugLoggerEnabled() {
        loadConfig();
        String value = config.getProperty("debug.logger.enabled", String.valueOf(DEFAULT_DEBUG_LOGGER_ENABLED));
        return Boolean.parseBoolean(value);
    }

    /**
     * Get the console log level.
     *
     * @return LogLevel enum value (DEBUG, INFO, WARN, ERROR)
     */
    public static NetworkDebugLogger.LogLevel getConsoleLogLevel() {
        loadConfig();
        String value = config.getProperty("debug.logger.console.level", DEFAULT_CONSOLE_LEVEL).toUpperCase();
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
        loadConfig();
        String value = config.getProperty("debug.logger.file.level", DEFAULT_FILE_LEVEL).toUpperCase();
        try {
            return NetworkDebugLogger.LogLevel.valueOf(value);
        } catch (IllegalArgumentException e) {
            System.err.println("[NetworkDebugConfig] Invalid file log level: " + value + ", using DEBUG");
            return NetworkDebugLogger.LogLevel.DEBUG;
        }
    }

    /**
     * Reload configuration from file.
     * Useful for testing or if you want to allow config changes without restart.
     */
    public static void reload() {
        loaded = false;
        config.clear();
        loadConfig();
    }
}
