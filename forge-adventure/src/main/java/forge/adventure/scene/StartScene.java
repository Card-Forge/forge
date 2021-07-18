package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.util.Controls;
import forge.adventure.world.WorldSave;

import java.util.concurrent.Callable;

public class StartScene extends Scene {
    Texture Background;
    Texture Title;

    public StartScene() {

    }
    Stage stage;
    @Override
    public void dispose() {
        stage.dispose();
        Background.dispose();
    }

    @Override
    public void render() {

        //Batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(1,0,1,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().begin();
        stage.getBatch().disableBlending();
        stage.getBatch().draw(Background,0,0,IntendedWidth,IntendedHeight);
        stage.getBatch().enableBlending();
        int width=Title.getWidth();
        stage.getBatch().draw(Title,(IntendedWidth/2)-(Title.getWidth()/2), IntendedHeight-IntendedHeight/4);
        stage.getBatch().end();
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
        //Batch.end();
    }

    private TextButton AddButton(String name, Callable func, int ypos)
    {
        TextButton button = Controls.newTextButton(name) ;
        button.getLabel().setFontScale(3);
        button.setPosition(960-200,ypos);
        button.setSize(400,80);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    func.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }});
        stage.addActor(button);
        return button;
    }
    public boolean NewGame()
    {
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.NewGameScene.instance);
        return true;
    }
    public boolean Save()
    {
        ((SaveLoadScene)SceneType.SaveLoadScene.instance).SetSaveGame(true);
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.SaveLoadScene.instance);
        return true;
    }
    public boolean Load()
    {
        ((SaveLoadScene)SceneType.SaveLoadScene.instance).SetSaveGame(false);
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.SaveLoadScene.instance);
        return true;
    }
    public boolean Resume()
    {
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.GameScene.instance);
        return true;
    }
    public boolean settings()
    {
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(forge.adventure.scene.SceneType.SettingsScene.instance);
        return true;
    }
    public boolean Exit()
    {
        Gdx.app.exit();
        return true;
    }
    TextButton saveButton;
    TextButton resumeButton;
    @Override
    public void Enter()
    {

        saveButton.setVisible(WorldSave.getCurrentSave()!=null);
        resumeButton.setVisible(WorldSave.getCurrentSave()!=null);
        Gdx.input.setInputProcessor(stage); //Start taking input from the ui
    }
    @Override
    public void create() {
        stage = new Stage(new StretchViewport(IntendedWidth,IntendedHeight));
        Background = new Texture( AdventureApplicationAdapter.CurrentAdapter.GetRes().GetFile("img/title_bg.png"));
        Title = new Texture( AdventureApplicationAdapter.CurrentAdapter.GetRes().GetFile("img/title.png"));

        AddButton("New Game", () -> NewGame(), 650);
        AddButton("Load",() -> Load(),550);
        saveButton= AddButton("Save",() -> Save(),450);
        resumeButton=AddButton("Resume",() -> Resume(),350);
        AddButton("Settings",() -> settings(),250);
        AddButton("Exit",() -> Exit(),150);
        saveButton.setVisible(false);
        resumeButton.setVisible(false);

    }
}
