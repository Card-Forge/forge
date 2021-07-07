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

public class NewGameScene extends Scene {
    Texture Background;

    public NewGameScene( ) {
        super();
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
        Stage.getBatch().end();
        Stage.act(Gdx.graphics.getDeltaTime());
        Stage.draw();
        //Batch.end();
    }

    public boolean Start()
    {

        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.GameScene);
        return true;
    }
    public boolean Back()
    {
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(SceneType.StartScene);
        return true;
    }
    @Override
    public void create() {
        Stage = new Stage(new StretchViewport(IntendedWidth,IntendedHeight));
        Background = new Texture(AdventureApplicationAdapter.CurrentAdapter.GetRes().GetFile("img/title_bg.png"));



        TextButton button = new TextButton("Start",AdventureApplicationAdapter.CurrentAdapter.GetRes().GetSkin()) ;
        button.setPosition(100,600);
        button.setSize(400,150);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    Start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }});

        Stage.addActor(button);

        button = new TextButton("Back",AdventureApplicationAdapter.CurrentAdapter.GetRes().GetSkin()) ;
        button.setPosition(100,400);
        button.setSize(400,150);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    Back();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }});
        Stage.addActor(button);


    }
}
