package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.util.UIActor;
import forge.adventure.world.WorldSave;

public class StartScene extends Scene {
    UIActor ui;
    Stage stage;
    Actor saveButton;
    Actor resumeButton;

    public StartScene() {

    }

    @Override
    public void dispose() {
        if(stage!=null)
            stage.dispose();
    }

    @Override
    public void render() {

        //Batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(1, 0, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
        //Batch.end();
    }

    public boolean NewGame() {
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.NewGameScene.instance);
        return true;
    }

    public boolean Save() {
        ((SaveLoadScene) SceneType.SaveLoadScene.instance).SetSaveGame(true);
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.SaveLoadScene.instance);
        return true;
    }

    public boolean Load() {
        ((SaveLoadScene) SceneType.SaveLoadScene.instance).SetSaveGame(false);
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.SaveLoadScene.instance);
        return true;
    }

    public boolean Resume() {
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.GameScene.instance);
        return true;
    }

    public boolean settings() {
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(forge.adventure.scene.SceneType.SettingsScene.instance);
        return true;
    }

    public boolean Exit() {
        Gdx.app.exit();
        return true;
    }

    @Override
    public void Enter() {


        saveButton.setVisible(WorldSave.getCurrentSave() != null);
        resumeButton.setVisible(WorldSave.getCurrentSave() != null);
        Gdx.input.setInputProcessor(stage); //Start taking input from the ui
    }

    @Override
    public void create() {


    }

    @Override
    public void ResLoaded() {
        stage = new Stage(new StretchViewport(GetIntendedWidth(), GetIntendedHeight()));
        ui = new UIActor(AdventureApplicationAdapter.CurrentAdapter.GetRes().GetFile("ui/startmenu.json"));

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
        stage.addActor(ui);
    }
}
