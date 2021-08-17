package forge.adventure.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WorldData implements Serializable {

    static Array<EnemyData> allEnemies;
    public int width;
    public int height;
    public float playerStartPosX;
    public float playerStartPosY;
    public float noiceZoomBiom;
    public float noiceZoomObject;
    public int tileSize;
    public List<String> biomNames;
    public String roadTileset;
    public String biomSprites;
    public float maxRoadDistance;
    private BiomSprites sprites;
    private List<BiomData> bioms;

    static public Array<EnemyData> getAllEnemies() {
        if (allEnemies == null) {
            Json json = new Json();
            FileHandle handle = forge.adventure.util.Res.CurrentRes.GetFile("world/enemies.json");
            if (handle.exists())
                allEnemies = json.fromJson(Array.class, EnemyData.class, handle);
        }
        return allEnemies;
    }

    public BiomSprites GetBiomSprites() {
        if (sprites == null) {
            Json json = new Json();
            sprites = (json.fromJson(BiomSprites.class, forge.adventure.util.Res.CurrentRes.GetFile(biomSprites)));
        }
        return sprites;
    }

    public List<BiomData> GetBioms() {
        if (bioms == null) {
            bioms = new ArrayList<BiomData>();
            Json json = new Json();
            for (String name : biomNames) {
                bioms.add(json.fromJson(BiomData.class, forge.adventure.util.Res.CurrentRes.GetFile(name)));
            }
        }
        return bioms;
    }


}