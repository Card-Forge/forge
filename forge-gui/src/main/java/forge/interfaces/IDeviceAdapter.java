package forge.interfaces;

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
}
