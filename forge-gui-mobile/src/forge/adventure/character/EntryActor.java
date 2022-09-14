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
                    stage.getPlayerSprite().setPosition(x+w/2-stage.getPlayerSprite().getWidth()/2,y+h);
                    break;
                case "down":
                    stage.getPlayerSprite().setPosition(x+w/2-stage.getPlayerSprite().getWidth()/2,y-stage.getPlayerSprite().getHeight());
                    break;
                case "right":
                    stage.getPlayerSprite().setPosition(x-stage.getPlayerSprite().getWidth(),y+h/2-stage.getPlayerSprite().getHeight()/2);
                    break;
                case "left":
                    stage.getPlayerSprite().setPosition(x+w,y+h/2-stage.getPlayerSprite().getHeight()/2);
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

