package forge.adventure.scene;

import com.github.tommyettinger.textra.TextraButton;
import forge.Forge;

/**
 * Scene to handle settings of the base forge and adventure mode
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