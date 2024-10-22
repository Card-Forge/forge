package forge.ios;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.commons.lang3.tuple.Pair;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIPasteboard;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.badlogic.gdx.backends.iosrobovm.IOSFiles;

import forge.Forge;
import forge.interfaces.IDeviceAdapter;

public class Main extends IOSApplication.Delegate {

    @Override
    protected IOSApplication createApplication() {
        final String assetsDir = new IOSFiles().getLocalStoragePath() + "/../../forge.ios.Main.app/";

        final IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        final ApplicationListener app = Forge.getApp(new IOSClipboard(), new IOSAdapter(), assetsDir, false, false, 0, false, 0, "", "");
        final IOSApplication iosApp = new IOSApplication(app, config);
        return iosApp;
    }

    public static void main(String[] args) {
        final NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(args, null, Main.class);
        pool.close();
    }

    //special clipboard that works on iOS
    private static final class IOSClipboard implements com.badlogic.gdx.utils.Clipboard {
        @Override
        public boolean hasContents() {
            return UIPasteboard.getGeneralPasteboard().toString().length() > 0;
        }

        @Override
        public String getContents() {
            return UIPasteboard.getGeneralPasteboard().getString();
        }

        @Override
        public void setContents(final String contents0) {
            UIPasteboard.getGeneralPasteboard().setString(contents0);
        }
    }

    private static final class IOSAdapter implements IDeviceAdapter {
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
            return new IOSFiles().getExternalStoragePath();
        }

        @Override
        public String getVersionString() {
            return "0.0";
        }

        @Override
        public boolean openFile(final String filename) {
            return new IOSFiles().local(filename).exists();
        }

        @Override
        public void setLandscapeMode(final boolean landscapeMode) {
            // TODO implement this
        }

        @Override
        public void preventSystemSleep(boolean preventSleep) {
            // TODO implement this
        }

        @Override
        public boolean isTablet() {
            return Gdx.graphics.getWidth() > Gdx.graphics.getHeight();
        }

        @Override
        public void restart() {
            // Not possible on iOS
        }

        @Override
        public void exit() {
            // Not possible on iOS
        }

        @Override
        public void closeSplashScreen() {
            //only for desktop mobile-dev
        }

        @Override
        public void convertToJPEG(InputStream input, OutputStream output) throws IOException {

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