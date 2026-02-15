package forge.adventure.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.SerializationException;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;
import forge.adventure.world.BiomeSprites;
import forge.util.FileUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
/**
 * Data class that will be used to read Json configuration files
 * UIData
 * contains the definition of the world
 */
public class WorldData implements Serializable {

    public int width;
    public int height;
    public float playerStartPosX;
    public float playerStartPosY;
    public float noiseZoomBiome;
    public int tileSize;
    public int miniMapTileSize;
    public BiomeData roadTileset;
    public String biomesSprites;
    public float maxRoadDistance;
    public String[] biomesNames;


    private BiomeSprites sprites;
    private List<BiomeData> biomes;

    private static Array<EnemyData> allEnemies;
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
            if (handle != null && handle.exists()) {
                Array readList = json.fromJson(Array.class, EnemyData.class, handle);
                allEnemies = readList;
            } else {
                String folderPath = Paths.ENEMIES.replace(".json", "/");
                FileHandle folderHandle = Config.instance().getFile(folderPath);
                if (folderHandle.exists() && folderHandle.isDirectory()) {
                    allEnemies = new Array<>();
                    for (FileHandle file : folderHandle.list()) {
                        if (file.extension().equals("json")) {
                            EnemyData enemy = json.fromJson(EnemyData.class, file);
                            allEnemies.add(enemy);
                        }
                    }
                }
            }
        }
        return allEnemies;
    }

    public static EnemyData getEnemy(String enemy) {
        for(EnemyData data: new Array.ArrayIterator<>(getAllEnemies())) {
            if(data.name.equals(enemy))
                return data;
        }
        for(EnemyData data: new Array.ArrayIterator<>(getAllEnemies())) {
            if(data.getName() != null && data.getName().equals(enemy))
                return data;
        }
        return null;
    }

    public static ArrayList<String> getTownNames(String name) {
        return new ArrayList<String>(FileUtil.readFile(Config.instance().getFilePath("world/town_names_" + name + ".txt")));
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
            try
            {
                biomes = new ArrayList<BiomeData>();
                Json json = new Json();
                for (String name : biomesNames) {
                    biomes.add(json.fromJson(BiomeData.class, Config.instance().getFile(name)));
                }
            }
            catch (SerializationException ex) {
                ex.printStackTrace();
            }
        }
        return biomes;
    }

}