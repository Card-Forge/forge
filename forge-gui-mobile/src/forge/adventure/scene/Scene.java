package forge.adventure.scene;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import forge.adventure.util.Config;

/**
 * Base class for all rendered scenes
 */
public abstract class Scene implements Disposable {

    public Scene() {

    }

    public static int GetIntendedWidth() {
        return Config.instance().getConfigData().screenWidth;
    }

    public static int GetIntendedHeight() {
        return Config.instance().getConfigData().screenHeight;
    }

    public abstract void act(float delta);
    public abstract void render();

    public void create() {

    }

    public Drawable DrawableImage(String path) {
        return new TextureRegionDrawable(new Texture(Config.instance().getFile(path)));
    }

    public void resLoaded() {
    }

    public boolean leave() {
        return true;
    }

    public void enter() {
    }



}
