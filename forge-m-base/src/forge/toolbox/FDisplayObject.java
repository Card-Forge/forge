package forge.toolbox;

import java.util.ArrayList;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import forge.Forge.Graphics;

public abstract class FDisplayObject {
    private final Rectangle bounds = new Rectangle();

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
    public float getRight() {
        return bounds.x + bounds.width;
    }
    public float getTop() {
        return bounds.y;
    }
    public float getBottom() {
        return bounds.y + bounds.height;
    }
    public float getWidth() {
        return bounds.width;
    }
    public float getHeight() {
        return bounds.height;
    }
    public boolean contains(float x, float y) {
        return bounds.contains(x, y);
    }

    public abstract void draw(Graphics g);

    public void buildObjectsContainingPoint(float x, float y, ArrayList<FDisplayObject> objs) {
        if (contains(x, y)) {
            objs.add(this);
        }
    }

    public boolean touchDown(float x, float y) {
        return false;
    }

    public boolean touchUp(float x, float y) {
        return false;
    }

    public boolean tap(float x, float y, int count) {
        return false;
    }

    public boolean longPress(float x, float y) {
        return tap(x, y, 1); //treat longPress the same as a tap by default
    }

    public boolean fling(float velocityX, float velocityY) {
        return false;
    }

    public boolean pan(float x, float y, float deltaX, float deltaY) {
        return false;
    }

    public boolean panStop(float x, float y) {
        return false;
    }

    public boolean zoom(float initialDistance, float distance) {
        return false;
    }

    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }
}
