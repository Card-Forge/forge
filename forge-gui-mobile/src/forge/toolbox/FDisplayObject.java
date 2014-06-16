package forge.toolbox;

import java.util.ArrayList;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import forge.Graphics;

public abstract class FDisplayObject {
    protected static final float DISABLED_COMPOSITE = 0.25f;

    private boolean visible = true;
    private boolean enabled = true;
    private final Rectangle bounds = new Rectangle();
    private final Vector2 screenPosition = new Vector2();

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

    public Vector2 getScreenPosition() {
        return screenPosition;
    }
    public void setScreenPosition(float x, float y) { //only call from Graphics when drawn
        screenPosition.set(x, y);
    }

    public float screenToLocalX(float x) {
        return x - screenPosition.x;
    }
    public float screenToLocalY(float y) {
        return y - screenPosition.y;
    }
    public float localToScreenX(float x) {
        return x + screenPosition.x;
    }
    public float localToScreenY(float y) {
        return y + screenPosition.y;
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

    public abstract void draw(Graphics g);
    public void buildTouchListeners(float screenX, float screenY, ArrayList<FDisplayObject> listeners) {
        if (enabled && contains(getLeft() + screenToLocalX(screenX), getTop() + screenToLocalY(screenY))) {
            listeners.add(this);
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
