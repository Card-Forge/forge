package forge.adventure;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Clipboard;
import forge.Forge;
import forge.adventure.util.Config;
import forge.util.BuildInfo;
import io.sentry.Sentry;
import io.sentry.SentryClient;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Main entry point
 */
public class Main {

    public static void main(String[] args) {

        Sentry.init();
        SentryClient sentryClient = Sentry.getStoredClient();
        sentryClient.setRelease(BuildInfo.getVersionString());
        sentryClient.setEnvironment(System.getProperty("os.name"));
        sentryClient.addTag("Java Version", System.getProperty("java.version"));

        // HACK - temporary solution to "Comparison method violates it's general contract!" crash
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        //Turn off the Java 2D system's use of Direct3D to improve rendering speed (particularly when Full Screen)
        System.setProperty("sun.java2d.d3d", "false");


        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setResizable(false);

        //todo icon config && fullscreen mode
        config.setWindowedMode(1280, 720);

        new Lwjgl3Application(Forge.getApp(new Lwjgl3Clipboard(), new DesktopAdapter(""), Files.exists(Paths.get("./res"))?"./":"../forge-gui/", true, false, 0, true, 0, "", "", true), config);

    }
}
