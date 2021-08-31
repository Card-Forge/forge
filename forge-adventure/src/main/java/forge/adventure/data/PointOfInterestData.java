package forge.adventure.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;

public class PointOfInterestData {
    public String name;
    public String type;
    public int count;
    public String spriteAtlas;
    public String sprite;
    public String map;
    public float radiusOffset;
    public float radiusFactor;



    private static Array<PointOfInterestData> pointOfInterestList;
    public static Array<PointOfInterestData> getAllPointOfInterest() {
        if (pointOfInterestList == null) {
            Json json = new Json();
            FileHandle handle = forge.adventure.util.Res.CurrentRes.GetFile("world/pointsOfInterest.json");
            if (handle.exists()) {
                pointOfInterestList = json.fromJson(Array.class, PointOfInterestData.class, handle);

            }

        }
        return pointOfInterestList;
    }
    public static PointOfInterestData getPointOfInterest(String name) {
        for(PointOfInterestData data:getAllPointOfInterest())
        {
            if(data.name.equals(name))
                return data;
        }
        return null;
    }
}
