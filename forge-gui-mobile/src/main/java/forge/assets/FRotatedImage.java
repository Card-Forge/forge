package forge.assets;

import com.badlogic.gdx.graphics.Texture;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.Graphics;

public class FRotatedImage extends FImageComplex {
    private final Texture texture;
    private final int srcX, srcY, srcWidth, srcHeight;
    private final boolean clockwise;

    public FRotatedImage(Texture texture0, int srcX0, int srcY0, int srcWidth0, int srcHeight0, boolean clockwise0) {
        texture = texture0;
        srcX = srcX0;
        srcY = srcY0;
        srcWidth = srcWidth0;
        srcHeight = srcHeight0;
        clockwise = clockwise0;
    }

    @Override
    public float getWidth() {
        return srcHeight; //width and height are swapped since image rotated
    }

    @Override
    public float getHeight() {
        return srcWidth;
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
        return srcX;
    }

    @Override
    public int getRegionY() {
        return srcY;
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        float originX, originY, rotation;
        if (clockwise) {
            originX = x + w / 2;
            originY = y + w / 2;
            rotation = -90;
        }
        else {
            originX = x + h / 2;
            originY = y + h / 2;
            rotation = 90;
        }
        g.drawRotatedImage(texture, x, y, h, w, originX, originY, srcX, srcY, srcWidth, srcHeight, rotation);
    }
}
