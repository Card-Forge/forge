package forge.adventure.data;

import com.badlogic.gdx.graphics.Color;

public class BiomData
{
    public double startPointX;
    public double startPointY;
    public double noiceWeight;
    public double distWeight;
    public String name;

    public double sizeX;
    public double sizeY;
    public String color;
    public boolean invertHeight;
    public Color GetColor(){return Color.valueOf(color);}
}