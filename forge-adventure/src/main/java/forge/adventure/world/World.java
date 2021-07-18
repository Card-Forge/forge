package forge.adventure.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import forge.adventure.data.BiomData;
import forge.adventure.data.BiomSpriteData;
import forge.adventure.data.WorldData;
import forge.adventure.scene.Scene;
import forge.adventure.util.Res;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import javafx.util.Pair;

import java.io.File;
import java.util.Random;

public class World {


    private WorldData data;
    private double[][] noiseData;
    private Pixmap biomImage;
    private Pixmap noiseImage;
    private int[][] biomMap; 
    private int width;
    private MapObjectMap mapObjectIds;
    private int height;
    private int tileSize;
    private BiomTexture[] biomTexture;


    public MapObject GetObject(int id)
    {
        return  mapObjectIds.get(id);
    }

    public Pixmap GetBiomSprite(int x, int y)
    {
        if(x<0||y<=0||x>=width||y>height)
            return new Pixmap(tileSize,tileSize, Pixmap.Format.RGB888);

        int biomIndex=GetBiom(x,y);
        double noise= GetNoise(x,y);
        BiomTexture  regions= biomTexture[biomIndex];
        int biomSubIndex=(int)(noise*(double)regions.images.size);
        if(x==0||y==1||x==width-1||y==height)//edge
            return  regions.GetPixmapFor(biomSubIndex);

        int neighbors=0b000_000_000;
        int biomID=biomIndex*10+biomSubIndex;
        int heighestBiomID=biomIndex*10+biomSubIndex;
        int bitIndex=8;
        for(int ny=1;ny>-2;ny--)
        {
            for(int nx=-1;nx<2;nx++)
            {
                int otherIndex=GetBiom(x+nx,y+ny);
                double othernoise= GetNoise(x+nx,y+ny);
                int otherbiomSubIndex=(int)(othernoise*(double) biomTexture[otherIndex].images.size);
                if(heighestBiomID<otherIndex*10+otherbiomSubIndex)
                    heighestBiomID=otherIndex*10+otherbiomSubIndex;
                boolean isSame=biomID==otherIndex*10+otherbiomSubIndex;
                if(isSame)
                {
                    int bit=1;
                    bit=bit<<bitIndex;
                    neighbors|=bit;

                }
                bitIndex--;
            }
        }
        if(biomID==heighestBiomID)
            return  regions.GetPixmapFor(biomSubIndex);
        BiomTexture  baseRegions= biomTexture[heighestBiomID/10];
        return  regions.GetPixmapFor(biomSubIndex,neighbors,heighestBiomID,baseRegions.GetPixmapFor(heighestBiomID%10));
    }


    public double GetNoise(int x,int y)
    {
        return noiseData[x][height-y];
    }
    public int GetBiom(int x,int y)
    {
        return biomMap[x][height-y];
    }
    static public Deck[] StarterDecks() {

        FileHandle handle = forge.adventure.util.Res.CurrentRes.GetFile("world/world.json");
        String rawJson=handle.readString();
        WorldData data = (new Json()).fromJson(WorldData.class,rawJson);
        Deck[] deck=new Deck[data.starterDecks.size()];
        for(int i=0;i<data.starterDecks.size();i++)
        {
            deck[i]= DeckSerializer.fromFile(new File(Res.CurrentRes.GetFilePath(data.starterDecks.get(i))));
        }
        return deck;
    }

