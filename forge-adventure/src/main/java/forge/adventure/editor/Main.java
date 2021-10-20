package forge.adventure.editor;

import forge.adventure.util.Config;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class Main {
    public static void main(String[] args) {
        Config.instance();
         new EditorMainWindow();
    }
}
