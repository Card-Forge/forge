package forge.adventure.character;

import forge.adventure.scene.SceneType;
import forge.adventure.scene.TileMapScene;
import forge.adventure.stage.MapStage;

/**
 * EntryActor
 * Used to teleport the player in and out of the map
 */
public class EntryActor extends MapActor{
    private final MapStage stage;
    private final int id;
    String targetMap;

    public EntryActor(MapStage stage,String sourceMap, int id,String targetMap,float x,float y,float w,float h,String direction)
    {
        this.stage = stage;
        this.id = id;
        this.targetMap = targetMap;


        if((targetMap==null||targetMap.isEmpty()&&sourceMap.isEmpty())||//if target is null and "from world"
            !sourceMap.isEmpty()&&targetMap.equals(sourceMap))          //or if source is this target
        {
            switch(direction)
            {
                case "up":
                    stage.GetPlayer().setPosition(x,y+h);
                    break;
                case "down":
                    stage.GetPlayer().setPosition(x,y-stage.GetPlayer().getHeight());
                    break;
                case "right":
                    stage.GetPlayer().setPosition(x-stage.GetPlayer().getWidth(),y);
                    break;
                case "left":
                    stage.GetPlayer().setPosition(x+w,y);
                    break;

            }
        }

    }

    public MapStage getMapStage()
    {
        return stage;
    }

    @Override
    public void  onPlayerCollide()
    {
        if(targetMap==null||targetMap.isEmpty())
        {
            stage.exit();
        }
        else
        {
            ((TileMapScene)SceneType.TileMapScene.instance).loadNext(targetMap);
        }
    }

    public int getObjectID() {
        return id;
    }
}

