package forge.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IDeviceAdapter {
    boolean isConnectedToInternet();
    boolean isConnectedToWifi();
    boolean isTablet();
    String getDownloadsDir();
    boolean openFile(String filename);
    void setLandscapeMode(boolean landscapeMode);
    void preventSystemSleep(boolean preventSleep);
    void restart();
    void exit();
    void convertToJPEG(InputStream input, OutputStream output) throws IOException;
}
