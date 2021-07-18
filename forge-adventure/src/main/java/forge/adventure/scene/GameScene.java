package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.GameStage;

public class GameScene extends Scene {

    GameStage stage;
    GameHUD hud;
    public GameScene() {

    }

    @Override
    public void Enter()
    {
        Gdx.input.setInputProcessor(stage);
        stage.Enter();
        hud.Enter();
    }
    @Override
    public void dispose() {
        stage.dispose();
    }

    @Override
    public void render() {

        //Batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());



        Gdx.gl.glClearColor(0,1,1,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.draw();

        //stage.getBatch().setProjectionMatrix(hud.getStage().getCamera().combined); //set the spriteBatch to draw what our stageViewport sees
        hud.draw();

        //Batch.end();
    }

    Texture Background;
    @Override
    public void create() {
        stage = new GameStage();
        hud = new GameHUD(stage);

        Background = new Texture(AdventureApplicationAdapter.CurrentAdapter.GetRes().GetFile("img/title_bg.png"));




    }
}

