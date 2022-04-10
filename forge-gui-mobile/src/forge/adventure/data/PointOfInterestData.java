package forge.adventure.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;

import java.util.ArrayList;

/**
 * Data class that will be used to read Json configuration files
 * BiomeData
 * contains the information for the point of interests like towns and dungeons
 */
public class PointOfInterestData {
    public String name;
    public String type;
    public int count;
    public String spriteAtlas;
    public String sprite;
    public String map;
    public float radiusFactor;



    private static Array<PointOfInterestData> pointOfInterestList;
    public static Array<PointOfInterestData> getAllPointOfInterest() {
        if (pointOfInterestList == null) {
            Json json = new Json();
            FileHandle handle = Config.instance().getFile(Paths.POINTS_OF_INTEREST);
            if (handle.exists()) {
                Array readJson = json.fromJson(Array.class, PointOfInterestData.class, handle);
                pointOfInterestList = readJson;

            }

        }
        return pointOfInterestList;
    }
    public static PointOfInterestData getPointOfInterest(String name) {
        ArrayList<PointOfInterestData> candidates = new ArrayList<PointOfInterestData>();
        for(PointOfInterestData data: new Array.ArrayIterator<>(getAllPointOfInterest())){
            if(data.name.equals(name)) candidates.add(data); //Populate candidates with specified name.
        }
        if(candidates.size() > 0){ //If we got any candidates, return a random one.
            return candidates.get((int) (Math.random() * candidates.size()));
        }
        return null;
    }
}
