package forge.toolbox;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import com.badlogic.gdx.math.Vector2;

import forge.Graphics;
import forge.gui.error.BugReporter;

public abstract class FContainer extends FDisplayObject {
    private final List<FDisplayObject> children = new ArrayList<>();

    public <T extends FDisplayObject> T add(T child) {
        children.add(child);
        return child;
    }

    public <T extends FDisplayObject> boolean remove(T child) {
        return children.remove(child);
    }

    public void clear() {
        children.clear();
    }

    public int indexOf(FDisplayObject child) {
        return children.indexOf(child);
    }

    public FDisplayObject getChildAt(int index) {
        return children.get(index);
    }

    public int getChildCount() {
        return children.size();
    }

    public Iterable<FDisplayObject> getChildren() {
        return children;
    }

    protected void drawBackground(Graphics g) {
    }

    public void draw(Graphics g) {
        try {
            drawBackground(g);
    
            boolean needOverlayDrawn = true;
            for (FDisplayObject child : children) {
                if (child.isVisible()) {
                    if (child.drawAboveOverlay() && needOverlayDrawn) {
                        drawOverlay(g);
                        needOverlayDrawn = false;
                    }
    
                    final boolean disabled = !child.isEnabled();
                    if (disabled) {
                        g.setAlphaComposite(DISABLED_COMPOSITE);
                    }
    
                    g.draw(child);
    
                    if (disabled) {
                        g.resetAlphaComposite();
                    }
    
                    child.drawOnContainer(g); //give child an additional chance to draw additional content on container outside its bounds
                }
            }
    
            if (needOverlayDrawn) {
                drawOverlay(g);
            }
        }
        catch (ConcurrentModificationException ex) {
            //ignore concurrent modification exceptions during render
        }
        catch (Exception ex) {
            BugReporter.reportException(ex);
        }
    }

    protected void drawOverlay(Graphics g) {
    }

    @Override
    public void setBounds(float x, float y, float width, float height) {
        super.setBounds(x, y, width, height);
        doLayout(width, height); //always re-do layout when setBounds used
    }

    @Override
    public void setSize(float width, float height) {
        if (getWidth() == width && getHeight() == height) { return; }

        super.setSize(width, height);
        doLayout(width, height);
    }

    public void revalidate() {
        revalidate(false);
    }
    public void revalidate(boolean forced) {
        if (forced) {
            doLayout(getWidth(), getHeight());
            return;
        }
        float w = getWidth();
        float h = getHeight();
        if (w == 0 || h == 0) { return; } //don't revalidate if size not set yet
        doLayout(w, h);
    }

    protected abstract void doLayout(float width, float height);

    @Override
    public void buildTouchListeners(float screenX, float screenY, List<FDisplayObject> listeners) {
        if (isEnabled() && isVisible() && screenPos.contains(screenX, screenY)) {
            for (int i = children.size() - 1; i >= 0; i--) {
                children.get(i).buildTouchListeners(screenX, screenY, listeners);
            }
            listeners.add(this);
        }
    }

    public final Vector2 getChildRelativePosition(FDisplayObject child) {
        return getChildRelativePosition(child, 0, 0);
    }

    protected Vector2 getChildRelativePosition(FDisplayObject child, float offsetX, float offsetY) {
        for (FDisplayObject c : children) { //check direct children first
            if (child == c) {
                return new Vector2(c.getLeft() + offsetX, c.getTop() + offsetY);
            }
        }
        for (FDisplayObject c : children) { //check each child's children next if possible
            if (c instanceof FContainer) {
                Vector2 pos = ((FContainer)c).getChildRelativePosition(child, c.getLeft() + offsetX, c.getTop() + offsetY);
                if (pos != null) {
                    return pos;
                }
            }
        }
        return null;
    }

    @Override
    public boolean keyDown(int keyCode) {
        //by default, give all enabled children a chance handle keyDown
        for (FDisplayObject c : children) {
            if (c.isEnabled() && c.keyDown(keyCode)) {
                return true;
            }
        }
        return false;
    }
}
