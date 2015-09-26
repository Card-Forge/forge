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

        new LwjglApplication(Forge.getApp(new LwjglClipboard(), new DesktopAdapter(),
                assetsDir), "Forge", Utils.DEV_SCREEN_WIDTH, Utils.DEV_SCREEN_HEIGHT);
    }

    private static class DesktopAdapter implements IDeviceAdapter {
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
            //TODO: Consider supporting toggling this on desktop for testing
        }
    }
}
