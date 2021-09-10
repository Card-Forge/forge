package forge.adventure;

import com.badlogic.gdx.Gdx;
import forge.interfaces.IDeviceAdapter;
import forge.util.FileUtil;
import forge.util.OperatingSystem;
import forge.util.RestartUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Adapter for desktop usage
 */
public class DesktopAdapter implements IDeviceAdapter {
    private final String switchOrientationFile;

    public DesktopAdapter(String switchOrientationFile0) {
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
        } catch (IOException e) {
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
}