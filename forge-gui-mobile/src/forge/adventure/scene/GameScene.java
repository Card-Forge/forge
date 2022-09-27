package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import forge.Forge;
import forge.adventure.stage.MapStage;
import forge.adventure.stage.WorldStage;

/**
 * Game scene main over world scene
 * does render the WorldStage and HUD
 */
public class GameScene extends HudScene {
    public GameScene() {
        super(WorldStage.getInstance());

    }


    private static GameScene object;

    public static GameScene instance() {
        if(object==null)
            object=new GameScene();
        return object;
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    @Override
    public void act(float delta) {
        stage.act(delta);
    }

    @Override
    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.draw();
        hud.draw();
    }


    @Override
    public void enter() {
        MapStage.getInstance().clearIsInMap();
        Forge.clearTransitionScreen();
        Forge.clearCurrentScreen();
        super.enter();
        WorldStage.getInstance().handlePointsOfInterestCollision();
    }

}

