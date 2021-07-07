package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import forge.adventure.stage.GameStage;

public class GameScene extends Scene {

    public GameScene() {

    }

    @Override
    public void dispose() {
        Stage.dispose();
    }

    @Override
    public void render() {

        //Batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0,1,1,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Stage.getBatch().begin();
        Stage.getBatch().end();
        Stage.act(Gdx.graphics.getDeltaTime());
        Stage.draw();
        //Batch.end();
    }

    public boolean NewGame()
    {

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
        Stage = new GameStage();






    }
}

