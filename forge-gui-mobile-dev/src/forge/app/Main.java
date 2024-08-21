package forge.app;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import forge.Forge;
import forge.adventure.util.Config;
import forge.assets.AssetsDownloader;
import forge.interfaces.IDeviceAdapter;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.FileUtil;
import forge.util.OperatingSystem;
import forge.util.RestartUtil;
import forge.util.Utils;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

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
        boolean desktopMode = true;//cmd.hasOption("fullscreen");
        // Set this to the location where you want the mobile game port to look for assets when working as a full-screen desktop application
        // (uncomment the bottom version and comment the top one to load the res folder from the current folder the .jar is in if you would
        // like to make the game load from a desktop game folder configuration).
        //String desktopModeAssetsDir = "../forge-gui/";
        String desktopModeAssetsDir = "./";
        if(!Files.exists(Paths.get(desktopModeAssetsDir+"res")))
            desktopModeAssetsDir = "../forge-gui/";//try IDE run

        // Assets directory used when the game fully emulates smartphone/tablet mode (desktopMode = false), useful when debugging from IDE
        String assetsDir ;
        if (!AssetsDownloader.SHARE_DESKTOP_ASSETS) {
            assetsDir= "testAssets/";
            FileUtil.ensureDirectoryExists(assetsDir);
        }
        else
        {
            assetsDir= "./";
            if(!Files.exists(Paths.get(assetsDir+"res")))
                assetsDir = "../forge-gui/";
        }

        // Place the file "switch_orientation.ini" to your assets folder to make the game switch to landscape orientation (unless desktopMode = true)
        String switchOrientationFile = assetsDir + "switch_orientation.ini";
        boolean landscapeMode = FileUtil.doesFileExist(switchOrientationFile) || cmd.hasOption("landscape");
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
            desktopMode=false;
        }

        // Fullscreen width and height for desktop mode (desktopMode = true)
        // Can be specified inside the file fullscreen_resolution.ini to override default (in the format WxH, e.g. 1920x1080)
        int desktopScreenWidth = Lwjgl3ApplicationConfiguration.getDisplayMode().width;
        int desktopScreenHeight = Lwjgl3ApplicationConfiguration.getDisplayMode().height;
        boolean fullscreenFlag = true;
        if (FileUtil.doesFileExist(desktopModeAssetsDir + "screen_resolution.ini")) {
            res = FileUtil.readFileToString(desktopModeAssetsDir + "screen_resolution.ini").split("x");
            fullscreenFlag = res.length != 3 || Integer.parseInt(res[2].trim()) > 0;
            if (res.length >= 2) {
                desktopScreenWidth = Integer.parseInt(res[0].trim());
                desktopScreenHeight = Integer.parseInt(res[1].trim());
            }
        }

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setResizable(false);
        ForgePreferences prefs = FModel.getPreferences();
        boolean propertyConfig = prefs != null && prefs.getPrefBoolean(ForgePreferences.FPref.UI_NETPLAY_COMPAT);
        ApplicationListener start = Forge.getApp(new Lwjgl3Clipboard(), new DesktopAdapter(switchOrientationFile),//todo get totalRAM && isTabletDevice
                desktopMode ? desktopModeAssetsDir : assetsDir, propertyConfig, false, 0, false, 0, "", "");
        if (Config.instance().getSettingData().fullScreen) {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
            config.setAutoIconify(true);
            config.setHdpiMode(HdpiMode.Logical);
        } else {
            config.setWindowedMode(Config.instance().getSettingData().width, Config.instance().getSettingData().height);
        }
        config.setTitle("Forge");
        config.setWindowListener(new Lwjgl3WindowAdapter(){
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
                System.exit(0);
            }
        }

        @Override
        public void exit() {
            Gdx.app.exit(); //can just use Gdx.app.exit for desktop
            System.exit(0);
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

        @Override
        public Pair<Integer, Integer> getRealScreenSize(boolean real) {
            return Pair.of(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }

        @Override
        public ArrayList<String> getGamepads() {
            return new ArrayList<>();
        }
    }
}
