package forge.adventure.util;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public  class Serializer   {


    static public void WritePixmap(java.io.ObjectOutputStream out,Pixmap pixmap) throws IOException {

        if(pixmap!=null)
        {
            PixmapIO.PNG png=new PixmapIO.PNG();
            ByteArrayOutputStream stream= new ByteArrayOutputStream();
            png.write(stream,pixmap);
            byte[] data=stream.toByteArray();
            out.writeInt(data.length);
            out.write(data);

        }
        else
        {
            out.writeInt(0);
        }
    }
     static public Pixmap ReadPixmap(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

        int length=in.readInt();
        if(length==0)
            return new Pixmap(1,1, Pixmap.Format.RGBA8888);

        byte[] data=new byte[length];
         in.readFully(data,0,length);

        return new Pixmap(data,0,length);

    }
}
