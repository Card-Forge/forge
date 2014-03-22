package forge.toolbox;

import forge.Forge.Graphics;

public abstract class FScrollPane extends FContainer {
    private float scrollLeft, scrollTop;
    private ScrollBounds scrollBounds;

    protected FScrollPane() {
        scrollBounds = new ScrollBounds();
    }

    public float getScrollLeft() {
        return scrollLeft;
    }

    public float getScrollTop() {
        return scrollTop;
    }

    public float getScrollWidth() {
        return scrollBounds.width;
    }

    public float getScrollHeight() {
        return scrollBounds.height;
    }

    @Override
    protected final void doLayout(float width, float height) {
        scrollBounds = layoutAndGetScrollBounds(width, height);
        scrollBounds.increaseWidthTo(width); //ensure scroll bounds extends at least to visible bounds
        scrollBounds.increaseHeightTo(height);
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

    public final void draw(Graphics g) {
        g.startClip(0, 0, getWidth(), getHeight());
        super.draw(g);
        g.endClip();
    }
}
