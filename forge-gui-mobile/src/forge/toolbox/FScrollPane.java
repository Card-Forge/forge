package forge.toolbox;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;

import forge.Forge.Animation;
import forge.Forge.Graphics;
import forge.util.PhysicsObject;

public abstract class FScrollPane extends FContainer {
    private static float FLING_DECEL = 750f;
    private static long FLING_STOP_DELAY = 500000000l; //half a second

    private float scrollLeft, scrollTop;
    private ScrollBounds scrollBounds;

    public FScrollPane() {
        scrollBounds = new ScrollBounds();
    }

    public float getScrollLeft() {
        return scrollLeft;
    }

    public void setScrollLeft(float scrollLeft0) {
        setScrollPositions(scrollLeft0, scrollTop);
    }

    public float getScrollTop() {
        return scrollTop;
    }

    public void setScrollTop(float scrollTop0) {
        setScrollPositions(scrollLeft, scrollTop0);
    }

    public float getScrollWidth() {
        return scrollBounds.width;
    }

    public void setScrollWidth(float scrollWidth0) {
        scrollBounds.width = scrollWidth0;
    }

    public float getScrollHeight() {
        return scrollBounds.height;
    }

    public void setScrollHeight(float scrollHeight0) {
        scrollBounds.height = scrollHeight0;
    }

    public float getMaxScrollLeft() {
        return getScrollWidth() - getWidth();
    }

    public float getMaxScrollTop() {
        return getScrollHeight() - getHeight();
    }

    public void scrollToLeft() {
        setScrollPositions(0, scrollTop);
    }

    public void scrollToRight() {
        setScrollPositions(getMaxScrollLeft(), scrollTop);
    }

    public void scrollToTop() {
        setScrollPositions(scrollLeft, 0);
    }

    public void scrollToBottom() {
        setScrollPositions(scrollLeft, getMaxScrollTop());
    }

    public void scrollIntoView(FDisplayObject child) {
        Vector2 childPos = getChildRelativePosition(child);
        if (childPos == null) { return; } //do nothing if not a valid child

        float childLeft = childPos.x;
        float childRight = childLeft + child.getWidth();
        float childTop = childPos.y;
        float childBottom = childTop + child.getHeight();

        float dx = 0;
        if (childLeft < 0) {
            dx = childLeft;
        }
        else if (childRight > getWidth()) {
            dx = childRight - getWidth();
        }
        float dy = 0;
        if (childTop < 0) {
            dy = childTop;
        }
        else if (childBottom > getHeight()) {
            dy = childBottom - getHeight();
        }

        if (dx == 0 && dy == 0) { return; }

        setScrollPositions(scrollLeft + dx, scrollTop + dy);
    }

    private boolean setScrollPositions(float scrollLeft0, float scrollTop0) {
        if (scrollLeft0 < 0) {
            scrollLeft0 = 0;
        }
        else {
            float maxScrollLeft = getMaxScrollLeft();
            if (scrollLeft0 > maxScrollLeft) {
                scrollLeft0 = maxScrollLeft;
            }
        }
        if (scrollTop0 < 0) {
            scrollTop0 = 0;
        }
        else {
            float maxScrollTop = getMaxScrollTop();
            if (scrollTop0 > maxScrollTop) {
                scrollTop0 = maxScrollTop;
            }
        }
        float dx = scrollLeft - scrollLeft0;
        float dy = scrollTop - scrollTop0;
        if (dx == 0 && dy == 0) { return false; } //do nothing if scroll didn't change

        scrollLeft = scrollLeft0;
        scrollTop = scrollTop0;

        //shift position of all children based on change in scroll positions
        for (FDisplayObject obj : getChildren()) {
            obj.setPosition(obj.getLeft() + dx, obj.getTop() + dy);
        }
        return true;
    }

    @Override
    protected final void doLayout(float width, float height) {
        scrollBounds = layoutAndGetScrollBounds(width, height);
        scrollBounds.increaseWidthTo(width); //ensure scroll bounds extends at least to visible bounds
        scrollBounds.increaseHeightTo(height);

        //attempt to restore current scroll positions after reseting them in order to update positions of newly laid out stuff
        float oldScrollLeft = scrollLeft;
        float oldScrollTop = scrollTop;
        scrollLeft = 0;
        scrollTop = 0;
        setScrollPositionsAfterLayout(oldScrollLeft, oldScrollTop);
    }

