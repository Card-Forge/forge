package forge.interfaces;

import org.apache.commons.lang3.tuple.Pair;
import org.jupnp.UpnpServiceConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.function.BiFunction;

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

    /**
     * Decodes a WebP image, which libGDX cannot read, into the buffer {@code rgba} returns for its width and
     * height: four bytes per pixel in R, G, B, A order, rows from the top.
     */
    void decodeWebp(File file, BiFunction<Integer, Integer, ByteBuffer> rgba) throws IOException;

    /** Confine the mouse cursor to the window (desktop LibGDX only). Default no-op. */
    default void setMouseConfined(boolean confined) {
    }

    Set<String> LWJGL_SUPPORTED_AUDIO_TYPES = Set.of(".wav", ".mp3", ".ogg");
    default boolean isSupportedAudioFormat(File file) {
        String path = file.getPath().toLowerCase();
        return LWJGL_SUPPORTED_AUDIO_TYPES.stream().anyMatch(path::endsWith);
    }
}
