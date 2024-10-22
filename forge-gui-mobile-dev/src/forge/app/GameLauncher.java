package forge.app;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Clipboard;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import forge.Forge;
import forge.adventure.util.Config;
import forge.assets.AssetsDownloader;
import forge.util.FileUtil;
import org.lwjgl.system.Configuration;

import java.nio.file.Files;
import java.nio.file.Paths;

public class GameLauncher {
    public GameLauncher(final String versionString) {
        // Set this to "true" to make the mobile game port run as a full-screen desktop application
        boolean desktopMode = true;//cmd.hasOption("fullscreen");
        // Set this to the location where you want the mobile game port to look for assets when working as a full-screen desktop application
        // (uncomment the bottom version and comment the top one to load the res folder from the current folder the .jar is in if you would
        // like to make the game load from a desktop game folder configuration).
        //String desktopModeAssetsDir = "../forge-gui/";
        String desktopModeAssetsDir = "./";
        if (!Files.exists(Paths.get(desktopModeAssetsDir + "res")))
            desktopModeAssetsDir = "../forge-gui/";//try IDE run

        // Assets directory used when the game fully emulates smartphone/tablet mode (desktopMode = false), useful when debugging from IDE
        String assetsDir;
        if (!AssetsDownloader.SHARE_DESKTOP_ASSETS) {
            assetsDir = "testAssets/";
            FileUtil.ensureDirectoryExists(assetsDir);
        } else {
            assetsDir = "./";
            if (!Files.exists(Paths.get(assetsDir + "res")))
                assetsDir = "../forge-gui/";
        }

        // Place the file "switch_orientation.ini" to your assets folder to make the game switch to landscape orientation (unless desktopMode = true)
        String switchOrientationFile = assetsDir + "switch_orientation.ini";
        // This should fix MAC-OS startup without the need for -XstartOnFirstThread parameter
        if (SharedLibraryLoader.isMac) {
            Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
        }
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setResizable(false);
        ApplicationListener start = Forge.getApp(new Lwjgl3Clipboard(), new Main.DesktopAdapter(switchOrientationFile),//todo get totalRAM && isTabletDevice
                desktopMode ? desktopModeAssetsDir : assetsDir, false, false, 0, false, 0, "", "");
        if (Config.instance().getSettingData().fullScreen) {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
            config.setAutoIconify(true);
            config.setHdpiMode(HdpiMode.Logical);
        } else {
            config.setWindowedMode(Config.instance().getSettingData().width, Config.instance().getSettingData().height);
        }
        config.setTitle("Forge - " + versionString);
        config.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override
            public boolean closeRequested() {
                //use the device adpater to exit properly
                if (Forge.safeToClose)
                    Forge.exit(true);
                return false;
            }
        });

        if (desktopMode)
            config.setHdpiMode(HdpiMode.Logical);

        new Lwjgl3Application(start, config);
    }
}
