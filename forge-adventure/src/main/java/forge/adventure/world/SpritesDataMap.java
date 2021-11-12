package forge.adventure.world;

import com.badlogic.gdx.math.Vector2;
import forge.adventure.data.BiomeSpriteData;
import forge.adventure.util.SaveFileContent;
import forge.adventure.util.SaveFileData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class that hold all sprites as a list for each chunk
 */
public class SpritesDataMap implements SaveFileContent {
    public class BiomeSpriteDataMap extends HashMap<Integer, BiomeSpriteData> implements SaveFileContent
    {
        @Override
        public void load(SaveFileData data) {
            clear();
            List<Integer> keyList=(List<Integer>)data.readObject("keyList");
            for(Integer key:keyList)
            {
                BiomeSpriteData biomeData=new BiomeSpriteData();
                biomeData.load(data.readSubData(key.toString()));
                put(key,biomeData);
            }
        }

        @Override
        public SaveFileData save() {

            SaveFileData data = new SaveFileData();
            List<Integer> keyList=new ArrayList<>();
            for(Entry<Integer, BiomeSpriteData> entry:this.entrySet())
            {
                keyList.add(entry.getKey());
                data.store(entry.getKey().toString(),entry.getValue().save());
            }
            data.storeObject("keyList",keyList);
            return data;
        }
    }
    private final int numberOfChunks;
    BiomeSpriteDataMap objectData = new BiomeSpriteDataMap();
    HashMap<String, Integer> objectKeys = new HashMap<>();
    int tileSize;
    int chunkSize;
    private List<Pair<Vector2, Integer>>[][] mapObjects;

    SpritesDataMap(int chunkSize, int tiles, int numberOfChunks) {
        this.tileSize = tiles;
        this.chunkSize = chunkSize;
        this.numberOfChunks = numberOfChunks;
        mapObjects = new List[numberOfChunks][numberOfChunks];
        for (int x = 0; x < numberOfChunks; x++) {
            for (int y = 0; y < numberOfChunks; y++) {
                mapObjects[x][y] = new ArrayList<Pair<Vector2, Integer>>();
            }
        }
    }



    public BiomeSpriteData get(int id) {
        return objectData.get(id);
    }

    public boolean containsKey(String spriteKey) {
        return objectKeys.containsKey(spriteKey);
    }

    public int put(String key, BiomeSpriteData mapObject, BiomeSprites sprites) {
        int retInt = objectData.size();
        objectData.put(retInt, mapObject);
        objectKeys.put(key, retInt);
        return retInt;
    }

    public int intKey(String spriteKey) {
        return objectKeys.get(spriteKey);
    }

    public void putPosition(int key, Vector2 vector2) {
        int chunkX = (int) ((vector2.x / tileSize) / chunkSize);
        int chunkY = (int) ((vector2.y / tileSize) / chunkSize);
        if (chunkX >= numberOfChunks || chunkY >= numberOfChunks || chunkX < 0 || chunkY < 0)
            return;
        mapObjects[chunkX][chunkY].add(Pair.of(vector2, key));
    }

    public List<Pair<Vector2, Integer>> positions(int chunkX, int chunkY) {
        if (chunkX >= numberOfChunks || chunkY >= numberOfChunks || chunkX < 0 || chunkY < 0)
            return new ArrayList<>();
        return mapObjects[chunkX][chunkY];
    }


    @Override
    public void load(SaveFileData data) {

        objectData.load(data.readSubData("objectData"));
        mapObjects = (List<Pair<Vector2, Integer>>[][])data.readObject("mapObjects");
        objectKeys = (HashMap<String, Integer>)data.readObject("objectKeys");
        tileSize = data.readInt("tileSize");
        chunkSize = data.readInt("chunkSize");
    }

    @Override
    public SaveFileData save() {
        SaveFileData data=new SaveFileData();
        data.store("objectData",objectData.save());
        data.storeObject("mapObjects",mapObjects);
        data.storeObject("objectKeys",objectKeys);
        data.store("tileSize",tileSize);
        data.store("chunkSize",chunkSize);

        return data;
    }

}
