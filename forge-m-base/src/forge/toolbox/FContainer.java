package forge.toolbox;

import java.util.ArrayList;

import forge.Forge.Graphics;

public abstract class FContainer extends FDisplayObject {
    private final ArrayList<FDisplayObject> children = new ArrayList<FDisplayObject>();

    public <T extends FDisplayObject> T add(T child) {
        children.add(child);
        return child;
    }

    protected void drawBackground(Graphics g) {
    }

    public final void draw(Graphics g) {
        drawBackground(g);
        for (FDisplayObject child : children) {
            child.draw(new Graphics(g, child.getLeft(), child.getTop()));
        }
        drawOverlay(g);
    }

    protected void drawOverlay(Graphics g) {
    }

    @Override
    public void setBounds(float x, float y, float width, float height) {
        boolean layoutChanged = (getWidth() != width || getHeight() != height);
        super.setBounds(x, y, width, height);
        if (layoutChanged) {
            doLayout(width, height);
        }
    }

    @Override
    public void setSize(float width, float height) {
        boolean layoutChanged = (getWidth() != width || getHeight() != height);
        super.setSize(width, height);
        if (layoutChanged) {
            doLayout(width, height);
        }
    }

    protected abstract void doLayout(float width, float height);
}