    //allow overriding to adjust what scroll positions are restored after layout
    protected void setScrollPositionsAfterLayout(float scrollLeft0, float scrollTop0) {
        setScrollPositions(scrollLeft0, scrollTop0);
    }

    protected abstract ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight);

    public static class ScrollBounds {
        private float width, height;

        protected ScrollBounds() {
            this(0, 0);
        }
        public ScrollBounds(float width0, float height0) {
            width = width0;
            height = height0;
        }

        public float getWidth() {
            return width;
        }

        public float getHeight() {
            return height;
        }

        //increase width the given value if it's higher
        public void increaseWidthTo(float width0) {
            if (width0 > width) {
                width = width0;
            }
        }

        //increase height to the given value if it's higher
        public void increaseHeightTo(float height0) {
            if (height0 > height) {
                height = height0;
            }
        }
    }

    private FlingAnimation activeFlingAnimation;
    private long lastFlingStopTime;

    private class FlingAnimation extends Animation {
        private final PhysicsObject physicsObj;

        private FlingAnimation(float velocityX, float velocityY) {
            physicsObj = new PhysicsObject(new Vector2(scrollLeft, scrollTop), new Vector2(velocityX, velocityY));
            physicsObj.setDecel(FLING_DECEL, FLING_DECEL);
        }

        @Override
        protected boolean advance(float dt) {
            if (physicsObj.isMoving()) { //avoid storing last fling stop time if scroll manually stopped
                physicsObj.advance(dt);
                Vector2 pos = physicsObj.getPosition();
                if (setScrollPositions(pos.x, pos.y) && physicsObj.isMoving()) {
                    return true;
                }
            }

            //end fling animation if can't scroll anymore or physics object is no longer moving
            lastFlingStopTime = TimeUtils.nanoTime();
            activeFlingAnimation = null;
            return false;
        }
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        if (Math.abs(velocityY) > Math.abs(velocityX)) {
            if (getMaxScrollTop() == 0) {
                return false; //if fling is more vertical and can't scroll vertically, don't scroll at all
            }
        }
        else if (getMaxScrollLeft() == 0) {
            return false; //if fling is more horizontal and can't scroll horizontally, don't scroll at all
        }

        velocityX = -velocityX; //reverse velocities to account for scroll moving in opposite direction
        velocityY = -velocityY;

        if (activeFlingAnimation == null) {
            activeFlingAnimation = new FlingAnimation(velocityX, velocityY);
            activeFlingAnimation.start();
        }
        else { //update existing animation with new velocity if needed
            activeFlingAnimation.physicsObj.getVelocity().set(velocityX, velocityY);
            activeFlingAnimation.physicsObj.setDecel(FLING_DECEL, FLING_DECEL);
        }
        return true;
    }

    @Override
    public void buildTouchListeners(float screenX, float screenY, ArrayList<FDisplayObject> listeners) {
        //if fling animation active, stop it and prevent child controls handling touch events before next touch down
        if (activeFlingAnimation != null) {
            activeFlingAnimation.physicsObj.stop();
            listeners.add(this);
            return;
        }

        //if fling ended just shortly before, still prevent touch events on child controls
        //in case user tapped just too late to stop scrolling before scroll bounds reached
        if (lastFlingStopTime > 0) {
            if (TimeUtils.nanoTime() - lastFlingStopTime < FLING_STOP_DELAY) {
                listeners.add(this);
                return;
            }
            lastFlingStopTime = 0; //don't need to hold onto this after it has elapsed
        }
        super.buildTouchListeners(screenX, screenY, listeners);
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
        if (getMaxScrollTop() <= 0 && (moreVertical || y < 0 || y >= getHeight() || Math.abs(deltaY) > Math.abs(deltaX))) {
            //if can't scroll vertically, don't scroll at all if pan is more vertical
            //or current position is above or below this scroll pane
            return false;
        }
        if (getMaxScrollLeft() <= 0 && (!moreVertical || x < 0 || x >= getWidth() || Math.abs(deltaX) > Math.abs(deltaY))) {
            //if can't scroll horizontally, don't scroll at all if pan is more horizontal
            //or current position is left or right of this scroll pane
            return false;
        }
        setScrollPositions(scrollLeft - deltaX, scrollTop - deltaY);
        return true;
    }

    @Override
    public void draw(Graphics g) {
        g.startClip(0, 0, getWidth(), getHeight());
        super.draw(g);
        g.endClip();
    }
}
