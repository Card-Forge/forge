package forge.adventure.character;

import forge.adventure.scene.TileMapScene;
import forge.adventure.stage.MapStage;

/**
 * EntryActor
 * Used to teleport the player in and out of the map
 */
public class EntryActor extends MapActor{
    private final MapStage stage;
    String targetMap;

    public EntryActor(MapStage stage, int id,String targetMap,float x,float y,float w,float h,String direction,boolean spawnPlayerThere)
    {
        super(id);
        this.stage = stage;
        this.targetMap = targetMap;


        if(spawnPlayerThere)          //or if source is this target
        {
            switch(direction)
            {
                case "up":
                    stage.GetPlayer().setPosition(x+w/2-stage.GetPlayer().getWidth()/2,y+h);
                    break;
                case "down":
                    stage.GetPlayer().setPosition(x+w/2-stage.GetPlayer().getWidth()/2,y-stage.GetPlayer().getHeight());
                    break;
                case "right":
                    stage.GetPlayer().setPosition(x-stage.GetPlayer().getWidth(),y+h/2-stage.GetPlayer().getHeight()/2);
                    break;
                case "left":
                    stage.GetPlayer().setPosition(x+w,y+h/2-stage.GetPlayer().getHeight()/2);
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
            TileMapScene.instance().loadNext(targetMap);
        }
    }

}

