package forge.adventure.world;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.github.sjcasey21.wavefunctioncollapse.OverlappingModel;
import forge.adventure.data.BiomeStructureData;
import forge.adventure.util.Config;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class BiomeStructure {

    private BiomeStructureData data;
    long seed;
    private int biomeWidth;
    private int biomeHeight;
    private int dataMap[][];
    boolean init=false;
    private TextureAtlas structureAtlas;
    public BiomeStructure(BiomeStructureData data,long seed,int width,int height)
    {
        this.data=data;
        this.seed=seed;
        this.biomeWidth = width;
        this.biomeHeight = height;
    }
    public TextureAtlas atlas() {
        if(structureAtlas==null)
        {
            structureAtlas = Config.instance().getAtlas(data.structureAtlasPath);
        }
        return structureAtlas;
    }
    public int structureObjectCount() {
        int count=0;
        for(TextureAtlas.AtlasRegion region:atlas ().getRegions())
        {
            if(region.name.startsWith("structure"))
            {
                count++;
            }
        }
        return count;
    }

    public int objectID(int x, int y) {

        if(!init)
        {
            init=true;
            initialize();
        }
        if(x>biomeWidth*data.width)
            return -1;
        if(y>biomeHeight*data.height)
            return -1;
        if(x<biomeWidth*data.x)
            return -1;
        if(y<biomeHeight*data.y)
            return -1;
        return dataMap[x][y]; 
    }

    private void initialize() {
        OverlappingModel model= new OverlappingModel(sourceImage(),data.N, (int) (data.width* biomeWidth), (int) (data.height*biomeHeight),data.periodicInput,data.periodicOutput,data.symmetry,data.ground);
        HashMap<Integer,Integer> colorIdMap=new HashMap<>();
        int counter=0;
        for(TextureAtlas.AtlasRegion region:atlas ().getRegions())
        {
            if(region.name.startsWith("structure"))
            {
               String[] split= region.name.split("_");
               if(split.length<2)
                   continue;
               int rgb=Integer.parseInt(split[1],16);
                colorIdMap.put(rgb,counter);
                counter++;
            }
        }
        BufferedImage image=model.graphics();
        dataMap=new int[image.getWidth()][image.getHeight()];
        for(int x=0;x<image.getWidth();x++)
        {

            for(int y=0;y<image.getHeight();y++)
            {
                int rgb=image.getRGB(x,y);
                if(!colorIdMap.containsKey(rgb))
                {
                    dataMap[x][y]=-1;
                }
                else {
                    dataMap[x][y]=colorIdMap.get(rgb);
                }
            }
        }

    }

    private BufferedImage sourceImage() {
        TextureAtlas.AtlasRegion region=atlas().findRegion("Source");
        if(region==null)
            return null;
        try {
            return ImageIO.read(new File(Config.instance().getFilePath(data.structureAtlasPath))).getSubimage((int) region.offsetX, (int) region.offsetY,region.originalWidth,region.originalHeight);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
