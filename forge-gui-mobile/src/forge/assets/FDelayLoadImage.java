package forge.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.Graphics;

//Special wrapper for a texture to be loaded later when it's needed
public class FDelayLoadImage extends FImageComplex {
    private final String filename;
    private Texture texture;

    public FDelayLoadImage(String filename0) {
        filename = filename0;
    }

    @Override
    public float getWidth() {
        return getTexture().getWidth();
    }

    @Override
    public float getHeight() {
        return getTexture().getHeight();
    }

    @Override
    public Texture getTexture() {
        if (texture == null) {
            texture = new Texture(Gdx.files.absolute(filename));
        }
        return texture;
    }

    @Override
    public TextureRegion getTextureRegion() {
        if (texture == null) {
            texture = new Texture(Gdx.files.absolute(filename));
        }
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
        g.drawImage(getTexture(), x, y, w, h);
    }
}
