package forge.gui;

import forge.util.HWInfo;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import org.tinylog.Logger;

public class GuiBase {
    private static IGuiBase guiInterface;
    private static boolean isAndroidport = false;
    private static boolean isIOSport = false;
    private static String adventureDirectory = null;
    private static int androidAPI = 0;
    private static String downloadsDir = "";
    private static boolean usingAppDirectory = false;
    private static HWInfo hwInfo;

    public static IGuiBase getInterface() { return guiInterface; }
    public static void setInterface(IGuiBase i0) { guiInterface = i0; }

    public static void setIsAndroid(boolean value) { isAndroidport = value; }
    public static boolean isAndroid() { return isAndroidport; }

    /** Set by the iOS launcher (forge.ios.Main), mirroring the Android flag above, so shared
     *  modules can branch on the platform without sniffing libGDX's ApplicationType. */
    public static void setIsIOS(boolean value) { isIOSport = value; }
    public static boolean isIOS() { return isIOSport; }
    public static int getAndroidAPILevel() { return androidAPI; }
    public static String getDownloadsDir() {
        return downloadsDir;
    }

    public static void setAdventureDirectory(String directory) { adventureDirectory = directory; }
    public static String getAdventureDirectory() { return adventureDirectory; }

    public static void setUsingAppDirectory(boolean value) { usingAppDirectory = value; }
    public static boolean isUsingAppDirectory() { return usingAppDirectory; }

    public static void setDeviceInfo(HWInfo hw, int AndroidAPI, String dir) {
        hwInfo = hw;
        androidAPI = AndroidAPI;
        downloadsDir = dir;
    }
    public static String getHWInfo() {
        Runtime runtime = Runtime.getRuntime();
        StringBuilder sb = new StringBuilder();
        sb.append("##########################################");
        sb.append("\nAPP: Forge v.").append(getInterface().getCurrentVersion());
        if (hwInfo != null) {
            sb.append("\nDEV: ").append(hwInfo.device().getName());
            sb.append(hwInfo.getChipset()
                    ? "\nSOC: " + hwInfo.device().getChipset()
                    : "\nCPU: " + hwInfo.device().getCpuDescription());
            sb.append("\nRAM: ").append(hwInfo.getTotalRam()).append(" MB");
            sb.append("\nOS: ").append(hwInfo.os().getRawDescription());
        } else {
            sb.append("\nJava: ").append(System.getProperty("java.version"))
                    .append(" (").append(System.getProperty("java.vendor")).append(")");
            sb.append("\nOS: ").append(System.getProperty("os.name"))
                    .append(" ").append(System.getProperty("os.version"))
                    .append(" ").append(System.getProperty("os.arch"));
            sb.append("\nRAM: ").append(runtime.maxMemory() / 1024 / 1024)
                    .append(" MB max, ").append(runtime.availableProcessors()).append(" CPUs");
        }
        sb.append("\n##########################################");
        return sb.toString();
    }
    public static void logHWInfo() {
        for (String line : getHWInfo().split("\n")) {
            Logger.info(line);
        }
    }

    public static boolean isNetPlay(IGuiGame game) {
        if (game != null) {
            // query AbstractGuiGame implementation if provided
            return game.isNetGame();
        }
        // both IGuiBase implementations should have (at least indirect) access to matches
        // to check all available IGuiGame
        return getInterface().hasNetGame();
    }
}
