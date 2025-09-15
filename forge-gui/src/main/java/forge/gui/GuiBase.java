package forge.gui;

import forge.util.HWInfo;
import forge.gui.interfaces.IGuiBase;
import forge.localinstance.properties.ForgePreferences;

public class GuiBase {
    private static IGuiBase guiInterface;
    private static boolean propertyConfig = true;
    private static boolean networkplay = false;
    private static boolean isAndroidport = false;
    private static boolean isAdventureMode = false;
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

    public static void setIsAdventureMode(boolean value) { isAdventureMode = value; }
    public static boolean isAdventureMode() { return isAdventureMode; }

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

    public static boolean isNetworkplay() { return networkplay; }
    public static void setNetworkplay(boolean value) { networkplay = value; }

    public static boolean hasPropertyConfig() { return propertyConfig; }
    public static void enablePropertyConfig(boolean value) { propertyConfig = value; }

    public static void setInterrupted(boolean value) { interrupted = value; }
    public static boolean isInterrupted() { return interrupted; }
}
