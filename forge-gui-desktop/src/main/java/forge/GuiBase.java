package forge;

import forge.interfaces.IGuiBase;

public class GuiBase {
    private static IGuiBase guiInterface;

    public static IGuiBase getInterface() {
        return guiInterface;
    }
    public static void setInterface(IGuiBase i0) {
        guiInterface = i0;
    }
}
