package forge.assets;

import forge.Forge.Graphics;

public interface FImage {
    float getWidth();
    float getHeight();
    void draw(Graphics g, float x, float y, float w, float h);
}
