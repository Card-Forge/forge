package forge.assets;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public abstract class FImageComplex implements FImage {
    public abstract Texture getTexture();
    public abstract TextureRegion getTextureRegion();
    public abstract int getRegionX();
    public abstract int getRegionY();
}
