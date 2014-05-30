package forge.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;

import forge.Forge.Graphics;

public enum FSkinTexture implements FImage {
    BG_TEXTURE("bg_texture.jpg", true),
    BG_MATCH("bg_match.jpg", false);

    private final String filename;
    private final boolean repeat;
    private Texture texture;

    FSkinTexture(String filename0, boolean repeat0) {
        filename = filename0;
        repeat = repeat0;
    }

    public void load() {
        FileHandle preferredFile = FSkin.getSkinFile(filename);
        if (preferredFile.exists()) {
            try {
                texture = new Texture(preferredFile);
            }
            catch (final Exception e) {
                System.err.println("Failed to load skin file: " + preferredFile);
                e.printStackTrace();
            }
        }
        if (texture == null) {
            //use default file if can't use preferred file
            FileHandle defaultFile = FSkin.getDefaultSkinFile(filename);
            if (defaultFile.exists()) {
                try {
                    texture = new Texture(defaultFile);
                }
                catch (final Exception e) {
                    System.err.println("Failed to load skin file: " + defaultFile);
                    e.printStackTrace();
                    return;
                }
            }
        }
        if (repeat) {
            texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
        }
    }

    @Override
    public float getWidth() {
        return texture.getWidth();
    }

    @Override
    public float getHeight() {
        return texture.getHeight();
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        if (repeat) {
            g.drawRepeatingImage(texture, x, y, w, h);
        }
        else {
            g.drawImage(texture, x, y, w, h);
        }
    }
}