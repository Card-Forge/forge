package forge.adventure.data;

import com.badlogic.gdx.utils.Json;

import java.util.ArrayList;
import java.util.List;
public class WorldData
{

    public int width;
    public int height;

    public float playerStartPosX;
    public float playerStartPosY;

    public int tileSize;
    public List<String> biomNames;
    public List<String> starterDecks;


    private List<BiomData> bioms;
    public List<BiomData>  GetBioms()
    {
        if(bioms==null)
        {
            bioms=new ArrayList<BiomData>();
            Json json=new Json();
            for(String name:biomNames)
            {
                bioms.add(json.fromJson(BiomData.class,forge.adventure.util.Res.CurrentRes.GetFile("world/"+name)));
            }
        }
        return bioms;
    }
}