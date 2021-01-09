package forge.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglClipboard;
import forge.Forge;
import forge.assets.AssetsDownloader;
import forge.interfaces.IDeviceAdapter;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.util.FileUtil;
import forge.util.OperatingSystem;
import forge.util.RestartUtil;
import forge.util.Utils;
import org.apache.commons.cli.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("h","help", false, "Show help.");
        options.addOption("f","fullscreen", false,"fullscreen mode");
        options.addOption("l","landscape", false,"landscape mode");
        options.addOption("r","resolution", true,"resolution (WxH)");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("forge-mobile-dev", options);

            System.exit(1);
            return;
        }

        // Set this to "true" to make the mobile game port run as a full-screen desktop application
        boolean desktopMode = true; // cmd.hasOption("fullscreen");
        // Set this to the location where you want the mobile game port to look for assets when working as a full-screen desktop application
        // (uncomment the bottom version and comment the top one to load the res folder from the current folder the .jar is in if you would
        // like to make the game load from a desktop game folder configuration).
        //String desktopModeAssetsDir = "../forge-gui/";
        String desktopModeAssetsDir = "./";

        // Assets directory used when the game fully emulates smartphone/tablet mode (desktopMode = false), useful when debugging from IDE
        String assetsDir = AssetsDownloader.SHARE_DESKTOP_ASSETS ? "../forge-gui/" : "testAssets/";
        if (!AssetsDownloader.SHARE_DESKTOP_ASSETS) {
            FileUtil.ensureDirectoryExists(assetsDir);
        }

        // Place the file "switch_orientation.ini" to your assets folder to make the game switch to landscape orientation (unless desktopMode = true)
        String switchOrientationFile = assetsDir + "switch_orientation.ini";
        Boolean landscapeMode = FileUtil.doesFileExist(switchOrientationFile) || cmd.hasOption("landscape");
        String[] res;

        // Width and height for standard smartphone/tablet mode (desktopMode = false)
        int screenWidth = landscapeMode ? (int)(Utils.BASE_HEIGHT * 16 / 9) : (int)Utils.BASE_WIDTH;
        int screenHeight = (int)Utils.BASE_HEIGHT;
        if (cmd.hasOption("resolution")) {
            res = cmd.getOptionValue("resolution").split("x");
            if (res.length >= 2) {
                screenWidth = Integer.parseInt(res[0].trim());
                screenHeight = Integer.parseInt(res[1].trim());
            }
        }

        // Fullscreen width and height for desktop mode (desktopMode = true)
        // Can be specified inside the file fullscreen_resolution.ini to override default (in the format WxH, e.g. 1920x1080)
        int desktopScreenWidth = LwjglApplicationConfiguration.getDesktopDisplayMode().width;
        int desktopScreenHeight = LwjglApplicationConfiguration.getDesktopDisplayMode().height;
        boolean fullscreenFlag = true;
        if (FileUtil.doesFileExist(desktopModeAssetsDir + "screen_resolution.ini")) {
            res = FileUtil.readFileToString(desktopModeAssetsDir + "screen_resolution.ini").split("x");
            fullscreenFlag = res.length != 3 || Integer.parseInt(res[2].trim()) > 0;
            if (res.length >= 2) {
                desktopScreenWidth = Integer.parseInt(res[0].trim());
                desktopScreenHeight = Integer.parseInt(res[1].trim());
            }
        }

        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.resizable = false;
        config.width = desktopMode ? desktopScreenWidth : screenWidth;
        config.height = desktopMode ? desktopScreenHeight : screenHeight;
        config.fullscreen = desktopMode && fullscreenFlag;
        config.title = "Forge";
        config.useHDPI = desktopMode; // enable HiDPI on Mac OS

        ForgePreferences prefs = FModel.getPreferences();
        boolean propertyConfig = prefs != null && prefs.getPrefBoolean(ForgePreferences.FPref.UI_NETPLAY_COMPAT);
        new LwjglApplication(Forge.getApp(new LwjglClipboard(), new DesktopAdapter(switchOrientationFile),//todo get totalRAM && isTabletDevice
                desktopMode ? desktopModeAssetsDir : assetsDir, propertyConfig, false, 0, false, 0), config);
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

        @Override
        public void convertToJPEG(InputStream input, OutputStream output) throws IOException {
            BufferedImage image = ImageIO.read(input);
            ImageIO.write(image, "jpg", output);
        }
    }
}
