package forge.adventure.scene;

import forge.screens.FScreen;
import forge.screens.settings.ExcludedSetsScreen;

public class ExcludedSetsScene extends ForgeScene {
    private static ExcludedSetsScene object;
    private ExcludedSetsScreen screen;
    private Runnable onChanged;

    private ExcludedSetsScene() {
    }

    public static ExcludedSetsScene getInstance(Runnable onChanged) {
        if (object == null)
            object = new ExcludedSetsScene();
        object.onChanged = onChanged;
        return object;
    }

    @Override
    public void enter() {
        // a new screen each time, so it starts from the current preferences
        screen = new ExcludedSetsScreen(onChanged);
        super.enter();
    }

    @Override
    public FScreen getScreen() {
        return screen;
    }
}
