package forge.adventure.scene;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.libgdxgui.screens.FScreen;
import forge.adventure.libgdxgui.toolbox.FDisplayObject;

import java.util.List;

public abstract class Scene implements Disposable {

    public Scene() {

    }

    public static int GetIntendedWidth() {
        return AdventureApplicationAdapter.CurrentAdapter.GetRes().GetConfigData().screenWidth;
    }

    public static int GetIntendedHeight() {
        return AdventureApplicationAdapter.CurrentAdapter.GetRes().GetConfigData().screenHeight;
    }

    public abstract void render();

    public void create() {

    }

    public Drawable DrawableImage(String path) {
        return new TextureRegionDrawable(new Texture(AdventureApplicationAdapter.CurrentAdapter.GetRes().GetFile(path)));
    }

    public void ResLoaded() {
    }

    public boolean Leave() {
        return true;
    }

    public void Enter() {
    }

    public void buildTouchListeners(int x, int y, List<FDisplayObject> potentialListeners) {

    }

    public FScreen forgeScreen() {
        return null;
    }

}
