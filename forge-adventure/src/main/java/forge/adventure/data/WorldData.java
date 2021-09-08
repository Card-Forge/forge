package forge.adventure.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WorldData implements Serializable {

    static Array<EnemyData> allEnemies;
    public int width;
    public int height;
    public float playerStartPosX;
    public float playerStartPosY;
    public float noiseZoomBiom;
    public int tileSize;
    public List<String> biomNames;
    public BiomData roadTileset;
    public String biomSprites;
    public float maxRoadDistance;
    private BiomSprites sprites;
    private List<BiomData> bioms;
    private static Array<ShopData> shopList;


    public static Array<ShopData> getShopList() {
        if (shopList == null) {
            shopList = new Array<ShopData>();
            Json json = new Json();
            FileHandle handle = Config.instance().getFile(Paths.Shops);
            if (handle.exists())
                shopList = json.fromJson(Array.class, ShopData.class, handle);
        }
        return shopList;
    }
    static public Array<EnemyData> getAllEnemies() {
        if (allEnemies == null) {
            Json json = new Json();
            FileHandle handle = Config.instance().getFile(Paths.EnemyPath);
            if (handle.exists())
                allEnemies = json.fromJson(Array.class, EnemyData.class, handle);
        }
        return allEnemies;
    }

    public static EnemyData getEnemy(String enemy) {
        for(EnemyData data:getAllEnemies())
        {
            if(data.name.equals(enemy))
                return data;
        }
        return null;
    }

    public BiomSprites GetBiomSprites() {
        if (sprites == null) {
            Json json = new Json();
            sprites = (json.fromJson(BiomSprites.class, Config.instance().getFile(biomSprites)));
        }
        return sprites;
    }

    public List<BiomData> GetBioms() {
        if (bioms == null) {
            bioms = new ArrayList<BiomData>();
            Json json = new Json();
            for (String name : biomNames) {
                bioms.add(json.fromJson(BiomData.class, Config.instance().getFile(name)));
            }
        }
        return bioms;
    }


}