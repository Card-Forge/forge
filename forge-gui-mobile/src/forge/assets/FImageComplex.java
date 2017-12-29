package forge.assets;

import com.badlogic.gdx.graphics.Texture;

public abstract class FImageComplex implements FImage {
    public abstract Texture getTexture();
    public abstract int getRegionX();
    public abstract int getRegionY();
}
