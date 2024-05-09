package forge.assets;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public interface FSkinImageInterface extends FImage {
	public void load(Pixmap preferredIcons);
	public TextureRegion getTextureRegion();
    public float getNearestHQWidth(float baseWidth);
    public float getNearestHQHeight(float baseHeight);
}
