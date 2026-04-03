package forge.adventure.scene;

import com.github.tommyettinger.textra.TextraButton;
import forge.Forge;

/**
 * Scene to show the current archipelago progress in Forge
 */
public class ArchipelagoTrackerScene extends UIScene {

    TextraButton backButton;

    private ArchipelagoTrackerScene() {
        super("ui/aptracker.json");

        backButton = ui.findActor("return");
        ui.onButtonPress("return", ArchipelagoTrackerScene.this::back);
    }


    public boolean back() {
        Forge.switchToLast();
        return true;
    }

    private static ArchipelagoTrackerScene object;

    public static ArchipelagoTrackerScene instance() {
        if (object == null)
            object = new ArchipelagoTrackerScene();
        return object;
    }


    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
    }

}