package forge.adventure.data;

public class BiomSpriteData {
    public String textureAltas;
    public String textureName;
    public double startArea;
    public double endArea;
    public double density;

    public String key(){return "BiomSprite&"+textureAltas+"&"+textureName;}
}
