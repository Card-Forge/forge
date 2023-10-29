package forge.adventure.character;

import forge.adventure.scene.TileMapScene;
import forge.adventure.stage.MapStage;

/**
 * EntryActor
 * Used to teleport the player in and out of the map
 */
public class EntryActor extends MapActor{
    final MapStage stage;
    String targetMap;
    float x;
    float y;
    float w;
    float h;
    String direction;
    String currentMap;
    int entryTargetObject;

    public EntryActor(MapStage stage, int id,String targetMap,float x,float y,float w,float h,String direction, String currentMap, int entryTargetObject)
    {
        super(id);
        this.stage = stage;
        this.targetMap = targetMap;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.currentMap = currentMap;
        this.entryTargetObject = entryTargetObject;

        this.direction = direction;
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
            stage.exitDungeon();
        }
        else
        {
            if (targetMap.equals(currentMap))
            {
                stage.spawn(entryTargetObject);
            }
            else
            {
                currentMap = targetMap;
                TileMapScene.instance().loadNext(targetMap, entryTargetObject);
            }
        }
    }

    public void spawn() {
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

