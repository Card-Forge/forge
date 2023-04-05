package forge.adventure.scene;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Disposable;
import forge.Forge;
import forge.adventure.util.Config;

/**
 * Base class for all rendered scenes
 */
public abstract class Scene implements Disposable {

    static class SceneControllerListener implements ControllerListener {

        @Override
        public void connected(Controller controller) {
            Forge.getCurrentScene().connected(controller);
        }

        @Override
        public void disconnected(Controller controller) {
            Forge.getCurrentScene().disconnected(controller);
        }

        @Override
        public boolean buttonDown(Controller controller, int i) {
            return Forge.getCurrentScene().buttonDown(controller, i);
        }

        @Override
        public boolean buttonUp(Controller controller, int i) {
            return Forge.getCurrentScene().buttonUp(controller, i);
        }

        @Override
        public boolean axisMoved(Controller controller, int i, float v) {
            return Forge.getCurrentScene().axisMoved(controller, i, v);
        }
    }

    static private SceneControllerListener listener = null;

    public Scene() {
        if (listener == null) {
            listener = new SceneControllerListener();
            Controllers.addListener(listener);
        }
    }

    public static int getIntendedWidth() {
        return Forge.isLandscapeMode() ? Config.instance().getConfigData().screenWidth : Config.instance().getConfigData().screenHeight;
    }

    public static int getIntendedHeight() {
        return Forge.isLandscapeMode() ? Config.instance().getConfigData().screenHeight : Config.instance().getConfigData().screenWidth;
    }

    public abstract void act(float delta);

    public abstract void render();

    public boolean leave() {
        return true;
    }

    public void enter() {

    }


    public void connected(Controller controller) {

    }

    public void disconnected(Controller controller) {

    }

    public boolean buttonDown(Controller controller, int buttonIndex) {
        return false;
    }

    public boolean buttonUp(Controller controller, int buttonIndex) {
        return false;
    }

    public boolean axisMoved(Controller controller, int axisIndex, float value) {
        return true;
    }

    public void updateInput() {

    }
    public void resize(int width, int height) {

    }


}
