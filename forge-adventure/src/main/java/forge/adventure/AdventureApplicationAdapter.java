package forge.adventure;

import com.badlogic.gdx.ApplicationAdapter;
import forge.adventure.scene.*;
import forge.adventure.util.Res;

import java.util.HashMap;

public class AdventureApplicationAdapter extends ApplicationAdapter {
    public static AdventureApplicationAdapter CurrentAdapter;
    String strPlane;
    Scene currentScene=null;
    HashMap<SceneType,Scene> allScenes= new HashMap<>();
    Res resourcesLoader;
    public AdventureApplicationAdapter(String plane) {
        CurrentAdapter=this;
        strPlane=plane;
        allScenes.put(SceneType.StartScene,new StartScene());
        allScenes.put(SceneType.NewGameScene,new NewGameScene());
        allScenes.put(SceneType.GameScene,new GameScene());
        allScenes.put(SceneType.DuelScene,new DuelScene());
    }
    public boolean SwitchScene(SceneType newScene)
    {
        if(currentScene!=null)
        {
            if(!currentScene.Leave())
                return false;
        }
        currentScene=allScenes.get(newScene);
        currentScene.Enter();
        return true;
    }
    public Res GetRes()
    {
        return resourcesLoader;
    }
    @Override
    public void create ()
    {
        resourcesLoader=new Res(strPlane);
        for(HashMap.Entry<SceneType, Scene>  entry:allScenes.entrySet())
        {
            entry.getValue().create();
        }
        SwitchScene(SceneType.StartScene);
    }
    @Override
    public void render(){
    currentScene.render();
    }
    @Override
    public void dispose(){
        currentScene.dispose();
    }
}
