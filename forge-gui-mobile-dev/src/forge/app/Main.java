package forge.app;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglClipboard;

import forge.Forge;
import forge.assets.AssetsDownloader;
import forge.interfaces.IDeviceAdapter;
import forge.util.FileUtil;
import forge.util.RestartUtil;
import forge.util.Utils;

public class Main {
    public static void main(String[] args) {
        String assetsDir = AssetsDownloader.SHARE_DESKTOP_ASSETS ? "../forge-gui/" : "testAssets/";
        if (!AssetsDownloader.SHARE_DESKTOP_ASSETS) {
            FileUtil.ensureDirectoryExists(assetsDir);
        }

        String switchOrientationFile = assetsDir + "switch_orientation.ini";
        boolean landscapeMode = FileUtil.doesFileExist(switchOrientationFile);
        int screenWidth = landscapeMode ? (int)(Utils.BASE_HEIGHT * 16 / 9) : (int)Utils.BASE_WIDTH;
        int screenHeight = (int)Utils.BASE_HEIGHT;

        new LwjglApplication(Forge.getApp(new LwjglClipboard(), new DesktopAdapter(switchOrientationFile),
                assetsDir), "Forge", screenWidth, screenHeight);
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
            
        }
    }
}
