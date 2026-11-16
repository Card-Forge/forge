package forge.adventure.scene;

import forge.screens.FScreen;
import forge.screens.settings.SettingsScreen;

public class GeneralSettingsScene extends ForgeScene {
    private static GeneralSettingsScene object;
    private SettingsScreen screen;

    private GeneralSettingsScene() {
    }

    public static GeneralSettingsScene instance() {
        if (object == null)
            object = new GeneralSettingsScene();
        return object;
    }

    @Override
    public void enter() {
        screen = SettingsScreen.getInstance(false);
        super.enter();
    }

    @Override
    public FScreen getScreen() {
        return screen;
    }
}
