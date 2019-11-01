package forge;

import forge.interfaces.IGuiBase;

public class GuiBase {
    private static IGuiBase guiInterface;
    private static boolean propertyConfig = true;
    private static boolean networkplay = false;

    public static IGuiBase getInterface() {
        return guiInterface;
    }
    public static void setInterface(IGuiBase i0) {
        guiInterface = i0;
    }
    public static void setpropertyConfig(boolean value) {
        propertyConfig = value;
    }
    public static boolean isNetworkplay() {
        return networkplay;
    }
    public static void setNetworkplay(boolean value) {
        networkplay = value;
    }
    public static boolean getpropertyConfig() {
        return propertyConfig;
    }
}
