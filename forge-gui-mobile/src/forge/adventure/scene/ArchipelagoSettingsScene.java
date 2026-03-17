package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;
import forge.Graphics;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.assets.ImageCache;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.sound.SoundSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

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
        ui.onButtonPress("Connect", ArchipelagoSettingsScene.this::connect);
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