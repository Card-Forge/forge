package forge.adventure.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Disposable;
import forge.adventure.scene.Scene;
import forge.adventure.util.Serializer;

import java.io.IOException;
import java.util.Date;

/**
 * Header information for the save file like a preview image, save name and saved date.
 */
public class WorldSaveHeader implements java.io.Serializable, Disposable {
    public static int previewImageWidth = 512;
    public Pixmap preview;
    public String name;
    public Date saveDate;

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {

        out.writeUTF(name);
        if (preview == null)
            preview = new Pixmap(1, 1, Pixmap.Format.RGB888);
        Serializer.WritePixmap(out, preview, true);
        out.writeObject(saveDate);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        if(preview!=null)
            preview.dispose();
        preview = Serializer.ReadPixmap(in);
        saveDate = (Date) in.readObject();

    }

    public void dispose() {
        preview.dispose();
    }

    public void createPreview() {
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Pixmap scaled = new Pixmap(WorldSaveHeader.previewImageWidth, (int) (WorldSaveHeader.previewImageWidth / (Scene.GetIntendedWidth() / (float) Scene.GetIntendedHeight())), Pixmap.Format.RGB888);
        scaled.drawPixmap(pixmap,
                0, 0, pixmap.getWidth(), pixmap.getHeight(),
                0, 0, scaled.getWidth(), scaled.getHeight());
        pixmap.dispose();
        if (preview != null)
            preview.dispose();
        preview = scaled;
    }
}