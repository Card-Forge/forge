package forge.adventure.data;

import java.awt.image.BufferedImage;

public class BiomeStructureData {
    public int N = 3;
    public float x;
    public float y;
    public float size;
    public boolean randomPosition;
    public boolean collision;

    public String structureAtlasPath;
    public boolean periodicInput;
    public float height;
    public float width;
    public int ground;
    public int symmetry;
    public boolean periodicOutput;

    public BiomeStructureData( )
    {

    }
    public BiomeStructureData(BiomeStructureData biomeStructureData) {
        this.structureAtlasPath=biomeStructureData.structureAtlasPath;
        this.x=biomeStructureData.x;
        this.y=biomeStructureData.y;
        this.size=biomeStructureData.size;
        this.randomPosition=biomeStructureData.randomPosition;
        this.collision=biomeStructureData.collision;
    }

    public BufferedImage sourceImage() {

        return null;
    }
}
