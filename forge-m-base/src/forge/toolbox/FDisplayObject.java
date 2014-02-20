package forge.toolbox;

import java.util.ArrayList;

import com.badlogic.gdx.math.Rectangle;

import forge.Forge.Graphics;

public abstract class FDisplayObject {
    private final ArrayList<FDisplayObject> children = new ArrayList<FDisplayObject>();
    private final Rectangle bounds = new Rectangle();

    public void setBounds(float x, float y, float width, float height) {
        boolean layoutChanged = (bounds.width != width || bounds.height != height);
        bounds.set(x, y, width, height);
        if (layoutChanged) {
            doLayout(width, height);
        }
    }
    public float getWidth() {
        return bounds.width;
    }
    public float getHeight() {
        return bounds.height;
    }

    public <T extends FDisplayObject> T add(T child) {
        children.add(child);
        return child;
    }

    public void draw(Graphics g) {
        for (FDisplayObject child : children) {
            child.draw(new Graphics(g, child.bounds.x, child.bounds.y));
        }
    }

    protected abstract void doLayout(float width, float height);
}
