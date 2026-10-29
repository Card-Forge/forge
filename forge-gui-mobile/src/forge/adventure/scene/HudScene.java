package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import forge.Forge;
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
    public boolean touchCancelled (int x, int y, int pointer, int button) {
        return touchUp(x, y, pointer, button);
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
        if (hud == null || hud.isLandscape != Forge.isLandscapeMode()) {
            reloadHud();
        }
        resize(Forge.getScreenWidth(), Forge.getScreenHeight());
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
        if (stage != null && stage.getViewport() != null) {
            stage.getViewport().apply();
        }
        stage.draw();
        if (hud != null && hud.getViewport() != null) {
            hud.getViewport().apply(true);
        }
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
        if (Forge.advFreezePlayerControls)
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
        if (Forge.advFreezePlayerControls)
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
        if (Forge.advFreezePlayerControls)
            return false;
        return stage.keyTyped(character);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        Forge.setLastInputWasController(false);
        if (hud.touchDown(screenX, screenY, pointer, button))
            return true;
        if (isInHudOnlyMode())
            return false;
        if (Forge.advFreezePlayerControls)
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
        Forge.setLastInputWasController(false);
        if (hud.touchDragged(screenX, screenY, pointer))
            return true;
        if (isInHudOnlyMode())
            return false;
        if (Forge.advFreezePlayerControls)
            return false;
        return stage.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (hud.mouseMoved(screenX, screenY))
            return true;
        if (isInHudOnlyMode())
            return false;
        if (Forge.advFreezePlayerControls)
            return false;
        return stage.mouseMoved(screenX, screenY);
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (hud.scrolled(amountX, amountY))
            return true;
        if (isInHudOnlyMode())
            return false;
        if (Forge.advFreezePlayerControls)
            return false;
        return stage.scrolled(amountX, amountY);
    }

    @Override
    public boolean axisMoved(Controller controller, int axisIndex, float value) {
        return stage.axisMoved(controller, axisIndex, value);
    }

    @Override
    public void setWinner(boolean winner, boolean isArena) {
        stage.setWinner(winner, isArena);
    }

    public boolean isInHudOnlyMode() {
        return false;
    }

    public void setFromWorldMap(boolean val) {
        hud.fromWorldMap = val;
    }

    public Batch getBatch() {
        return stage.getBatch();
    }

    @Override
    public void resize(int width, int height) {
        float worldW = getWorldWidth(width, height);
        float worldH = getWorldHeight(width, height);
        if (stage != null && stage.getViewport() != null) {
            stage.getViewport().setWorldSize(worldW, worldH);
            stage.getViewport().update(width, height, false);
        }
        if (hud != null && hud.getViewport() != null) {
            hud.getViewport().setWorldSize(worldW, worldH);
            hud.getViewport().update(width, height, true);
            hud.layoutHud(worldW, worldH);
        }
    }

    @Override
    public void onOrientationChanged() {
        int sw = Forge.getScreenWidth();
        int sh = Forge.getScreenHeight();
        float worldW = getWorldWidth(sw, sh);
        float worldH = getWorldHeight(sw, sh);
        if (stage != null && stage.getViewport() != null) {
            stage.getViewport().setWorldSize(worldW, worldH);
            stage.getViewport().update(sw, sh, false);
        }
        reloadHud();
    }

    public void reloadHud() {
        boolean wasDialogShowing = stage != null && stage.isDialogOnlyInput();
        if (hud != null) {
            hud.dispose();
            hud = null;
        }
        hud = GameHUD.reload(stage);
        int sw = Forge.getScreenWidth();
        int sh = Forge.getScreenHeight();
        float worldW = getWorldWidth(sw, sh);
        float worldH = getWorldHeight(sw, sh);
        if (hud != null && hud.getViewport() != null) {
            hud.getViewport().setWorldSize(worldW, worldH);
            hud.getViewport().update(sw, sh, true);
            hud.layoutHud(worldW, worldH);
        }
        if (stage != null) {
            stage.setDialogStage(hud);
            if (wasDialogShowing) {
                stage.showDialog();
            }
        }
    }

    public GameStage getStage() {
        return stage;
    }
}
