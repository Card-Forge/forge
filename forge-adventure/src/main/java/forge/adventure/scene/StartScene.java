package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.world.WorldSave;

/**
 * First scene after the splash screen
 */
public class StartScene extends UIScene {

    Actor saveButton;
    Actor resumeButton;

    public StartScene()
    {
        super("ui/start_menu.json");

    }

    public boolean NewGame() {
        AdventureApplicationAdapter.instance.switchScene(SceneType.NewGameScene.instance);
        return true;
    }

    public boolean Save() {
        ((SaveLoadScene) SceneType.SaveLoadScene.instance).setSaveGame(true);
        AdventureApplicationAdapter.instance.switchScene(SceneType.SaveLoadScene.instance);
        return true;
    }

    public boolean Load() {
        ((SaveLoadScene) SceneType.SaveLoadScene.instance).setSaveGame(false);
        AdventureApplicationAdapter.instance.switchScene(SceneType.SaveLoadScene.instance);
        return true;
    }

    public boolean Resume() {
        AdventureApplicationAdapter.instance.switchToLast();
        return true;
    }

    public boolean settings() {
        AdventureApplicationAdapter.instance.switchScene(forge.adventure.scene.SceneType.SettingsScene.instance);
        return true;
    }

    public boolean Exit() {
        Gdx.app.exit();
        System.exit(0);
        return true;
    }

    @Override
    public void enter() {


        saveButton.setVisible(WorldSave.getCurrentSave().getWorld().getData() != null);
        resumeButton.setVisible(WorldSave.getCurrentSave().getWorld().getData() != null);
        Gdx.input.setInputProcessor(stage); //Start taking input from the ui
    }

    @Override
    public void create() {


    }

    @Override
    public void resLoaded() {
        super.resLoaded();

        ui.onButtonPress("Start", () -> NewGame());
        ui.onButtonPress("Load", () -> Load());
        ui.onButtonPress("Start", () -> NewGame());
        ui.onButtonPress("Save", () -> Save());
        ui.onButtonPress("Resume", () -> Resume());

        saveButton = ui.findActor("Save");
        resumeButton = ui.findActor("Resume");
        ui.onButtonPress("Settings", () -> settings());
        ui.onButtonPress("Exit", () -> Exit());
        saveButton.setVisible(false);
        resumeButton.setVisible(false);
    }
}
