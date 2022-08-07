package forge.adventure.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import forge.adventure.data.BiomeStructureData;
import forge.adventure.util.Config;

import java.util.HashMap;

public class BiomeStructure {

    private BiomeStructureData data;
    long seed;
    private int biomeWidth;
    private int biomeHeight;
    private int dataMap[][];
    private boolean collisionMap[][];
    boolean init=false;
    private TextureAtlas structureAtlas;
    public ColorMap image;

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
            try
            {
                structureAtlas = Config.instance().getAtlas(data.structureAtlasPath);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return structureAtlas;
    }
    public int structureObjectCount() {
        return data.mappingInfo.length;
    }

    public int objectID(int x, int y) {

        if(!init)
        {
            initialize();
        }
        if(x>=dataMap.length||x<0||y<0||y>=dataMap[0].length)
            return -1;
        return dataMap[x][y]; 
    }

    public void initialize(ColorMap sourceImage,ColorMap maskImage) {
        long currentTime = System.currentTimeMillis();

        init=true;
        OverlappingModel model= new OverlappingModel(sourceImage,data.N, (int) (data.width* biomeWidth), (int) (data.height*biomeHeight),data.periodicInput,data.periodicOutput,data.symmetry,data.ground);
        HashMap<Integer,Integer> colorIdMap=new HashMap<>();
        for(int i=0;i<data.mappingInfo.length;i++)
        {
            colorIdMap.put(Integer.parseInt(data.mappingInfo[i].color,16),i);
        }
        boolean suc=false;
        for(int i=0;i<10&&!suc;i++)
            suc=model.run((int) seed+(i*5355),0);
        currentTime= System.currentTimeMillis();
        if(!suc)
        {
            dataMap=new int[(int) (data.width* biomeWidth)][ (int) (data.height*biomeHeight)];
            collisionMap=new boolean[(int) (data.width* biomeWidth)][ (int) (data.height*biomeHeight)];
            for(int x=0;x<dataMap.length;x++)
                for(int y=0;y<dataMap[x].length;y++)
                    dataMap[x][y]=-1;
            return;
        }
        image=model.graphics();
        dataMap=new int[image.getWidth()][image.getHeight()];
        collisionMap=new boolean[image.getWidth()][image.getHeight()];
        for(int x=0;x<image.getWidth();x++)
        {

            for(int y=0;y<image.getHeight();y++)
            {
                boolean isWhitePixel=maskImage!=null&&(maskImage.getColor((int) (x*maskImage.getWidth()/(float)image.getWidth()),(int)(y*(maskImage.getHeight()/(float)image.getHeight())) )).equals(Color.WHITE);

                if(isWhitePixel)
                    image.setColor(x,y, Color.WHITE);
                int rgb=Color.rgb888(image.getColor(x,y))  ;
                if(isWhitePixel||!colorIdMap.containsKey(rgb))
                {
                    dataMap[x][y]=-1;
                }
                else
                {
                    dataMap[x][y]=colorIdMap.get(rgb);
                    collisionMap[x][y]=data.mappingInfo[colorIdMap.get(rgb)].collision;
                }
            }
        }

    }
    public void initialize() {
       initialize(sourceImage(),maskImage());
    }

    public ColorMap sourceImage() {
            return new ColorMap(Config.instance().getFile(data.sourcePath));
    }
    public String sourceImagePath() {
        return  (Config.instance().getFilePath(data.sourcePath));
    }
    private ColorMap maskImage() {
        return new ColorMap(Config.instance().getFile(data.maskPath));

    }


    public BiomeStructureData.BiomeStructureDataMapping[] mapping() {
        return data.mappingInfo;
    }


    public boolean collision(int x, int y) {
        if(!init)
        {
            initialize();
        }
        if(x>=collisionMap.length||x<0||y<0||y>=collisionMap[0].length)
            return false;
        return collisionMap[x][y];
    }

    public String maskImagePath() {
        return  (Config.instance().getFilePath(data.maskPath));
    }
}
