package forge.app;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglClipboard;

import forge.Forge;
import forge.assets.AssetsDownloader;
import forge.interfaces.IDeviceAdapter;
import forge.util.FileUtil;
import forge.util.Utils;

public class Main {
    public static void main(String[] args) {
        String assetsDir = AssetsDownloader.SHARE_DESKTOP_ASSETS ? "../forge-gui/" : "testAssets/";
        if (!AssetsDownloader.SHARE_DESKTOP_ASSETS) {
            FileUtil.ensureDirectoryExists(assetsDir);
        }

        new LwjglApplication(Forge.getApp(new LwjglClipboard(), new DesktopAdapter(),
                assetsDir, null), "Forge", Utils.DEV_SCREEN_WIDTH, Utils.DEV_SCREEN_HEIGHT);
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
    }
}
