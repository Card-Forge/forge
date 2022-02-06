package forge.adventure.editor;

import forge.GuiMobile;
import forge.adventure.util.Config;
import forge.gui.GuiBase;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class Main {
    public static void main(String[] args) {
        GuiBase.setInterface(new GuiMobile(Files.exists(Paths.get("./res"))?"./":"../forge-gui/"));
        GuiBase.setDeviceInfo("", "", 0, 0);
        Config.instance();
         new EditorMainWindow();
    }
}
