package forge.app;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglClipboard;

import forge.Forge;
import forge.assets.AssetsDownloader;
import forge.interfaces.IDeviceAdapter;
import forge.util.FileUtil;
import forge.util.OperatingSystem;
import forge.util.RestartUtil;
import forge.util.Utils;

public class Main {
    public static void main(String[] args) {
        // Set this to "true" to make the mobile game port run as a full-screen desktop application
        boolean desktopMode = false;
        // Set this to the location where you want the mobile game port to look for assets when working as a full-screen desktop application
        // (uncomment the bottom version and comment the top one to load the res folder from the current folder the .jar is in if you would
        // like to make the game load from a desktop game folder configuration).
        String desktopModeAssetsDir = "../forge-gui/";
        //String desktopModeAssetsDir = "./";
        
        // Assets directory used when the game fully emulates smartphone/tablet mode (desktopMode = false), useful when debugging from IDE
        String assetsDir = AssetsDownloader.SHARE_DESKTOP_ASSETS ? "../forge-gui/" : "testAssets/";
        if (!AssetsDownloader.SHARE_DESKTOP_ASSETS) {
            FileUtil.ensureDirectoryExists(assetsDir);
        }

        // Place the file "switch_orientation.ini" to your assets folder to make the game switch to landscape orientation (unless desktopMode = true)
        String switchOrientationFile = assetsDir + "switch_orientation.ini";
        boolean landscapeMode = FileUtil.doesFileExist(switchOrientationFile);

        // Width and height for standard smartphone/tablet mode (desktopMode = false)
        int screenWidth = landscapeMode ? (int)(Utils.BASE_HEIGHT * 16 / 9) : (int)Utils.BASE_WIDTH;
        int screenHeight = (int)Utils.BASE_HEIGHT;

        // Fullscreen width and height for desktop mode (desktopMode = true)
        // Can be specified inside the file fullscreen_resolution.ini to override default (in the format WxH, e.g. 1920x1080)
        int fullscreenWidth = LwjglApplicationConfiguration.getDesktopDisplayMode().width;
        int fullscreenHeight = LwjglApplicationConfiguration.getDesktopDisplayMode().height;
        if (FileUtil.doesFileExist(assetsDir + "fullscreen_resolution.ini")) {
            String[] res = new String(FileUtil.readFileToString(assetsDir + "fullscreen_resolution.ini")).split("x");
            if (res.length == 2) {
                fullscreenWidth = Integer.parseInt(res[0].trim());
                fullscreenHeight = Integer.parseInt(res[1].trim());
            }
        }
                
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.resizable = false;
        config.width = desktopMode ? fullscreenWidth : screenWidth;
        config.height = desktopMode ? fullscreenHeight : screenHeight;
        config.fullscreen = desktopMode ? true : false;
        config.title = "Forge";
        config.useHDPI = desktopMode ? true : false; // enable HiDPI on Mac OS

        new LwjglApplication(Forge.getApp(new LwjglClipboard(), new DesktopAdapter(switchOrientationFile),
                desktopMode ? desktopModeAssetsDir : assetsDir), config);
    }

    private static class DesktopAdapter implements IDeviceAdapter {
        private final String switchOrientationFile;

        private DesktopAdapter(String switchOrientationFile0) {
            switchOrientationFile = switchOrientationFile0;
        }

        //just assume desktop always connected to wifi
        @Override
        public boolean isConnectedToInternet() {
            return true;
        }

        @Override
        public boolean isConnectedToWifi() {
            return true;
        }

        @Override
        public String getDownloadsDir() {
            return System.getProperty("user.home") + "/Downloads/";
        }

        @Override
        public boolean openFile(String filename) {
            try {
                Desktop.getDesktop().open(new File(filename));
                return true;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public void restart() {
            if (RestartUtil.prepareForRestart()) {
                Gdx.app.exit();
            }
        }

        @Override
        public void exit() {
            Gdx.app.exit(); //can just use Gdx.app.exit for desktop
        }

        @Override
        public boolean isTablet() {
            return true; //treat desktop the same as a tablet
        }

        @Override
        public void setLandscapeMode(boolean landscapeMode) {
            //create file to indicate that landscape mode should be used
            if (landscapeMode) {
                FileUtil.writeFile(switchOrientationFile, "1");
            }
            else {
                FileUtil.deleteFile(switchOrientationFile);
            }
        }

        @Override
        public void preventSystemSleep(boolean preventSleep) {
            OperatingSystem.preventSystemSleep(preventSleep);
        }
    }
}
