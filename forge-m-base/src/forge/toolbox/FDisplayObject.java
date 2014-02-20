package forge.toolbox;

import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import forge.Forge.Graphics;

public abstract class FDisplayObject implements GestureListener {
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

    public abstract void draw(Graphics g);

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
        return false;
    }

    @Override
    public boolean longPress(float x, float y) {
        return false;
    }

    @Override
    public boolean fling(float velocityX, float velocityY, int button) {
        return false;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        return false;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        return false;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }
}
