package forge.adventure.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import forge.util.BlurUtils;
import forge.Forge;
import forge.Graphics;
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
            preview = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
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
        TextureRegion tr = Forge.takeScreenshot();
        Matrix4 m  = new Matrix4();
        Graphics g = new Graphics();
        FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
        frameBuffer.begin();
        m.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        g.begin(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        g.setProjectionMatrix(m);
        g.startClip();
        g.drawImage(tr, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        g.end();
        g.endClip();
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (Forge.lastPreview != null)
            Forge.lastPreview.dispose();
        Pixmap blurred = BlurUtils.blur(pixmap, 4, 2, false, true);
        Forge.lastPreview = new Texture(blurred);
        Pixmap scaled = new Pixmap(WorldSaveHeader.previewImageWidth, (int) (WorldSaveHeader.previewImageWidth / (Scene.getIntendedWidth() / (float) Scene.getIntendedHeight())), Pixmap.Format.RGBA8888);
        scaled.drawPixmap(pixmap,
                0, 0, pixmap.getWidth(), pixmap.getHeight(),
                0, 0, scaled.getWidth(), scaled.getHeight());
        pixmap.dispose();
        blurred.dispose();
        if (preview != null)
            preview.dispose();
        preview = scaled;
        frameBuffer.end();
        g.dispose();
        frameBuffer.dispose();
    }
}