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

import java.util.concurrent.Callable;

public class StartScene extends Scene {
    Texture Background;
    Texture Title;

    public StartScene() {

    }

    @Override
    public void dispose() {
        Stage.dispose();
        Background.dispose();
    }

    @Override
    public void render() {

        //Batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(1,0,1,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Stage.getBatch().begin();
        Stage.getBatch().disableBlending();
        Stage.getBatch().draw(Background,0,0,IntendedWidth,IntendedHeight);
        Stage.getBatch().enableBlending();
        int width=Title.getWidth();
        Stage.getBatch().draw(Title,(IntendedWidth/2)-(Title.getWidth()/2), IntendedHeight-IntendedHeight/4);
        Stage.getBatch().end();
        Stage.act(Gdx.graphics.getDeltaTime());
        Stage.draw();
        //Batch.end();
    }

    private void AddButton(String name, Callable func, int ypos)
    {
        TextButton button = Controls.newTextButton(name) ;
        button.getLabel().setFontScale(3);
        button.setPosition(1200,ypos);
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
        Stage.addActor(button);
    }
    public boolean NewGame()
    {
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(forge.adventure.scene.SceneType.NewGameScene.instance);
        return true;
    }
    public boolean Load()
    {
        return true;
    }
    public boolean Resume()
    {
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
    @Override
    public void create() {
        Stage = new Stage(new StretchViewport(IntendedWidth,IntendedHeight));
        Background = new Texture( AdventureApplicationAdapter.CurrentAdapter.GetRes().GetFile("img/title_bg.png"));
        Title = new Texture( AdventureApplicationAdapter.CurrentAdapter.GetRes().GetFile("img/title.png"));

        AddButton("new game", () -> NewGame(), 800);
        AddButton("load",() -> Load(),700);
        AddButton("save",() -> Load(),600);
        AddButton("resume",() -> Resume(),500);
        AddButton("settings",() -> settings(),400);
        AddButton("exit",() -> Exit(),300);



    }
}
