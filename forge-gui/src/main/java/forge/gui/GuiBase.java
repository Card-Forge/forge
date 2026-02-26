package forge.gui;

import forge.util.HWInfo;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.localinstance.properties.ForgePreferences;
import org.tinylog.Logger;

public class GuiBase {
    private static IGuiBase guiInterface;
    private static boolean propertyConfig = true;
    private static boolean isAndroidport = false;
    private static String adventureDirectory = null;
    private static boolean interrupted = false;
    private static int androidAPI = 0;
    private static int deviceRAM = 0;
    private static String downloadsDir = "";
    private static boolean usingAppDirectory = false;
    private static ForgePreferences forgePrefs;
    private static HWInfo hwInfo;

    public static IGuiBase getInterface() { return guiInterface; }
    public static void setInterface(IGuiBase i0) { guiInterface = i0; }
    public static ForgePreferences getForgePrefs() {
        if (forgePrefs == null)
            forgePrefs = new ForgePreferences();
        return forgePrefs;
    }

    public static void setIsAndroid(boolean value) { isAndroidport = value; }
    public static boolean isAndroid() { return isAndroidport; }

    public static void setAdventureDirectory(String directory) { adventureDirectory = directory; }
    public static String getAdventureDirectory() { return adventureDirectory; }

    public static void setUsingAppDirectory(boolean value) { usingAppDirectory = value; }
    public static boolean isUsingAppDirectory() { return usingAppDirectory; }

    public static void setDeviceInfo(HWInfo hw, int AndroidAPI, int RAM, String dir) {
        hwInfo = hw;
        androidAPI = AndroidAPI;
        deviceRAM = RAM;
        downloadsDir = dir;
    }
    public static String getHWInfo() {
        Runtime runtime = Runtime.getRuntime();
        StringBuilder sb = new StringBuilder();
        sb.append("##########################################\n");
        sb.append("APP: Forge v.").append(getInterface().getCurrentVersion());
        if (hwInfo != null) {
            sb.append("\nDEV: ").append(hwInfo.device().getName());
            sb.append(hwInfo.getChipset()
                    ? "\nSOC: " + hwInfo.device().getChipset()
                    : "\nCPU: " + hwInfo.device().getCpuDescription());
            sb.append("\nRAM: ").append(deviceRAM).append(" MB");
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
    public static String getDownloadsDir() {
        return downloadsDir;
    }
    public static int getAndroidAPILevel() { return androidAPI; }
    public static int getDeviceRAM() { return deviceRAM; }

    public static boolean isNetworkplay(IGuiGame game) {
        if (game != null) {
            // query AbstractGuiGame implementation if provided
            return game.isNetGame();
        }
        // both IGuiBase implementations should have (at least indirect) access to matches
        // to check all available IGuiGame
        return getInterface().hasNetGame();
    }

    public static boolean hasPropertyConfig() { return propertyConfig; }
    public static void enablePropertyConfig(boolean value) { propertyConfig = value; }

    public static void setInterrupted(boolean value) { interrupted = value; }
    public static boolean isInterrupted() { return interrupted; }
}
