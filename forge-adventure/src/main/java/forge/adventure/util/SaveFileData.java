package forge.adventure.util;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.io.*;
import java.util.HashMap;

public class SaveFileData extends HashMap<String,byte[]>
{
    public void store(String key,SaveFileData subData)
    {
        try {

            ByteArrayOutputStream stream=new ByteArrayOutputStream();
            ObjectOutputStream objStream=new ObjectOutputStream(stream);
            objStream.writeObject(subData);
            objStream.flush();
            put(key,stream.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void store(String key,float subData)
    {
        try {
            ByteArrayOutputStream stream=new ByteArrayOutputStream();
            ObjectOutputStream objStream=new ObjectOutputStream(stream);
            objStream.writeFloat(subData);
            objStream.flush();
            put(key,stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void store(String key,double subData)
    {
        try {
            ByteArrayOutputStream stream=new ByteArrayOutputStream();
            ObjectOutputStream objStream=new ObjectOutputStream(stream);
            objStream.writeDouble(subData);
            objStream.flush();
            put(key,stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void store(String key,int subData)
    {
        try {
            ByteArrayOutputStream stream=new ByteArrayOutputStream();
            ObjectOutputStream objStream=new ObjectOutputStream(stream);
            objStream.writeInt(subData);
            objStream.flush();
            put(key,stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void store(String key,long subData)
    {
        try {
            ByteArrayOutputStream stream=new ByteArrayOutputStream();
            ObjectOutputStream objStream=new ObjectOutputStream(stream);
            objStream.writeLong(subData);
            objStream.flush();
            put(key,stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void store(String key,boolean subData)
    {
        try {
            ByteArrayOutputStream stream=new ByteArrayOutputStream();
            ObjectOutputStream objStream=new ObjectOutputStream(stream);
            objStream.writeBoolean(subData);
            objStream.flush();
            put(key,stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void store(String key, Pixmap pixmap)
    {
        try {
            ByteArrayOutputStream stream=new ByteArrayOutputStream();

            PixmapIO.PNG png = new PixmapIO.PNG();
            png.setFlipY(false);
            png.write(stream, pixmap);
            stream.flush();
            put(key,stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void storeObject(String key,Object subData)
    {
        try {
            ByteArrayOutputStream stream=new ByteArrayOutputStream();
            ObjectOutputStream objStream=new ObjectOutputStream(stream);
            objStream.writeObject(subData);
            objStream.flush();
            put(key,stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void store(String key,String subData)
    {
        try {
            ByteArrayOutputStream stream=new ByteArrayOutputStream();
            ObjectOutputStream objStream=new ObjectOutputStream(stream);
            objStream.writeUTF(subData);
            objStream.flush();
            put(key,stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void store(String key, Vector2 vector) {

        try {
            ByteArrayOutputStream stream=new ByteArrayOutputStream();
            ObjectOutputStream objStream=new ObjectOutputStream(stream);
            objStream.writeFloat(vector.x);
            objStream.writeFloat(vector.y);
            objStream.flush();
            put(key,stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void store(String key, Rectangle rectangle) {

        try {
            ByteArrayOutputStream stream=new ByteArrayOutputStream();
            ObjectOutputStream objStream=new ObjectOutputStream(stream);
            objStream.writeFloat(rectangle.x);
            objStream.writeFloat(rectangle.y);
            objStream.writeFloat(rectangle.width);
            objStream.writeFloat(rectangle.height);
            objStream.flush();
            put(key,stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SaveFileData readSubData(String key)
    {
        if(!containsKey(key))
            return null;
        try {

            ByteArrayInputStream stream=new ByteArrayInputStream(get(key));
            ObjectInputStream objStream=new ObjectInputStream(stream);
            return (SaveFileData)objStream.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    public Object readObject(String key)
    {
        if(!containsKey(key))
            return null;
        try {

            ByteArrayInputStream stream=new ByteArrayInputStream(get(key));
            ObjectInputStream objStream=new ObjectInputStream(stream);
            return objStream.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String readString(String key)
    {
        if(!containsKey(key))
            return null;
        try {

            ByteArrayInputStream stream=new ByteArrayInputStream(get(key));
            ObjectInputStream objStream=new ObjectInputStream(stream);
            return objStream.readUTF();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public long readLong(String key) {
        if(!containsKey(key))
            return 0;
        try {

            ByteArrayInputStream stream=new ByteArrayInputStream(get(key));
            ObjectInputStream objStream=new ObjectInputStream(stream);
            return objStream.readLong();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public float readFloat(String key)
    {
        if(!containsKey(key))
            return 0.0f;
        try {

            ByteArrayInputStream stream=new ByteArrayInputStream(get(key));
            ObjectInputStream objStream=new ObjectInputStream(stream);
            return objStream.readFloat();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0.0f;
    }

    public double readDouble(String key)
    {
        if(!containsKey(key))
            return 0.0;
        try {

            ByteArrayInputStream stream=new ByteArrayInputStream(get(key));
            ObjectInputStream objStream=new ObjectInputStream(stream);
            return objStream.readDouble();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0.0;
    }
    public Vector2 readVector2(String key)
    {
        if(!containsKey(key))
            return new Vector2();
        try {

            ByteArrayInputStream stream=new ByteArrayInputStream(get(key));
            ObjectInputStream objStream=new ObjectInputStream(stream);
            float x= objStream.readFloat();
            float y= objStream.readFloat();
            return new Vector2(x,y);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Vector2();
    }
    public Rectangle readRectangle(String key)
    {
        if(!containsKey(key))
            return new Rectangle();
        try {

            ByteArrayInputStream stream=new ByteArrayInputStream(get(key));
            ObjectInputStream objStream=new ObjectInputStream(stream);
            float x= objStream.readFloat();
            float y= objStream.readFloat();
            float width= objStream.readFloat();
            float height= objStream.readFloat();
            return new Rectangle(x,y,width,height);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Rectangle();
    }



    public Pixmap readPixmap(String key)
    {
        if(!containsKey(key))
            return null;
        return new Pixmap(get(key), 0, get(key).length);
    }
    public int readInt(String key)
    {
        if(!containsKey(key))
            return 0;
        try {

            ByteArrayInputStream stream=new ByteArrayInputStream(get(key));
            ObjectInputStream objStream=new ObjectInputStream(stream);
            return objStream.readInt();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public boolean readBool(String key)
    {
        if(!containsKey(key))
            return false;
        try {

            ByteArrayInputStream stream=new ByteArrayInputStream(get(key));
            ObjectInputStream objStream=new ObjectInputStream(stream);
            return objStream.readBoolean();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


}
