package forge.assets;

import com.badlogic.gdx.graphics.Texture;

import forge.Graphics;

public class FTextureImage implements FImage {
    private final Texture texture;

    public FTextureImage(Texture texture0) {
        texture = texture0;
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
