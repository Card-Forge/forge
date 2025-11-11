package forge.adventure.util;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import forge.Forge;
import io.sentry.Hint;
import io.sentry.Sentry;

import java.io.*;
import java.util.HashMap;

public class SaveFileData extends HashMap<String, byte[]> {
    public void store(String key, SaveFileData subData) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(stream);
            objStream.writeObject(subData);
            objStream.flush();
            put(key, stream.toByteArray());
        } catch (IOException e) {
            put("IOException", e.toString().getBytes());
            captureException(e, key, subData);
        }
    }


    public void store(String key, float subData) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(stream);
            objStream.writeFloat(subData);
            objStream.flush();
            put(key, stream.toByteArray());
        } catch (IOException e) {
            put("IOException", e.toString().getBytes());
            e.printStackTrace();
        }
    }

    public void store(String key, double subData) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(stream);
            objStream.writeDouble(subData);
            objStream.flush();
            put(key, stream.toByteArray());
        } catch (IOException e) {
            put("IOException", e.toString().getBytes());
            e.printStackTrace();
        }
    }

    public void store(String key, int subData) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(stream);
            objStream.writeInt(subData);
            objStream.flush();
            put(key, stream.toByteArray());
        } catch (IOException e) {
            put("IOException", e.toString().getBytes());
            e.printStackTrace();
        }
    }

    public void store(String key, long subData) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(stream);
            objStream.writeLong(subData);
            objStream.flush();
            put(key, stream.toByteArray());
        } catch (IOException e) {
            put("IOException", e.toString().getBytes());
            e.printStackTrace();
        }
    }

    public void store(String key, boolean subData) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(stream);
            objStream.writeBoolean(subData);
            objStream.flush();
            put(key, stream.toByteArray());
        } catch (IOException e) {
            put("IOException", e.toString().getBytes());
            e.printStackTrace();
        }
    }

    public void store(String key, Pixmap pixmap) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            PixmapIO.PNG png = new PixmapIO.PNG();
            png.setFlipY(false);
            png.write(stream, pixmap);
            stream.flush();
            put(key, stream.toByteArray());
        } catch (IOException e) {
            put("IOException", e.toString().getBytes());
            e.printStackTrace();
        }
    }

    public void storeObject(String key, Object subData) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(stream);
            objStream.writeObject(subData);
            objStream.flush();
            put(key, stream.toByteArray());
        } catch (IOException e) {
            put("IOException", e.toString().getBytes());
            captureException(e, key, subData);
        }
    }

    public void store(String key, String subData) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(stream);
            objStream.writeUTF(subData);
            objStream.flush();
            put(key, stream.toByteArray());
        } catch (IOException e) {
            put("IOException", e.toString().getBytes());
            captureException(e, key, subData);
        }
    }

    public void store(String key, Vector2 vector) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(stream);
            objStream.writeFloat(vector.x);
            objStream.writeFloat(vector.y);
            objStream.flush();
            put(key, stream.toByteArray());
        } catch (IOException e) {
            put("IOException", e.toString().getBytes());
            e.printStackTrace();
        }
    }

    public void store(String key, Rectangle rectangle) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(stream);
            objStream.writeFloat(rectangle.x);
            objStream.writeFloat(rectangle.y);
            objStream.writeFloat(rectangle.width);
            objStream.writeFloat(rectangle.height);
            objStream.flush();
            put(key, stream.toByteArray());
        } catch (IOException e) {
            put("IOException", e.toString().getBytes());
            e.printStackTrace();
        }
    }

    public SaveFileData readSubData(String key) {
        if (!containsKey(key))
            return null;
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(get(key));
            ObjectInputStream objStream = new DecompressibleInputStream(stream);
            return (SaveFileData) objStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object readObject(String key) {
        if (!containsKey(key))
            return null;
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(get(key));
            ObjectInputStream objStream = new DecompressibleInputStream(stream);
            return objStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            Forge.delayedSwitchBack();
        } catch (ClassCastException e) { //this allows loading
            System.err.println("Encountered problem loading object: " + key);
        }
        return null;
    }

    public String readString(String key) {
        if (!containsKey(key))
            return null;
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(get(key));
            ObjectInputStream objStream = new DecompressibleInputStream(stream);
            return objStream.readUTF();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public long readLong(String key) {
        if (!containsKey(key))
            return 0;
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(get(key));
            ObjectInputStream objStream = new DecompressibleInputStream(stream);
            return objStream.readLong();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public float readFloat(String key) {
        if (!containsKey(key))
            return 0.0f;
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(get(key));
            ObjectInputStream objStream = new DecompressibleInputStream(stream);
            return objStream.readFloat();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0.0f;
    }

    public double readDouble(String key) {
        if (!containsKey(key))
            return 0.0;
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(get(key));
            ObjectInputStream objStream = new DecompressibleInputStream(stream);
            return objStream.readDouble();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public Vector2 readVector2(String key) {
        if (!containsKey(key))
            return new Vector2();
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(get(key));
            ObjectInputStream objStream = new DecompressibleInputStream(stream);
            float x = objStream.readFloat();
            float y = objStream.readFloat();
            return new Vector2(x, y);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Vector2();
    }

    public Rectangle readRectangle(String key) {
        if (!containsKey(key))
            return new Rectangle();
        try {

            ByteArrayInputStream stream = new ByteArrayInputStream(get(key));
            ObjectInputStream objStream = new DecompressibleInputStream(stream);
            float x = objStream.readFloat();
            float y = objStream.readFloat();
            float width = objStream.readFloat();
            float height = objStream.readFloat();
            return new Rectangle(x, y, width, height);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Rectangle();
    }


    public Pixmap readPixmap(String key) {
        if (!containsKey(key))
            return null;
        return new Pixmap(get(key), 0, get(key).length);
    }

    public int readInt(String key) {
        if (!containsKey(key))
            return 0;
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(get(key));
            ObjectInputStream objStream = new DecompressibleInputStream(stream);
            return objStream.readInt();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean readBool(String key) {
        if (!containsKey(key))
            return false;
        try {

            ByteArrayInputStream stream = new ByteArrayInputStream(get(key));
            ObjectInputStream objStream = new DecompressibleInputStream(stream);
            return objStream.readBoolean();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void captureException(Exception e, String key, Object object) {
        Hint hint = new Hint();
        hint.set(key, object);
        Sentry.captureException(e, hint);
        e.printStackTrace();
    }

    static class DecompressibleInputStream extends ObjectInputStream {

        /*https://stackoverflow.com/questions/1816559/make-java-runtime-ignore-serialversionuids*/

        //private static Logger logger = LoggerFactory.getLogger(DecompressibleInputStream.class);

        public DecompressibleInputStream(InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            ObjectStreamClass resultClassDescriptor = super.readClassDescriptor(); // initially streams descriptor
            Class localClass; // the class in the local JVM that this descriptor represents.
            try {
                localClass = Class.forName(resultClassDescriptor.getName());
            } catch (ClassNotFoundException e) {
                //logger.error("No local class for " + resultClassDescriptor.getName(), e);
                System.err.println("[Class Not Found Exception]\nNo local class for " + resultClassDescriptor.getName());
                return resultClassDescriptor;
            }
            ObjectStreamClass localClassDescriptor = ObjectStreamClass.lookup(localClass);
            if (localClassDescriptor != null) { // only if class implements serializable
                final long localSUID = localClassDescriptor.getSerialVersionUID();
                final long streamSUID = resultClassDescriptor.getSerialVersionUID();
                if (streamSUID != localSUID) { // check for serialVersionUID mismatch.
                    String s = "Overriding serialized class version mismatch:"
                            + " class = " + resultClassDescriptor.getName()
                            + " local serialVersionUID = " + localSUID +
                            " stream serialVersionUID = " + streamSUID;

                    System.err.println("[Invalid Class Exception]\n" + s);
                    resultClassDescriptor = localClassDescriptor; // Use local class descriptor for deserialization
                }
            }
            return resultClassDescriptor;
        }
    }
}
