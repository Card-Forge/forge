package forge.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import forge.Forge.Graphics;

public enum FSkinTexture implements FImage {
    BG_TEXTURE("bg_texture.jpg"),
    BG_MATCH("bg_match.jpg");
    
    private final String filename;
    private Texture texture; 

    FSkinTexture(String filename0) {
        filename = filename0;
    }

    public void load(String preferredDir, String defaultDir) {
        String preferredFile = preferredDir + filename;
        FileHandle file = Gdx.files.internal(preferredFile);
        if (file.exists()) {
            try {
                texture = new Texture(file);
            }
            catch (final Exception e) {
                System.err.println("Failed to load skin file: " + preferredFile);
                e.printStackTrace();
            }
        }
        if (texture == null) {
            //use default file if can't use preferred file
            String defaultFile = defaultDir + filename;
            file = Gdx.files.internal(defaultFile);
            if (file.exists()) {
                try {
                    texture = new Texture(file);
                }
                catch (final Exception e) {
                    System.err.println("Failed to load skin file: " + defaultFile);
                    e.printStackTrace();
                }
            }
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
        g.drawImage(texture, x, y, w, h);
    }
}