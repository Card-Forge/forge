package forge.adventure.world;

import com.badlogic.gdx.math.Vector2;
import forge.adventure.data.BiomeSpriteData;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class that hold all sprites as a list for each chunk
 */
public class SpritesDataMap implements Serializable {

    private final int numberOfChunks;
    HashMap<Integer, BiomeSpriteData> objectData = new HashMap<>();
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

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {

        out.writeObject(mapObjects);
        out.writeObject(objectData);
        out.writeObject(objectKeys);
        out.writeInt(tileSize);
        out.writeInt(chunkSize);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

        mapObjects = (List<Pair<Vector2, Integer>>[][]) in.readObject();
        objectData = (HashMap<Integer, BiomeSpriteData>) in.readObject();
        objectKeys = (HashMap<String, Integer>) in.readObject();
        tileSize = in.readInt();
        chunkSize = in.readInt();


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
}
