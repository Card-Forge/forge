package forge.toolbox;

import java.util.List;

import com.badlogic.gdx.math.Vector2;

import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkinColor;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.PhysicsObject;
import forge.util.Utils;

public abstract class FScrollPane extends FContainer {
    private static final float FLING_DECEL = 750f;
    private static final FSkinColor INDICATOR_COLOR = FSkinColor.get(FSkinColor.Colors.CLR_TEXT).alphaColor(0.7f);
    private static final float INDICATOR_SIZE = Utils.scale(5);
    private static final float INDICATOR_MARGIN = Utils.scale(3);

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

    public float getScrollHeight() {
        return scrollBounds.height;
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
        scrollIntoView(child, 0);
    }
    public void scrollIntoView(FDisplayObject child, float margin) {
        Vector2 childPos = getChildRelativePosition(child);
        if (childPos == null) { return; } //do nothing if not a valid child

        scrollIntoView(childPos.x, childPos.y, child.getWidth(), child.getHeight(), margin);
    }
    public void scrollIntoView(float childLeft, float childTop, float childWidth, float childHeight, float margin) {
        float childRight = childLeft + childWidth;
        float childBottom = childTop + childHeight;

        float dx = 0;
        if (childLeft < margin) {
            dx = childLeft - margin;
        }
        else if (childRight > getWidth() - margin) {
            dx = childRight - getWidth() + margin;
        }
        float dy = 0;
        if (childTop < margin) {
            dy = childTop - margin;
        }
        else if (childBottom > getHeight() - margin) {
            dy = childBottom - getHeight() + margin;
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
        try {
            for (FDisplayObject obj : getChildren()) {
                if (obj != null) {
                    if (obj instanceof FCardPanel) { // don't animate while moving the field
                        ((FCardPanel) obj).getCard().updateNeedsTransformAnimation(false);
                        ((FCardPanel) obj).getCard().updateNeedsUntapAnimation(false);
                        ((FCardPanel) obj).getCard().updateNeedsTapAnimation(false);
                    }
                    obj.setPosition(obj.getLeft() + dx, obj.getTop() + dy);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    protected void doLayout(float width, float height) {
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

    @Override
    protected void drawOverlay(Graphics g) {
        try {
            boolean isFieldZoneView = toString().contains("VField")||toString().contains("VZoneDisplay");
            //if ForgePreferences.FPref.UI_ENABLE_MATCH_SCROLL_INDICATOR is missing this will return NPE and could cause lockup on Startup
            if (!FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_ENABLE_MATCH_SCROLL_INDICATOR))
                return;
            if (!isFieldZoneView)
                return;
            //TODO: Consider other ways to indicate scroll potential that fade in and out based on input
            //draw triangles indicating scroll potential
            if (scrollLeft > 0) {
                float x = INDICATOR_MARGIN;
                float y = getHeight() / 2;
                g.fillTriangle(INDICATOR_COLOR, x, y, x + INDICATOR_SIZE, y - INDICATOR_SIZE, x + INDICATOR_SIZE, y + INDICATOR_SIZE);
            }
            if (scrollLeft < getMaxScrollLeft()) {
                float x = getWidth() - INDICATOR_MARGIN;
                float y = getHeight() / 2;
                g.fillTriangle(INDICATOR_COLOR, x, y, x - INDICATOR_SIZE, y - INDICATOR_SIZE, x - INDICATOR_SIZE, y + INDICATOR_SIZE);
            }
            if (scrollTop > 0) {
                float x = getWidth() / 2;
                float y = INDICATOR_MARGIN;
                g.fillTriangle(INDICATOR_COLOR, x, y, x - INDICATOR_SIZE, y + INDICATOR_SIZE, x + INDICATOR_SIZE, y + INDICATOR_SIZE);
            }
            if (scrollTop < getMaxScrollTop()) {
                float x = getWidth() / 2;
                float y = getHeight() - INDICATOR_MARGIN;
                g.fillTriangle(INDICATOR_COLOR, x, y, x - INDICATOR_SIZE, y - INDICATOR_SIZE, x + INDICATOR_SIZE, y - INDICATOR_SIZE);
            }
        } catch (Exception e) {}
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

    private class FlingAnimation extends ForgeAnimation {
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
                return setScrollPositions(pos.x, pos.y) && physicsObj.isMoving();
            }

            //end fling animation if can't scroll anymore or physics object is no longer moving
            return false;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            activeFlingAnimation = null;
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
    public void buildTouchListeners(float screenX, float screenY, List<FDisplayObject> listeners) {
        //if fling animation active, stop it and prevent child controls handling touch events before next touch down unless already moving really slow
        if (activeFlingAnimation != null) {
            boolean suppressEvent = activeFlingAnimation.physicsObj.getVelocity().len() > Utils.AVG_FINGER_HEIGHT;
            activeFlingAnimation.physicsObj.stop();
            if (suppressEvent) {
                listeners.add(this);
                return;
            }
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

    protected void startClip(Graphics g) {
        g.startClip(0, 0, getWidth(), getHeight());
    }

    @Override
    public void draw(Graphics g) {
        startClip(g);
        super.draw(g);
        g.endClip();
    }
}
