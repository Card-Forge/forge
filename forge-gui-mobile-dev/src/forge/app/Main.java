package forge.app;

import com.badlogic.gdx.Gdx;
import forge.interfaces.IDeviceAdapter;
import forge.util.*;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.SplashScreen;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

public class Main {
    private static final String versionString = BuildInfo.getVersionString();
    public static void main(String[] args) {
        new GameLauncher(versionString);
    }

    public static class DesktopAdapter implements IDeviceAdapter {
        private final String switchOrientationFile;

        DesktopAdapter(String switchOrientationFile0) {
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
        public String getVersionString() {
            return versionString;
        }

        @Override
        public String getLatestChanges(String commitsAtom, Date buildDateOriginal, Date max) {
            return RSSReader.getCommitLog(commitsAtom, buildDateOriginal, max);
        }

        @Override
        public String getReleaseTag(String releaseAtom) {
            return RSSReader.getLatestReleaseTag(releaseAtom);
        }

        @Override
        public boolean openFile(String filename) {
            try {
                File installer = new File(filename);
                if (installer.exists()) {
                    if (filename.endsWith(".jar")) {
                        installer.setExecutable(true, false);
                        Desktop.getDesktop().open(installer);
                    } else {
                        Desktop.getDesktop().open(installer.getParentFile());
                    }
                }
                return true;
            } catch (IOException e) {
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
        public void closeSplashScreen() {
            // FIXME: on Linux system it can't close splashscreen image or crash with SIGSEGV? How come it works on other OS?
            if (OperatingSystem.isUnix() || OperatingSystem.isSolaris())
                return;
            //could throw exception..
            try {
                Optional.ofNullable(SplashScreen.getSplashScreen()).ifPresent(SplashScreen::close);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            } else {
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
