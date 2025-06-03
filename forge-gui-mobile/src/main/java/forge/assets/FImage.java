package forge.assets;

import forge.Graphics;
import forge.localinstance.skin.ISkinImage;

public interface FImage extends ISkinImage {
    float getWidth();
    float getHeight();
    void draw(Graphics g, float x, float y, float w, float h);
}
