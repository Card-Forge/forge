package forge.assets;

import com.badlogic.gdx.math.Vector2;

import forge.Forge.Graphics;

public interface FImage {
    Vector2 getSize();
	void draw(Graphics g, float x, float y);
	void draw(Graphics g, float x, float y, float w, float h);
}
