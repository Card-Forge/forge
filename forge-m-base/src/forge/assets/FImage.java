package forge.assets;

import forge.Forge.Graphics;

public interface FImage {
    float getSourceWidth();
    float getSourceHeight();
    void draw(Graphics g, float x, float y, float w, float h);
}
