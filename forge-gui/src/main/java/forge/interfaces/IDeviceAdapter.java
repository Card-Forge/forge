package forge.interfaces;

import org.apache.commons.lang3.tuple.Pair;
import org.jupnp.UpnpServiceConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public interface IDeviceAdapter {
    boolean isConnectedToInternet();
    boolean isConnectedToWifi();
    boolean isTablet();
    String getDownloadsDir();
    String getVersionString();
    String getLatestChanges(String commitsAtom, Date buildDateOriginal, Date maxDate);
    String getReleaseTag(String releaseAtom);
    boolean openFile(String filename);
    void setLandscapeMode(boolean landscapeMode);
    void preventSystemSleep(boolean preventSleep);
    void restart();
    void exit();
    void closeSplashScreen();
    void convertToJPEG(InputStream input, OutputStream output) throws IOException;
    void convertToPNG(InputStream input, OutputStream output) throws IOException;
    Pair<Integer, Integer> getRealScreenSize(boolean real);
    ArrayList<String> getGamepads();
    UpnpServiceConfiguration getUpnpPlatformService();
    boolean needFileAccess();
    void requestFileAcces();

    /** Confine the mouse cursor to the window (desktop LibGDX only). Default no-op. */
    default void setMouseConfined(boolean confined) {
    }

    /** Whether Forge runs in a desktop window it can resize, maximize and make fullscreen (desktop LibGDX only). */
    default boolean hasWindowControls() {
        return false;
    }

    /** Whether the window covers its monitor (borderless fullscreen). */
    default boolean isFullscreen() {
        return false;
    }

    /** Switches between borderless fullscreen on the window's monitor and the window it was before. Default no-op. */
    default void setFullscreen(boolean fullscreen) {
    }

    /** Leaves fullscreen and maximized state and resizes the window to this size, centred on its monitor. Default no-op. */
    default void setWindowSize(int width, int height) {
    }

    /** Lets the user resize the window or not (desktop). Default no-op. */
    default void setWindowResizable(boolean resizable) {
    }

    /** The smallest size the window may have, {width, height} (desktop), or null when there is no window to speak of. */
    default int[] getMinimumWindowSize() {
        return null;
    }

    /** The size of the monitor the window is on (desktop), or null when there is no window to speak of. */
    default int[] getMonitorSize() {
        return null;
    }

    Set<String> LWJGL_SUPPORTED_AUDIO_TYPES = Set.of(".wav", ".mp3", ".ogg");
    default boolean isSupportedAudioFormat(File file) {
        String path = file.getPath().toLowerCase();
        return LWJGL_SUPPORTED_AUDIO_TYPES.stream().anyMatch(path::endsWith);
    }
}
