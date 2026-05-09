package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;
import forge.adventure.archipelago.Archipelago;

import java.net.URISyntaxException;

/**
 * Scene to handle settings of the archipelago client
 */
public class ArchipelagoSettingsScene extends UIScene {

    private TextraButton backButton;
    private TypingLabel connectStatusLabel;
    private TextField ipTextField;
    private TextField portTextField;
    private TextField slotNameTextField;
    private TextField passwordTextField;

    private ArchipelagoSettingsScene() {
        super("ui/apsettings.json");

        backButton = ui.findActor("return");
        ipTextField = ui.findActor("apIP");
        portTextField = ui.findActor("apPort");
        slotNameTextField = ui.findActor("apSlotName");
        passwordTextField = ui.findActor("apPassword");

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
        Archipelago.getInstance().connect(ipTextField.getText(), portTextField.getText(), slotNameTextField.getText(), passwordTextField.getText(), connectStatusLabel);
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
