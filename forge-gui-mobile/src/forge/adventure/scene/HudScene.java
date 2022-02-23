package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.GameStage;

/**
 * Hud base scene
 */
public class HudScene extends Scene implements InputProcessor {

    GameHUD hud;
    GameStage stage;

    protected HudScene(GameStage s) {
        stage = s;
        hud = GameHUD.getInstance();
    }

    @Override
    public boolean leave() {
        stage.leave();
        return true;
    }

    @Override
    public void enter() {
        Gdx.input.setInputProcessor(this);
        stage.enter();
        hud.enter();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    @Override
    public void act(float delta)
    {
        stage.act(delta);
        hud.act(delta);
    }
    @Override
    public void render() {

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.draw();
        hud.draw();

    }

    @Override
    public void resLoaded() {


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