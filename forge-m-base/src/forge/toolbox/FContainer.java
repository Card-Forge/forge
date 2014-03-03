package forge.toolbox;

import java.util.ArrayList;

import forge.Forge.Graphics;

public abstract class FContainer extends FDisplayObject {
    private final ArrayList<FDisplayObject> children = new ArrayList<FDisplayObject>();

    public <T extends FDisplayObject> T add(T child) {
        children.add(child);
        return child;
    }

    public Iterable<FDisplayObject> getChildren() {
        return children;
    }

    protected void drawBackground(Graphics g) {
    }

    public final void draw(Graphics g) {
        drawBackground(g);
        for (FDisplayObject child : children) {
            if (child.isVisible()) {
                g.draw(child);
            }
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

    @Override
    public final void buildTouchListeners(float x, float y, ArrayList<FDisplayObject> listeners) {
        if (isEnabled() && contains(x, y)) {
            for (FDisplayObject child : children) {
                child.buildTouchListeners(x, y, listeners);
            }
            listeners.add(this);
        }
    }
}
