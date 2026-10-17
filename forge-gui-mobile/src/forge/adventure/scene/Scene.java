package forge.adventure.scene;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import forge.Forge;
import forge.adventure.util.Config;
import forge.adventure.util.ShaderDrawable;
import forge.sound.SoundSystem;
import forge.util.ShaderUtil;

/**
 * Base class for all rendered scenes
 */
public abstract class Scene implements Disposable {

    static class SceneControllerListener implements ControllerListener {
        // On iOS, IosControllerManager fires connected() during Controllers
        // initialization for already-present controllers — before any scene
        // exists, so Forge.getCurrentScene() can still be null here.

        @Override
        public void connected(Controller controller) {
            Scene scene = Forge.getCurrentScene();
            if (scene != null) {
                scene.connected(controller);
            }
        }

        @Override
        public void disconnected(Controller controller) {
            Scene scene = Forge.getCurrentScene();
            if (scene != null) {
                scene.disconnected(controller);
            }
        }

        @Override
        public boolean buttonDown(Controller controller, int i) {
            if (!SoundSystem.instance.hasWindowFocus()) {
                return false;
            }
            Scene scene = Forge.getCurrentScene();
            return scene != null && scene.buttonDown(controller, i);
        }

        @Override
        public boolean buttonUp(Controller controller, int i) {
            if (!SoundSystem.instance.hasWindowFocus()) {
                return false;
            }
            Scene scene = Forge.getCurrentScene();
            return scene != null && scene.buttonUp(controller, i);
        }

        @Override
        public boolean axisMoved(Controller controller, int i, float v) {
            if (!SoundSystem.instance.hasWindowFocus()) {
                return false;
            }
            Scene scene = Forge.getCurrentScene();
            return scene != null && scene.axisMoved(controller, i, v);
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
        int w = Config.instance().getConfigData() != null ? Config.instance().getConfigData().screenWidth : 480;
        int h = Config.instance().getConfigData() != null ? Config.instance().getConfigData().screenHeight : 270;
        return Forge.isLandscapeMode() ? w : h;
    }

    public static int getIntendedHeight() {
        int w = Config.instance().getConfigData() != null ? Config.instance().getConfigData().screenWidth : 480;
        int h = Config.instance().getConfigData() != null ? Config.instance().getConfigData().screenHeight : 270;
        return Forge.isLandscapeMode() ? h : w;
    }

    public static float getWorldWidth(int screenW, int screenH) {
        float aspect = (screenW > 0 && screenH > 0) ? (float) screenW / (float) screenH : (Forge.isLandscapeMode() ? 16f / 9f : 9f / 16f);
        float baseW = getIntendedWidth();
        float baseH = getIntendedHeight();
        float baseAspect = baseW / baseH;
        if (aspect >= baseAspect) {
            return baseH * aspect;
        } else {
            return baseW;
        }
    }

    public static float getWorldHeight(int screenW, int screenH) {
        float aspect = (screenW > 0 && screenH > 0) ? (float) screenW / (float) screenH : (Forge.isLandscapeMode() ? 16f / 9f : 9f / 16f);
        float baseW = getIntendedWidth();
        float baseH = getIntendedHeight();
        float baseAspect = baseW / baseH;
        if (aspect >= baseAspect) {
            return baseH;
        } else {
            return baseW / aspect;
        }
    }

    public static float getWorldWidth() {
        return getWorldWidth(Forge.getScreenWidth(), Forge.getScreenHeight());
    }

    public static float getWorldHeight() {
        return getWorldHeight(Forge.getScreenWidth(), Forge.getScreenHeight());
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

    public void onOrientationChanged() {

    }

    private ShaderDrawable lastPreviewDrawable, lastScreenshotDrawable;
    private float uniformWidth = 0;
    private float uniformHeight = 0;
    private float uniformPixelSize = 0;
    // move here so Scene can access it
    public ShaderDrawable getLastPreviewDrawable(TextureRegion region) {
        float width = getIntendedWidth();
        float height = getIntendedHeight();
        float mul = 1.2f;
        float pixelSize = width > height ? (width / height) * mul : (height / width) * mul;

        // sync to local vars
        this.uniformWidth = width;
        this.uniformHeight = height;
        this.uniformPixelSize = pixelSize;

        if (lastPreviewDrawable == null) {
            lastPreviewDrawable = new ShaderDrawable(ShaderUtil.getInstance().getShaderPix());
            lastPreviewDrawable.setUniformSetter(shader -> {
                shader.setUniformf("u_resolution", this.uniformWidth, this.uniformHeight);
                shader.setUniformf("u_pixelSize", this.uniformPixelSize);
                shader.setUniformf("u_bias", 0.8f);
            });
        }

        lastPreviewDrawable.setRegion(region);
        return lastPreviewDrawable;
    }
}
