package forge.adventure.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Json;
import forge.adventure.data.BiomData;
import forge.adventure.data.WorldData;
import forge.adventure.util.Res;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;

import java.io.File;
import java.util.Random;

public class World {


    private double[][] NoiseData;
    private Pixmap BiomImage;
    private Pixmap NoiseImage;
    private WorldData Data;
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

    public World GenerateNew() {

        FileHandle handle= forge.adventure.util.Res.CurrentRes.GetFile("world/world.json");
        String rawJson=handle.readString();
        WorldData data = (new Json()).fromJson(WorldData.class,rawJson);
        int seed= new Random().nextInt();
        OpenSimplexNoise noise=new OpenSimplexNoise(seed);

        double noiceZoom=10;

        //save at all data
        double[][] noiseData=new double[data.sizeX][data.sizeY];
        Pixmap pix=new Pixmap(data.sizeX,data.sizeY, Pixmap.Format.RGB888);
        Pixmap noicePix=new Pixmap(data.sizeX,data.sizeY, Pixmap.Format.RGB888);

        for(int x=0;x<data.sizeX;x++)
        {
            for(int y=0;y<data.sizeY;y++)
            {
                noiseData[x][y]=(noise.eval(x/(double)data.sizeX*noiceZoom,y/(double)data.sizeY*noiceZoom)+1)/2;
                noicePix.setColor((float)noiseData[x][y],(float)noiseData[x][y],(float)noiseData[x][y],1);
                noicePix.drawPixel(x,y);
            }
        }

        pix.setColor(1,0,0,1);
        pix.fill();

        /*for(long x=0;x<data.sizeX;x++)
        {
            for(long y=0;y<data.sizeY;y++)
            {
                //value 0-1 based on noise
                ;
                float value=(float)noise.eval((double) x/(double)data.sizeX*noiceZoom,(double)y/(double)data.sizeY*noiceZoom);


            }
        }*/
        for(BiomData biom:data.GetBioms())
        {
            int biomXStart=(int)Math.round(biom.startPointX * (double)data.sizeX);
            int biomYStart=(int)Math.round(biom.startPointY * (double)data.sizeY);
            int biomXSize=(int)Math.round(biom.sizeX * (double)data.sizeX);
            int biomYSize=(int)Math.round(biom.sizeY * (double)data.sizeY);

            int beginx=Math.max(biomXStart-biomXSize/2,0);
            int beginy=Math.max(biomYStart-biomYSize/2,0);
            int endx=Math.min(biomXStart+biomXSize,data.sizeX);
            int endy=Math.min(biomYStart+biomYSize,data.sizeY);
            if(biom.sizeX==1.0&&biom.sizeY==1.0)
            {
                beginx=0;
                beginy=0;
                endx=data.sizeX;
                endy=data.sizeY;
            }
            for(int x=beginx;x<endx;x++)
            {
                for(int y=beginy;y<endy;y++)
                {
                    //value 0-1 based on noise
                    double noiseValue=noiseData[x][y];
                    noiseValue*=biom.noiceWeight;
                    //value 0-1 based on dist to origin
                    double distanceValue=(Math.sqrt((x-biomXStart)*(x-biomXStart) + (y-biomYStart)*(y-biomYStart)))/(Math.max(biomXSize,biomYSize)/2);
                    distanceValue*=biom.distWeight;
                    if(noiseValue+distanceValue<1.0||biom.invertHeight&&(1-noiseValue)+distanceValue<1.0)
                    {
                        Color color=biom.GetColor();
                        pix.setColor(color.r,color.g,color.b,1);
                        pix.drawPixel(x,y);
                    }

                }
            }

        }

        World ret=new World();
        ret.Data=data;
        ret.NoiseImage=noicePix;
        ret.BiomImage=pix;
        ret.NoiseData=noiseData;

        return ret;//new World();
    }
}
