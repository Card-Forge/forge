package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import forge.Forge;
import forge.adventure.stage.WorldStage;

/**
 * Game scene main over world scene
 * does render the WorldStage and HUD
 */
public class GameScene extends HudScene {
    private float cameraWidth = 0f, cameraHeight = 0f;
    boolean init;
    public GameScene() {
        super(WorldStage.getInstance());
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
    public void resLoaded() {
        if (!this.init) {
            //set initial camera width and height
            if (cameraWidth == 0f)
                cameraWidth = stage.getCamera().viewportWidth;
            if (cameraHeight == 0f)
                cameraHeight = stage.getCamera().viewportHeight;
            this.init = true;
        }
    }

    @Override
    public void enter() {
        Forge.clearTransitionScreen();
        Forge.clearCurrentScreen();
        if (!Forge.isLandscapeMode()) {
            //Trick: switch the camera viewport width and height so it looks normal since we shrink the width for portrait mode for WorldStage
            stage.getCamera().viewportHeight = cameraWidth;
            stage.getCamera().viewportWidth = cameraHeight;
            ((OrthographicCamera)stage.getCamera()).zoom = 0.85f;
        }
        super.enter();
    }

}

