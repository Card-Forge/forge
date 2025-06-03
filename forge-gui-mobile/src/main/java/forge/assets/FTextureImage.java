package forge.assets;

import com.badlogic.gdx.graphics.Texture;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.Graphics;

public class FTextureImage extends FImageComplex {
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
    public Texture getTexture() {
        return texture;
    }

    @Override
    public TextureRegion getTextureRegion() {
        return new TextureRegion(texture);
    }

    @Override
    public int getRegionX() {
        return 0;
    }

    @Override
    public int getRegionY() {
        return 0;
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        g.drawImage(texture, x, y, w, h);
    }
}
