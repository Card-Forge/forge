package forge.adventure.world;

import com.badlogic.gdx.graphics.Pixmap;
import forge.adventure.util.Serializer;

import java.io.IOException;

public class WorldSaveHeader implements java.io.Serializable {
    public static int previewImageWidth=512;
    private  void writeObject(java.io.ObjectOutputStream out) throws IOException {

        out.writeUTF(name);
        if(preview==null)
            preview=new Pixmap(1,1, Pixmap.Format.RGB888);
        Serializer.WritePixmap(out,preview);
    }
    private  void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        name=in.readUTF();
        preview= Serializer.ReadPixmap(in);

    }

    public Pixmap preview;
    public String name;
}