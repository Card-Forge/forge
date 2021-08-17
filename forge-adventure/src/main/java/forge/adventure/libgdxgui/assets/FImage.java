package forge.adventure.libgdxgui.assets;

import forge.adventure.libgdxgui.Graphics;
import forge.localinstance.skin.ISkinImage;

public interface FImage extends ISkinImage {
    float getWidth();
    float getHeight();
    void draw(Graphics g, float x, float y, float w, float h);
}
