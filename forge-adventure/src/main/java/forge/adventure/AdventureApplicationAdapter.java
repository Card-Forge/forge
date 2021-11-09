package forge.adventure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.ScreenUtils;
import forge.Forge;
import forge.Graphics;
import forge.adventure.scene.ForgeScene;
import forge.adventure.scene.Scene;
import forge.adventure.scene.SceneType;
import forge.adventure.util.Config;
import forge.interfaces.IDeviceAdapter;

/**
 * Application adapter the handle switching and fading between scenes
 */
public class AdventureApplicationAdapter extends Forge {
    public static AdventureApplicationAdapter instance;
    Scene currentScene = null;
    Array<Scene> lastScene = new Array<>();
    private int currentWidth;
    private int currentHeight;
    private float animationTimeout;
    Batch animationBatch;
    Texture transitionTexture;
    TextureRegion lastScreenTexture;
    private boolean sceneWasSwapped =false;
    private Graphics graphics;

    public Graphics getGraphics()
    {
        if(graphics==null)
            graphics=new Graphics();
        return graphics;
    }

    public TextureRegion getLastScreenTexture() {
        return lastScreenTexture;
    }
    public AdventureApplicationAdapter(Clipboard clipboard0, IDeviceAdapter deviceAdapter0, String assetDir0, boolean value, boolean androidOrientation, int totalRAM, boolean isTablet, int AndroidAPI, String AndroidRelease, String deviceName) {
        super(clipboard0, deviceAdapter0, assetDir0, value, androidOrientation,  totalRAM,  isTablet,  AndroidAPI,  AndroidRelease,  deviceName);
        instance = this;
    }

    public int getCurrentWidth() {
        return currentWidth;
    }

    public int getCurrentHeight() {
        return currentHeight;
    }


    public Scene getCurrentScene() {
        return currentScene;
    }

    @Override
    public void resize(int w, int h) {
        currentWidth = w;
        currentHeight = h;
        super.resize(w, h);
    }

    public boolean switchScene(Scene newScene) {

        if (currentScene != null) {
            if (!currentScene.leave())
                return false;
            lastScene.add(currentScene);
        }
        storeScreen();
        sceneWasSwapped =true;
        currentScene = newScene;
        currentScene.enter();
        return true;
    }

    private void storeScreen() {
         if(!(currentScene instanceof ForgeScene))
         {
            if(lastScreenTexture!=null)
                lastScreenTexture.getTexture().dispose();
             lastScreenTexture = ScreenUtils.getFrameBufferTexture();
         }


    }

    public void resLoaded() {
        for (forge.adventure.scene.SceneType entry : SceneType.values()) {
            entry.instance.resLoaded();
        }
        //AdventureApplicationAdapter.CurrentAdapter.switchScene(SceneType.RewardScene.instance);


        switchScene(SceneType.StartScene.instance);
        animationBatch=new SpriteBatch();
        transitionTexture =new Texture(Config.instance().getFile("ui/transition.png"));
    }


    @Override
    public void create() {

        Pixmap pm = new Pixmap(Config.instance().getFile("skin/cursor.png"));
        Gdx.graphics.setCursor(Gdx.graphics.newCursor(pm, 0, 0));
        pm.dispose();
        for (forge.adventure.scene.SceneType entry : SceneType.values()) {
            entry.instance.create();
        }
        super.create();
    }

    @Override
    public void render() {
        float delta=Gdx.graphics.getDeltaTime();
        float transitionTime = 0.2f;
        if(sceneWasSwapped)
        {
            sceneWasSwapped =false;
            animationTimeout= transitionTime;
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            return;
        }
        if(animationTimeout>=0)
        {
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            animationBatch.begin();
            animationTimeout-=delta;
            animationBatch.setColor(1,1,1,1);
            animationBatch.draw(lastScreenTexture,0,0, Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
            animationBatch.setColor(1,1,1,1-(1/ transitionTime)*animationTimeout);
            animationBatch.draw(transitionTexture,0,0, Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
            animationBatch.draw(transitionTexture,0,0, Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
            animationBatch.end();
            if(animationTimeout<0)
            {
                currentScene.render();
                storeScreen();
                Gdx.gl.glClearColor(0, 0, 0, 1);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            }
            else
            {
                return;
            }
        }
        if(animationTimeout>=-transitionTime)
        {
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            animationBatch.begin();
            animationTimeout-=delta;
            animationBatch.setColor(1,1,1,1);
            animationBatch.draw(lastScreenTexture,0,0, Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
            animationBatch.setColor(1,1,1,(1/ transitionTime)*(animationTimeout+ transitionTime));
            animationBatch.draw(transitionTexture,0,0, Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
            animationBatch.draw(transitionTexture,0,0, Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
            animationBatch.end();
            return;
        }
        currentScene.render();
        currentScene.act(delta);
    }

    @Override
    public void dispose() {
        for (forge.adventure.scene.SceneType entry : SceneType.values()) {
            entry.instance.dispose();
        }
        System.exit(0);
    }

    private Scene getLastScene() {
        return lastScene.size==0?null: lastScene.get(lastScene.size-1);
    }

    public Scene switchToLast() {

        if(lastScene.size!=0)
        {
            storeScreen();
            currentScene = lastScene.get(lastScene.size-1);
            currentScene.enter();
            sceneWasSwapped =true;
            lastScene.removeIndex(lastScene.size-1);
            return currentScene;
        }
        return null;
    }

}
