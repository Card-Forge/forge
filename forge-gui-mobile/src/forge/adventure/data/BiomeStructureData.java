package forge.adventure.data;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BiomeStructureData {


    static public class BiomeStructureDataMapping
    {
        public int getColor() {
            return ((Integer.parseInt(color,16)<<8)|0xff);
        }
        public String name;
        public String color;
        public boolean collision;

         public BiomeStructureDataMapping() {

        }
        public BiomeStructureDataMapping(BiomeStructureDataMapping biomeStructureDataMapping) {
            this.name=biomeStructureDataMapping.name;
            this.color=biomeStructureDataMapping.color;
            this.collision=biomeStructureDataMapping.collision;
        }
    }
    public int N = 3;
    public float x;
    public float y;
    public boolean randomPosition;

    public String structureAtlasPath;
    public String sourcePath;
    public boolean periodicInput=true;
    public float height;
    public float width;
    public int ground;
    public int symmetry=2;
    public boolean periodicOutput=true;
    public BiomeStructureDataMapping[] mappingInfo;

    public BiomeStructureData( ) {

    }
    public BiomeStructureData(BiomeStructureData biomeStructureData) {
        this.structureAtlasPath=biomeStructureData.structureAtlasPath;
        this.sourcePath=biomeStructureData.sourcePath;
        this.x=biomeStructureData.x;
        this.y=biomeStructureData.y;
        this.width=biomeStructureData.width;
        this.height=biomeStructureData.height;
        this.randomPosition=biomeStructureData.randomPosition;
        if(biomeStructureData.mappingInfo!=null)
        {
            this.mappingInfo=new BiomeStructureDataMapping[  biomeStructureData.mappingInfo.length];
            for(int i=0;i<biomeStructureData.mappingInfo.length;i++)
                this.mappingInfo[i]=new BiomeStructureDataMapping(biomeStructureData.mappingInfo[i]);
        }
        else
            this.mappingInfo=null;
    }

    public BufferedImage sourceImage() {

        return null;
    }
}