    public WorldData GetData()
    {
        return  data;
    }
    public World GenerateNew() {

        FileHandle handle= forge.adventure.util.Res.CurrentRes.GetFile("world/world.json");
        String rawJson=handle.readString();
        data = (new Json()).fromJson(WorldData.class,rawJson);
        Random rand=new Random();
        int seed= rand.nextInt();
        OpenSimplexNoise noise=new OpenSimplexNoise(seed);

        double noiceZoom=10;
        double noiceObjectZoom=10;
        width =data.width;
        height =data.height;
        tileSize=data.tileSize;
        //save at all data
        noiseData=new double[width][height];
        biomMap=new int[width][height];
        Pixmap pix=new Pixmap(width, height, Pixmap.Format.RGB888);
        Pixmap noicePix=new Pixmap(width, height, Pixmap.Format.RGB888);

        for(int x = 0; x< width; x++)
        {
            for(int y = 0; y< height; y++)
            {
                noiseData[x][y]=(noise.eval(x/(double) width *noiceZoom,y/(double) height *noiceZoom)+1)/2;
                noicePix.setColor((float)noiseData[x][y],(float)noiseData[x][y],(float)noiseData[x][y],1);
                noicePix.drawPixel(x,y);
            }
        }

        pix.setColor(1,0,0,1);
        pix.fill();

        /*for(long x=0;x<sizeX;x++)
        {
            for(long y=0;y<sizeY;y++)
            {
                //value 0-1 based on noise
                ;
                float value=(float)noise.eval((double) x/(double)sizeX*noiceZoom,(double)y/(double)sizeY*noiceZoom);


            }
        }*/
        int i=-1;
        biomTexture =new BiomTexture[data.GetBioms().size()];
        for(BiomData biom:data.GetBioms())
        {

            i++;
            biomTexture[i]=new BiomTexture(new TextureAtlas(Res.CurrentRes.GetFile(biom.tileset)),data.tileSize,biom.name);
            int biomXStart=(int)Math.round(biom.startPointX * (double) width);
            int biomYStart=(int)Math.round(biom.startPointY * (double) height);
            int biomWidth=(int)Math.round(biom.width * (double) width);
            int biomHeight=(int)Math.round(biom.height * (double) height);

            int beginx=Math.max(biomXStart-biomWidth/2,0);
            int beginy=Math.max(biomYStart-biomHeight/2,0);
            int endx=Math.min(biomXStart+biomWidth, width);
            int endy=Math.min(biomYStart+biomHeight, height);
            if(biom.width ==1.0&&biom.height ==1.0)
            {
                beginx=0;
                beginy=0;
                endx= width;
                endy= height;
            }
            for(int x=beginx;x<endx;x++)
            {
                for(int y=beginy;y<endy;y++)
                {
                    //value 0-1 based on noise
                    double noiseValue=noiseData[x][y];
                    noiseValue*=biom.noiceWeight;
                    //value 0-1 based on dist to origin
                    double distanceValue=(Math.sqrt((x-biomXStart)*(x-biomXStart) + (y-biomYStart)*(y-biomYStart)))/(Math.max(biomWidth,biomHeight)/2);
                    distanceValue*=biom.distWeight;
                    if(noiseValue+distanceValue<1.0||biom.invertHeight&&(1-noiseValue)+distanceValue<1.0)
                    {
                        Color color=biom.GetColor();
                        pix.setColor(color.r,color.g,color.b,1);
                        pix.drawPixel(x,y);
                        biomMap[x][y]=i;
                    }

                }
            }
        }
        mapObjectIds= new MapObjectMap(GetChunkSize(),tileSize);
        for(int x = 0; x< width; x++)
        {
            for(int y = 0; y< height; y++)
            {
                int invertedHeight=height-y-1;
                int currentBiom=biomMap[x][invertedHeight];
                double spriteNoise=(noise.eval(x/(double) width *noiceObjectZoom,y/(double) invertedHeight *noiceObjectZoom)+1)/2;
                BiomData biom=data.GetBioms().get(currentBiom);
                for(BiomSpriteData sprite:biom.GetSprites())
                {
                    if(spriteNoise>=sprite.startArea&&spriteNoise<=sprite.endArea)
                    {
                        if(rand.nextFloat()<=sprite.density)
                        {
                            String spriteKey=sprite.key();
                            int key=-1;
                            if(!mapObjectIds.containsKey(spriteKey))
                            {

                                key=mapObjectIds.put(sprite.key(),new MapObject(sprite));
                            }
                            else
                            {
                                key=mapObjectIds.intKey(spriteKey);
                            }
                            mapObjectIds.putPosition(key,new Vector2((float)x*tileSize+(rand.nextFloat()*32.0f),(float)y*tileSize+(rand.nextFloat()*32.0f)));
                        }
                    }
                }
            }
        }


        noiseImage=noicePix;
        biomImage=pix;

        return this;//new World();
    }

    public int GetWidthInTiles() {
        return width;
    }

    public int GetHeightInTiles() {
        return height;
    }
    public int GetWidthInPixels() {
        return width*32;
    }

    public int GetHeightInPixels() {
        return height*32;
    }
    public int GetWidthInChunks() {
        return width/GetChunkSize();
    }

    public int GetHeightInChunks() {
        return height/GetChunkSize();
    }
    public int GetTileSize() {
        return tileSize;
    }

    public Pixmap getBiomImage() {
        return biomImage;
    }

    public Pixmap getNoiseImage() {
        return noiseImage;
    }

    public Array<Pair<Vector2,Integer>> GetMapObjects(int chunkx, int chunky) {
        return mapObjectIds.positions(chunkx,chunky);
    }

    public int GetChunkSize() {
        return Scene.IntendedWidth/tileSize;
    }
}
