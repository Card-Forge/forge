package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import forge.adventure.AdventureApplicationAdapter;

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

        ImageButton button = new ImageButton(DrawableImage("img/title_"+name+".png")) ;
        Button.ButtonStyle style=new ImageButton.ImageButtonStyle();
        style.up=DrawableImage("img/title_"+name+".png");
        style.down=DrawableImage("img/title_"+name+"_pressed.png");
        style.over=DrawableImage("img/title_"+name+"_hover.png");
        button.setStyle(style);
        button.setPosition((IntendedWidth/2)-(button.getWidth()/2),ypos);
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
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.NewGameScene);
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

        AddButton("new_game", () -> NewGame(), (IntendedHeight / 6) * 3);
        AddButton("load",() -> Load(),(IntendedHeight/6)*2);
        AddButton("resume",() -> Resume(),(IntendedHeight/6)*1);
        AddButton("exit",() -> Exit(),0);



    }
}
