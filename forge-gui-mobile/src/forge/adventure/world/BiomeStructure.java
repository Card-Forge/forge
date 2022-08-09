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
    private final static int MAXIMUM_WAVEFUNCTIONSIZE=50;

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
        int targetWidth=(int) (data.width* biomeWidth);
        int targetHeight=(int) (data.width* biomeWidth);
        dataMap=new int[targetWidth][  targetHeight];
        collisionMap=new boolean[targetWidth][ targetHeight];
        ColorMap finalImage=new ColorMap(targetWidth, targetHeight);
        HashMap<Integer,Integer> colorIdMap=new HashMap<>();
        for(int i=0;i<data.mappingInfo.length;i++)
        {
            colorIdMap.put(Integer.parseInt(data.mappingInfo[i].color,16),i);
        }
        for(int mx=0;mx<targetWidth;mx+=Math.min(targetWidth-mx,MAXIMUM_WAVEFUNCTIONSIZE))
        {
            for(int my=0;my<targetWidth;my+=Math.min(targetHeight-my,MAXIMUM_WAVEFUNCTIONSIZE))
            {
                OverlappingModel model= new OverlappingModel(sourceImage,data.N,Math.min(targetWidth-mx,MAXIMUM_WAVEFUNCTIONSIZE), Math.min(targetHeight-my,MAXIMUM_WAVEFUNCTIONSIZE),data.periodicInput,data.periodicOutput,data.symmetry,data.ground);

                boolean suc=false;
                for(int i=0;i<10&&!suc;i++)
                    suc=model.run((int) seed+(i*5355)+mx*my,0);
                if(!suc)
                {
                    for(int x=0;x<dataMap.length;x++)
                        for(int y=0;y<dataMap[x].length;y++)
                            dataMap[mx+x][my+y]=-1;
                    return;
                }
                image=model.graphics();
                for(int x=0;x<image.getWidth();x++)
                {

                    for(int y=0;y<image.getHeight();y++)
                    {
                        boolean isWhitePixel=maskImage!=null&&(maskImage.getColor((int) ((mx+x)*maskImage.getWidth()/(float)targetWidth),(int)((my+y)*(maskImage.getHeight()/(float)targetHeight)) )).equals(Color.WHITE);

                        if(isWhitePixel)
                            finalImage.setColor(mx+x,my+y, Color.WHITE);
                        else
                            finalImage.setColor(mx+x,my+y, image.getColor(x,y));
                        int rgb=Color.rgb888(image.getColor(x,y))  ;
                        if(isWhitePixel||!colorIdMap.containsKey(rgb))
                        {
                            dataMap[mx+x][my+y]=-1;
                        }
                        else
                        {
                            dataMap[mx+x][my+y]=colorIdMap.get(rgb);
                            collisionMap[mx+x][my+y]=data.mappingInfo[colorIdMap.get(rgb)].collision;
                        }
                    }
                }

            }
            image=finalImage;
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
