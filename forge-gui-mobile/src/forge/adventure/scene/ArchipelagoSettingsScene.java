package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;

/**
 * Scene to handle settings of the base forge and adventure mode
 */
public class ArchipelagoSettingsScene extends UIScene {

    private TextraButton backButton;
    private TypingLabel connectStatusLabel;
    private ArchipelagoSettingsScene() {
        super("ui/apsettings.json");

        backButton = ui.findActor("return");

        connectStatusLabel = ui.findActor("connectStatusLabel");
        connectStatusLabel.setText("{FADE=RED;RED;0.1}Not Connected...");
        ui.onButtonPress("return", ArchipelagoSettingsScene.this::back);
        ui.onButtonPress("connect", ArchipelagoSettingsScene.this::connect);
    }


    public boolean back() {
        Forge.switchToLast();
        return true;
    }

    public void connect() {
        //TODO: Setup AP client and connect
    }


    private static ArchipelagoSettingsScene object;

    public static ArchipelagoSettingsScene instance() {
        if (object == null)
            object = new ArchipelagoSettingsScene();
        return object;
    }


    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
    }

}