package forge.assets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import forge.Graphics;

public class FTextureRegionImage implements FImage {
    private final TextureRegion textureRegion;

    public FTextureRegionImage(TextureRegion textureRegion0) {
        textureRegion = textureRegion0;
    }

    @Override
    public float getWidth() {
        return textureRegion.getRegionWidth();
    }

    @Override
    public float getHeight() {
        return textureRegion.getRegionHeight();
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        g.drawImage(textureRegion, x, y, w, h);
    }
}
