package forge.adventure.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import javafx.util.Pair;

import java.util.HashMap;

public class MapObjectMap
{

    private Array<Pair<Vector2,Integer>>[][] mapObjects;
    HashMap<Integer, MapObject> objectIds=new HashMap<>();
    HashMap<String,Integer> objectKeys=new HashMap<>();
    int tileSize;
    int chunkSize;
    MapObjectMap(int chunkSize,int tiles)
    {
        this.tileSize=tiles;
        this.chunkSize=chunkSize;
        mapObjects=new Array[chunkSize][chunkSize];
        for(int x=0;x<chunkSize;x++)
        {
            for(int y=0;y<chunkSize;y++)
            {
                mapObjects[x][y]=new Array<>();
            }
        }
    }

    public MapObject get(int id) {
        return objectIds.get(id);
    }

    public boolean containsKey(String spriteKey)
    {
        return objectKeys.containsKey(spriteKey);
    }

    public int put(String key, MapObject mapObject) {
        int retInt=objectIds.size();
        objectIds.put(retInt,mapObject);
        objectKeys.put(key,retInt);
        return retInt;
    }

    public int intKey(String spriteKey) {
        return objectKeys.get(spriteKey);
    }

    public void putPosition(int key, Vector2 vector2)
    {
        int chunkX=(int)((vector2.x/tileSize)/chunkSize);
        int chunkY=(int)((vector2.y/tileSize)/chunkSize);
        if(chunkX>=chunkSize||chunkY>=chunkSize||chunkX<0||chunkY<0)
            return;
        mapObjects[chunkX][chunkY].add(new Pair<>(vector2,key));
    }

    public Array<Pair<Vector2,Integer>> positions(int chunkX, int chunky) {
        return mapObjects[chunkX][chunky];
    }
}
