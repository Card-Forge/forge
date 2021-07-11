package forge.adventure.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Json;
import forge.adventure.data.BiomData;
import forge.adventure.data.WorldData;

import java.awt.image.BufferedImage;
import java.util.Random;

public class World {


    public Pixmap GenerateNew() {

        FileHandle handle= forge.adventure.util.Res.CurrentRes.GetFile("world/word.json");
        String rawJson=handle.readString();
        WorldData data = (new Json()).fromJson(WorldData.class,rawJson);
        int seed= new Random().nextInt();
        OpenSimplexNoise noise=new OpenSimplexNoise(seed);

        double noiceZoom=10;

        //save at all data
        double[][] noiseData=new double[data.sizeX][data.sizeY];

        for(int x=0;x<data.sizeX;x++)
        {
            for(int y=0;y<data.sizeY;y++)
            {
                noiseData[x][y]=noise.eval(x/data.sizeX*noiceZoom,y/data.sizeY*noiceZoom);
            }
        }
        BufferedImage image = new BufferedImage(data.sizeX, data.sizeY, BufferedImage.TYPE_4BYTE_ABGR);
        byte[] imagedata = new byte[data.sizeX* data.sizeY*4];

        Pixmap pix=new Pixmap(data.sizeX,data.sizeY, Pixmap.Format.RGB888);
        pix.setColor(1,0,0,1);
        pix.fill();

        /*for(long x=0;x<data.sizeX;x++)
        {
            for(long y=0;y<data.sizeY;y++)
            {
                //value 0-1 based on noise
                ;
                float value=(float)noise.eval((double) x/(double)data.sizeX*noiceZoom,(double)y/(double)data.sizeY*noiceZoom);
                value=(value+1)/2;
                pix.setColor(value,value,value,1);
                pix.drawPixel((int)x,(int)y);

            }
        }*/
        for(BiomData biom:data.bioms)
        {
         long biomXStart=Math.round(biom.startPointX * (double)data.sizeX);
         long biomYStart=Math.round(biom.startPointY * (double)data.sizeY);
            long biomXSize=Math.round(biom.sizeX * (double)data.sizeX);
            long biomYSize=Math.round(biom.sizeY * (double)data.sizeY);

            if(biom.sizeX==1.0&&biom.sizeY==1.0)
            {
                pix.setColor(biom.GetColor().r,biom.GetColor().g,biom.GetColor().b,1);
                pix.fill();
            }
            else
            {
                for(long x=Math.max(biomXStart-biomXSize/2,0);x<biomXStart+biomXSize;x++)
                {
                    for(long y=Math.max(biomYStart-biomYSize/2,0);y<biomYStart+biomYSize;y++)
                    {
                        //value 0-1 based on noise
                        double noiseValue=noise.eval((double) x/(double)data.sizeX*noiceZoom,(double)y/(double)data.sizeY*noiceZoom);
                        noiseValue*=biom.noiceWeight;
                        //value 0-1 based on dist to origin
                        double distanceValue=(Math.sqrt((x-biomXStart)*(x-biomXStart) + (y-biomYStart)*(y-biomYStart)))/(Math.max(biomXSize,biomYSize)/2);
                        distanceValue*=biom.distWeight;
                        if(noiseValue+distanceValue<1.0||biom.invertHeight&&(1-noiseValue)+distanceValue<1.0)
                        {
                            imagedata[(((int)y*data.sizeX)+(int)x*4)]=(byte)(biom.GetColor().r*255);
                            imagedata[(((int)y*data.sizeX)+(int)x*4)+1]=(byte)(biom.GetColor().g*255);
                            imagedata[(((int)y*data.sizeX)+(int)x*4)+2]=(byte)(biom.GetColor().b*255);
                            pix.setColor(biom.GetColor().r,biom.GetColor().g,biom.GetColor().b,1);
                            pix.drawPixel((int)x,(int)y);
                        }

                    }
                }
            }

        }


        return pix;//new World();
    }
}
