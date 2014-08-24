package forge.interfaces;

public interface IDeviceAdapter {
    boolean isConnectedToInternet();
    boolean isConnectedToWifi();
    String getDownloadsDir();
    boolean openFile(String filename);
    void exit();
}
