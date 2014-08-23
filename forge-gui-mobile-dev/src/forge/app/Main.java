package forge.app;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglClipboard;

import forge.Forge;
import forge.assets.AssetsDownloader;
import forge.interfaces.INetworkConnection;
import forge.util.FileUtil;
import forge.util.Utils;

public class Main {
    public static void main(String[] args) {
        String assetsDir = AssetsDownloader.SHARE_DESKTOP_ASSETS ? "../forge-gui/" : "testAssets/";
        if (!AssetsDownloader.SHARE_DESKTOP_ASSETS) {
            FileUtil.ensureDirectoryExists(assetsDir);
        }

        new LwjglApplication(Forge.getApp(new LwjglClipboard(), new DesktopNetworkConnection(),
                assetsDir, null), "Forge", Utils.DEV_SCREEN_WIDTH, Utils.DEV_SCREEN_HEIGHT);
    }

    private static class DesktopNetworkConnection implements INetworkConnection {
        //just assume desktop always connected to wifi
        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public boolean isConnectedToWifi() {
            return true;
        }
    }
}
