package forge.adventure;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import forge.adventure.libgdxgui.Graphics;
import forge.adventure.scene.Scene;
import forge.adventure.scene.SceneType;
import forge.adventure.util.Res;

public class AdventureApplicationAdapter extends ApplicationAdapter {
    public static AdventureApplicationAdapter CurrentAdapter;
    String strPlane;
    Scene currentScene = null;
    Scene lastScene = null;
    Res resourcesLoader;
    private int currentWidth;
    private int currentHeight;
    private Graphics graphics;

    public AdventureApplicationAdapter(String plane) {
        CurrentAdapter = this;
        strPlane = plane;
    }

    public int getCurrentWidth() {
        return currentWidth;
    }

    public int getCurrentHeight() {
        return currentHeight;
    }

    public Graphics getGraphics() {
        return graphics;
    }

    public Scene getCurrentScene() {
        return currentScene;
    }

    @Override
    public void resize(int w, int h) {
        currentWidth = w;
        currentHeight = h;
        StartAdventure.app.resize(w, h);
        super.resize(w, h);
    }

    public boolean SwitchScene(Scene newScene) {
        if (currentScene != null) {
            if (!currentScene.Leave())
                return false;
        }
        lastScene = currentScene;
        currentScene = newScene;
        currentScene.Enter();
        return true;
    }

    public void ResLoaded() {
        for (forge.adventure.scene.SceneType entry : SceneType.values()) {
            entry.instance.ResLoaded();
        }
        SwitchScene(SceneType.StartScene.instance);

    }

    public Res GetRes() {
        return resourcesLoader;
    }

    @Override
    public void create() {
        graphics = new Graphics();
        resourcesLoader = new Res(strPlane);

        Pixmap pm = new Pixmap(Res.CurrentRes.GetFile("skin/cursor.png"));
        Gdx.graphics.setCursor(Gdx.graphics.newCursor(pm, 0, 0));
        pm.dispose();
        for (forge.adventure.scene.SceneType entry : SceneType.values()) {
            entry.instance.create();
        }
    }

    @Override
    public void render() {
        currentScene.render();
    }

    @Override
    public void dispose() {
        for (forge.adventure.scene.SceneType entry : SceneType.values()) {
            entry.instance.dispose();
        }
    }

    public Scene GetLastScene() {
        return lastScene;
    }

    public void SwitchToLast() {
        SwitchScene(lastScene);
    }
}
