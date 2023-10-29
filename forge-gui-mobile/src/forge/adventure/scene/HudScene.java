package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.graphics.GL20;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.GameStage;
import forge.adventure.stage.IAfterMatch;
import forge.adventure.stage.MapStage;
import forge.adventure.util.KeyBinding;

/**
 * Hud base scene
 */
public abstract class HudScene extends Scene implements InputProcessor, IAfterMatch {

    GameHUD hud;
    GameStage stage;

    protected HudScene(GameStage s) {
        stage = s;
        hud = GameHUD.getInstance();
    }

    @Override
    public void connected(final Controller controller) {
        hud.ui.controllerConnected();
    }

    @Override
    public void disconnected(final Controller controller) {
        hud.ui.controllerDisconnected();
    }

    @Override
    public boolean leave() {
        stage.leave();
        return super.leave();
    }

    @Override
    public void enter() {
        Gdx.input.setInputProcessor(this);
        stage.enter();
        hud.enter();
        super.enter();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    @Override
    public void act(float delta) {
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
    public boolean keyDown(int keycode) {
        if (MapStage.getInstance().isDialogOnlyInput()) {
            MapStage.getInstance().stop();
            return MapStage.getInstance().dialogInput(keycode);
        }
        if (hud.keyDown(keycode))
            return true;
        if (isInHudOnlyMode())
            return false;
        return stage.keyDown(keycode);
    }

    @Override
    public boolean keyUp(int keycode) {
        if (MapStage.getInstance().isDialogOnlyInput()) {
            MapStage.getInstance().stop();
            return true;
        }
        if (hud.keyUp(keycode))
            return true;
        if (isInHudOnlyMode())
            return false;
        return stage.keyUp(keycode);
    }

    @Override
    public boolean buttonDown(Controller var1, int var2) {
        return keyDown(KeyBinding.controllerButtonToKey(var1, var2));
    }

    @Override
    public boolean buttonUp(Controller var1, int var2) {
        return keyUp(KeyBinding.controllerButtonToKey(var1, var2));
    }

    @Override
    public boolean keyTyped(char character) {
        if (hud.keyTyped(character))
            return true;
        if (isInHudOnlyMode())
            return false;
        return stage.keyTyped(character);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (hud.touchDown(screenX, screenY, pointer, button))
            return true;
        if (isInHudOnlyMode())
            return false;
        return stage.touchDown(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (hud.touchUp(screenX, screenY, pointer, button))
            return true;
        if (isInHudOnlyMode())
            return false;
        return stage.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (hud.touchDragged(screenX, screenY, pointer))
            return true;
        if (isInHudOnlyMode())
            return false;
        return stage.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (hud.mouseMoved(screenX, screenY))
            return true;
        if (isInHudOnlyMode())
            return false;
        return stage.mouseMoved(screenX, screenY);
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (hud.scrolled(amountX, amountY))
            return true;
        if (isInHudOnlyMode())
            return false;
        return stage.scrolled(amountX, amountY);
    }

    @Override
    public boolean axisMoved(Controller controller, int axisIndex, float value) {
        return stage.axisMoved(controller, axisIndex, value);
    }

    @Override
    public void setWinner(boolean winner) {
        stage.setWinner(winner);
    }

    public boolean isInHudOnlyMode() {
        return false;
    }
}
