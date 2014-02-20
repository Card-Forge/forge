package forge.toolbox;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;

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
            g.draw(child);
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
    public boolean touchDown(float x, float y, int pointer, int button) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean longPress(float x, float y) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean fling(float velocityX, float velocityY, int button) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2,
            Vector2 pointer1, Vector2 pointer2) {
        // TODO Auto-generated method stub
        return false;
    }
}
