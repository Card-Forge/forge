package forge.adventure.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;

import java.io.Serializable;

/**
 * Data class that will be used to read Json configuration files
 * BiomeData
 * contains the information for the point of interests like towns and dungeons
 */
public class PointOfInterestData implements Serializable {
    public String name;
    public String type;
    public int count;
    public String spriteAtlas;
    public String sprite;
    public String map;
    public float radiusFactor;
    public float offsetX=0f;
    public float offsetY=0f;
    public boolean active = true;
    public String[] questTags = new String[0];




    private static Array<PointOfInterestData> pointOfInterestList;
    public static Array<PointOfInterestData> getAllPointOfInterest() {
        if (pointOfInterestList == null) {
            Json json = new Json();
            FileHandle handle = Config.instance().getFile(Paths.POINTS_OF_INTEREST);
            if (handle.exists()) {
                pointOfInterestList = json.fromJson(Array.class, PointOfInterestData.class, handle);
            }

        }
        return pointOfInterestList;
    }
    public static PointOfInterestData getPointOfInterest(String name) {
        for(PointOfInterestData data: new Array.ArrayIterator<>(getAllPointOfInterest())){
            if(data.name.equals(name)) return data;
        }
        return null;
    }
    public PointOfInterestData()
    {

    }
    public PointOfInterestData(PointOfInterestData other)
    {
        name=other.name;
        type=other.type;
        count=other.count;
        spriteAtlas=other.spriteAtlas;
        sprite=other.sprite;
        map=other.map;
        radiusFactor=other.radiusFactor;
        offsetX=other.offsetX;
        offsetY=other.offsetY;
        active=other.active;
        questTags = other.questTags.clone();
    }
}
