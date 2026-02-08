package forge.gui;

import forge.util.HWInfo;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.localinstance.properties.ForgePreferences;

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
        if (hwInfo != null) {
            return "##########################################\n" +
                    "APP: Forge v." + getInterface().getCurrentVersion() +
                    "\nDEV: " + hwInfo.device().getName() + (hwInfo.getChipset() ?
                    "\nSOC: " + hwInfo.device().getChipset() :
                    "\nCPU: " + hwInfo.device().getCpuDescription()) +
                    "\nRAM: " + deviceRAM + " MB" +
                    "\nOS: " + hwInfo.os().getRawDescription() +
                    "\n##########################################";
        }
        return "";
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
