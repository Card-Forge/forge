package forge.toolbox;

import java.util.List;

import com.badlogic.gdx.math.Rectangle;

import forge.Forge;
import forge.Graphics;
import forge.gui.GuiBase;

public abstract class FDisplayObject {
    protected static final float DISABLED_COMPOSITE = 0.25f;

    private boolean visible = true;
    private boolean enabled = true;
    private boolean rotate90 = false;
    private boolean rotate180 = false;
    private boolean hovered = false;
    private final Rectangle bounds = new Rectangle();
    public final Rectangle screenPos = new Rectangle();

    public void setPosition(float x, float y) {
        bounds.setPosition(x, y);
    }
    public void setSize(float width, float height) {
        bounds.setSize(width, height);
    }
    public void setBounds(float x, float y, float width, float height) {
        bounds.set(x, y, width, height);
    }
    public float getLeft() {
        return bounds.x;
    }
    public void setLeft(float x) {
        bounds.x = x;
    }
    public float getRight() {
        return bounds.x + bounds.width;
    }
    public float getTop() {
        return bounds.y;
    }
    public void setTop(float y) {
        bounds.y = y;
    }
    public float getBottom() {
        return bounds.y + bounds.height;
    }
    public float getWidth() {
        return bounds.width;
    }
    public void setWidth(float width) {
        bounds.width = width;
    }
    public float getHeight() {
        return bounds.height;
    }
    public void setHeight(float height) {
        bounds.height = height;
    }
    public boolean contains(float x, float y) {
        return visible && bounds.contains(x, y);
    }

    public float screenToLocalX(float x) {
        return x - screenPos.x;
    }
    public float screenToLocalY(float y) {
        return y - screenPos.y;
    }
    public float localToScreenX(float x) {
        return x + screenPos.x;
    }
    public float localToScreenY(float y) {
        return y + screenPos.y;
    }

    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean b0) {
        enabled = b0;
    }

    public boolean isVisible() {
        return visible;
    }
    public void setVisible(boolean b0) {
        visible = b0;
    }

    public boolean isHovered() {
        return hovered;
    }
    public void setHovered(boolean b0) {
        hovered = b0;
    }

    public boolean getRotate90() {
        return rotate90;
    }
    public void setRotate90(boolean b0) {
        rotate90 = b0;
    }
    public boolean getRotate180() {
        return rotate180;
    }
    public void setRotate180(boolean b0) {
        rotate180 = b0;
    }

    //these tooltip functions exist for the sake of interfaces, should be overriden if values needed
    public String getToolTipText() {
        return null;
    }
    public void setToolTipText(String s0) {
    }

    //override to return true if drawOverlay should be called on container before drawing this object
    protected boolean drawAboveOverlay() {
        return false;
    }

    //override if anything should be drawn at the container level immediately after this object is drawn
    //this allows drawing outside the bounds of this object
    protected void drawOnContainer(Graphics g) {
    }

    public abstract void draw(Graphics g);
    public void buildTouchListeners(float screenX, float screenY, List<FDisplayObject> listeners) {
        boolean exact = !GuiBase.isAndroid() && (this instanceof FCardPanel);
        if (enabled && visible && screenPos.contains(screenX, screenY)) {
            listeners.add(this);
        }
        //TODO: mouse detection on android?
        if (Forge.afterDBloaded && !GuiBase.isAndroid()) {
            Forge.hoveredCount = listeners.size();
            if (!Forge.getCurrentScreen().toString().contains("Match"))
                Forge.hoveredCount = 1;
            if (exact) {
                setHovered(this.enabled && this.visible && ((FCardPanel) this).renderedCardContains(screenToLocalX(screenX), screenToLocalY(screenY)) && Forge.hoveredCount < 2);
            } else {
                setHovered(this.enabled && this.visible && this.screenPos.contains(screenX, screenY) && Forge.hoveredCount < 2);
            }
        }
    }

    public boolean press(float x, float y) {
        return false;
    }

    public boolean longPress(float x, float y) {
        return false;
    }

    public boolean release(float x, float y) {
        return false;
    }

    public boolean tap(float x, float y, int count) {
        return false;
    }

    public boolean flick(float x, float y) {
        return false;
    }

    public boolean fling(float velocityX, float velocityY) {
        return false;
    }

    public boolean flingStop(float x, float y) {
        return false;
    }

    public boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
        return false;
    }

    public boolean panStop(float x, float y) {
        return false;
    }

    public boolean zoom(float x, float y, float amount) {
        return false;
    }

    public boolean keyDown(int keyCode) {
        return false;
    }
}
