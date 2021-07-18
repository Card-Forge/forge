package forge.adventure.scene;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import forge.adventure.AdventureApplicationAdapter;

public abstract class Scene implements Disposable {

    public static int IntendedWidth = 1920;
    public static int IntendedHeight = 1080;
    public Scene()
    {

    }
    public abstract void render() ;

    public abstract void create();

    public Drawable DrawableImage(String path)
    {
        return new TextureRegionDrawable(new Texture(AdventureApplicationAdapter.CurrentAdapter.GetRes().GetFile(path)));
    }

    public void ResLoaded()
    {
    }
    public boolean Leave(){return true;}
    public void Enter()
    {
    }

}
