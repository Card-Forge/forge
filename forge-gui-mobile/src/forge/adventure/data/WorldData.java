package forge.adventure.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;
import forge.adventure.world.BiomeSprites;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
/**
 * Data class that will be used to read Json configuration files
 * UIData
 * contains the definition of the world
 */
public class WorldData implements Serializable {

    static Array<EnemyData> allEnemies;
    public int width;
    public int height;
    public float playerStartPosX;
    public float playerStartPosY;
    public float noiseZoomBiome;
    public int tileSize;
    public List<String> biomesNames;
    public BiomeData roadTileset;
    public String biomesSprites;
    public float maxRoadDistance;
    private BiomeSprites sprites;
    private List<BiomeData> biomes;
    private static Array<ShopData> shopList;


    public static Array<ShopData> getShopList() {
        if (shopList == null) {
            shopList = new Array<>();
            Json json = new Json();
            FileHandle handle = Config.instance().getFile(Paths.SHOPS);
            if (handle.exists())
            {

                Array readList = json.fromJson(Array.class, ShopData.class, handle);
                shopList = readList;
            }
        }
        return shopList;
    }
    static public Array<EnemyData> getAllEnemies() {
        if (allEnemies == null) {
            Json json = new Json();
            FileHandle handle = Config.instance().getFile(Paths.ENEMIES);
            if (handle.exists())
            {

                Array readList =  json.fromJson(Array.class, EnemyData.class, handle);
                allEnemies = readList;
            }
        }
        return allEnemies;
    }

    public static EnemyData getEnemy(String enemy) {
        for(EnemyData data: new Array.ArrayIterator<>(getAllEnemies()))
        {
            if(data.name.equals(enemy))
                return data;
        }
        return null;
    }

    public BiomeSprites GetBiomeSprites() {
        if (sprites == null) {
            Json json = new Json();
            sprites = (json.fromJson(BiomeSprites.class, Config.instance().getFile(biomesSprites)));
        }
        return sprites;
    }

    public List<BiomeData> GetBiomes() {
        if (biomes == null) {
            biomes = new ArrayList<BiomeData>();
            Json json = new Json();
            for (String name : biomesNames) {
                biomes.add(json.fromJson(BiomeData.class, Config.instance().getFile(name)));
            }
        }
        return biomes;
    }


}