package forge.adventure.util;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Abstract class to serialize other objects.
 */
public abstract class Serializer {


    static public void WritePixmap(java.io.ObjectOutputStream out, Pixmap pixmap) throws IOException {

        if (pixmap != null) {
            PixmapIO.PNG png = new PixmapIO.PNG();
            png.setFlipY(false);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            png.write(stream, pixmap);
            byte[] data = stream.toByteArray();
            out.writeInt(data.length);
            out.write(data);
        } else {
            out.writeInt(0);
        }
    }

    static public Pixmap ReadPixmap(ObjectInputStream in) throws IOException, ClassNotFoundException {

        int length = in.readInt();
        if (length == 0)
            return new Pixmap(1, 1, Pixmap.Format.RGBA8888);

        byte[] data = new byte[length];
        in.readFully(data, 0, length);

        return new Pixmap(data, 0, length);

    }

    public static void WritePixmap(ObjectOutputStream out, Pixmap pixmap, boolean b) throws IOException {
        if (pixmap != null) {
            PixmapIO.PNG png = new PixmapIO.PNG();
            png.setFlipY(b);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            png.write(stream, pixmap);
            byte[] data = stream.toByteArray();
            out.writeInt(data.length);
            out.write(data);
        } else {
            out.writeInt(0);
        }
    }

    public static void writeVector(ObjectOutputStream out,Vector2 position) throws IOException {
        out.writeFloat(position.x);
        out.writeFloat(position.y);
    }
    public static void readVector(ObjectInputStream in, Vector2 position) throws IOException {
        float x=in.readFloat();
        position.set(x,in.readFloat());
    }

    public static void writeRectangle(ObjectOutputStream out,Rectangle rectangle) throws IOException {
        out.writeFloat(rectangle.x);
        out.writeFloat(rectangle.y);
        out.writeFloat(rectangle.width);
        out.writeFloat(rectangle.height);
    }
    public static void readRectangle(ObjectInputStream in, Rectangle rectangle) throws IOException {
        float x=in.readFloat();
        float y=in.readFloat();
        float w=in.readFloat();
        float h=in.readFloat();
        rectangle.set(x,y,w,h);
    }
}
