package forge.adventure.data;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Json;

import java.util.ArrayList;
import java.util.List;

public class BiomData
{
    public double startPointX;
    public double startPointY;
    public double noiceWeight;
    public double distWeight;
    public String name;
    public String tileset;

    public double width;
    public double height;
    public String color;
    public boolean invertHeight;
    public Color GetColor(){return Color.valueOf(color);}

    public List<String> spriteNames;
    private List<BiomSpriteData> sprites;
    public List<BiomSpriteData>  GetSprites()
    {
        if(sprites==null)
        {
            sprites=new ArrayList<>();
            Json json=new Json();
            for(String name:spriteNames)
            {
                sprites.add(json.fromJson(BiomSpriteData.class,forge.adventure.util.Res.CurrentRes.GetFile("world/"+name)));
            }
        }
        return sprites;
    }
}