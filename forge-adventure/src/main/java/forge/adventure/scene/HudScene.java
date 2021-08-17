package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.GameStage;

public class HudScene extends Scene implements InputProcessor {

    GameHUD hud;
    GameStage stage;

    protected HudScene(GameStage s) {
        stage = s;
        hud = GameHUD.getInstance();
    }

    @Override
    public boolean Leave() {
        stage.Leave();
        return true;
    }

    @Override
    public void Enter() {
        Gdx.input.setInputProcessor(this);
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


        //Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling? GL20.GL_COVERAGE_BUFFER_BIT_NV:0));
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.draw();

        //stage.getBatch().setProjectionMatrix(hud.getStage().getCamera().combined); //set the spriteBatch to draw what our stageViewport sees
        hud.draw();

        //Batch.end();
    }

    @Override
    public void ResLoaded() {


    }

    @Override
    public boolean keyDown(int keycode) {

        if (hud.keyDown(keycode))
            return true;
        return stage.keyDown(keycode);
    }

    @Override
    public boolean keyUp(int keycode) {

        if (hud.keyUp(keycode))
            return true;
        return stage.keyUp(keycode);
    }

    @Override
    public boolean keyTyped(char character) {

        if (hud.keyTyped(character))
            return true;
        return stage.keyTyped(character);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (hud.touchDown(screenX, screenY, pointer, button))
            return true;
        return stage.touchDown(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (hud.touchUp(screenX, screenY, pointer, button))
            return true;
        return stage.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (hud.touchDragged(screenX, screenY, pointer))
            return true;
        return stage.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (hud.mouseMoved(screenX, screenY))
            return true;
        return stage.mouseMoved(screenX, screenY);
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (hud.scrolled(amountX, amountY))
            return true;
        return stage.scrolled(amountX, amountY);
    }
}