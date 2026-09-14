package forge.ios;

import org.robovm.rt.bro.Bro;
import org.robovm.rt.bro.annotation.Bridge;
import org.robovm.rt.bro.annotation.Library;

/**
 * Native os_log wrapper for iOS 26+ compatibility.
 *
 * iOS 26 marks all NSLog/Foundation.log output as <private>.
 * This wrapper uses os_log with %{public}s to ensure logs are visible.
 */
@Library(Library.INTERNAL)
public class ForgeOSLog {

    static {
        Bro.bind(ForgeOSLog.class);
    }

    /**
     * Log a message at default level with public visibility.
     */
    @Bridge(symbol = "forge_os_log_public")
    public static native void logPublic(String message);

    /**
     * Log a message at info level with public visibility.
     */
    @Bridge(symbol = "forge_os_log_info")
    public static native void logInfo(String message);

    /**
     * Log a message at debug level with public visibility.
     */
    @Bridge(symbol = "forge_os_log_debug")
    public static native void logDebug(String message);

    /**
     * Log a message at error level with public visibility.
     */
    @Bridge(symbol = "forge_os_log_error")
    public static native void logError(String message);

    /**
     * Convenience method to log with FORGE: prefix.
     */
    public static void log(String message) {
        logPublic("FORGE: " + message);
    }
}
