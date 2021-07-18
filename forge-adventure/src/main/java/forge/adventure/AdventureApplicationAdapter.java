package forge.adventure;

import com.badlogic.gdx.ApplicationAdapter;
import forge.Graphics;
import forge.adventure.scene.Scene;
import forge.adventure.scene.SceneType;
import forge.adventure.util.Res;

public class AdventureApplicationAdapter extends ApplicationAdapter {
    public static AdventureApplicationAdapter CurrentAdapter;
    String strPlane;
    Scene currentScene=null;
    Scene lastScene=null;
    Res resourcesLoader;
    private int currentWidth;
    private int currentHeight;
    private Graphics graphics;

    public int getCurrentWidth(){return currentWidth;}
    public int getCurrentHeight(){return currentHeight;}
    public Graphics getGraphics(){return graphics;}
    @Override
    public void resize(int w,int h)
    {
        currentWidth=w;
        currentHeight=h;
        super.resize(w,h);
    }
    public AdventureApplicationAdapter(String plane) {
        CurrentAdapter=this;
        strPlane=plane;
    }
    public boolean SwitchScene(Scene newScene)
    {
        if(currentScene!=null)
        {
            if(!currentScene.Leave())
                return false;
        }
        lastScene=currentScene;
        currentScene=newScene;
        currentScene.Enter();
        return true;
    }
    public void ResLoaded()
    {
        for( forge.adventure.scene.SceneType entry:SceneType.values())
        {
            entry.instance.ResLoaded();
        }

    }
    public Res GetRes()
    {
        return resourcesLoader;
    }
    @Override
    public void create ()
    {
        graphics = new Graphics();
        resourcesLoader=new Res(strPlane);
        for( forge.adventure.scene.SceneType entry:SceneType.values())
        {
            entry.instance.create();
        }
        SwitchScene(SceneType.StartScene.instance);
    }
    @Override
    public void render(){
    currentScene.render();
    }
    @Override
    public void dispose(){
        currentScene.dispose();
    }

    public Scene GetLastScene() {
        return lastScene;
    }

    public void SwitchToLast() {
        SwitchScene(lastScene);
    }
}
