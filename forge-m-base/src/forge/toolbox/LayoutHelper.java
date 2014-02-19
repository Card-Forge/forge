package forge.toolbox;

import com.badlogic.gdx.scenes.scene2d.Actor;

/** 
 * Helper class for doing custom layout
 *
 */
public final class LayoutHelper {
    private final float parentWidth, parentHeight;
    private float x, y, lineBottom, gapX, gapY;

    public LayoutHelper(float parentWidth0, float parentHeight0) {
        this(parentWidth0, parentHeight0, 3, 3);
    }
    public LayoutHelper(float parentWidth0, float parentHeight0, float gapX0, float gapY0) {
        parentWidth = parentWidth0;
        parentHeight = parentHeight0;
        gapX = gapX0;
        gapY = gapY0;
    }

    /**
     * Layout actoronent to fill remaining vertical space of parent
     * @param actor
     */
    public void fill(final Actor actor) {
        newLine(); //start new line if needed
        include(actor, parentWidth, parentHeight - y);
    }

    /**
     * Layout actoronent to fill remaining horizontal space of current line
     * @param actor
     * @param height
     */
    public void fillLine(final Actor actor, float height) {
        fillLine(actor, height, 0);
    }

    /**
     * Layout actoronent to fill remaining horizontal space of current line
     * @param actor
     * @param height
     * @param rightPadding
     */
    public void fillLine(final Actor actor, float height, float rightPadding) {
        if (x >= parentWidth) {
            newLine();
        }
        include(actor, parentWidth - x - rightPadding, height);
    }

    /**
     * Include actoronent in layout with a percentage width and fixed height
     * @param actor
     * @param widthPercent
     * @param height
     */
    public void include(final Actor actor, double widthPercent, float height) {
        include(actor, parentWidth * widthPercent, height);
    }

    /**
     * Include actoronent in layout with a fixed width and percentage height
     * @param actor
     * @param width
     * @param heightPercent
     */
    public void include(final Actor actor, float width, double heightPercent) {
        include(actor, width, parentHeight * heightPercent);
    }

    /**
     * Include actoronent in layout with a percentage width and height
     * @param actor
     * @param widthPercent
     * @param heightPercent
     */
    public void include(final Actor actor, double widthPercent, double heightPercent) {
        include(actor, parentWidth * widthPercent, parentHeight * heightPercent);
    }

    /**
     * Include actoronent in layout with a fixed width and height
     * @param actor
     * @param width
     * @param height
     */
    public void include(final Actor actor, float width, float height) {
        if (width <= 0 || height <= 0) { return; }

        if (x + width > parentWidth) {
            newLine();
            if (width > parentWidth) {
                width = parentWidth;
            }
        }
        if (y + height > parentHeight) {
            y = parentHeight - height;
            if (y >= parentHeight) { return; }
        }
        actor.setBounds(x, y, width, height);
        x += width + gapX;
        if (y + height > lineBottom) {
            lineBottom = y + height;
        }
    }

    /**
     * Offset current layout helper position
     * @param dx
     * @param dy
     */
    public void offset(float dx, float dy) {
        x += dx;
        y += dy;
    }

    /**
     * Start new line of layout
     */
    public void newLine() {
        if (lineBottom == y) { return; }
        x = 0;
        y = lineBottom + gapY;
        lineBottom = y;
    }

    /**
     * Start new line of layout
     */
    public void newLine(float dy) {
        x = 0;
        y = lineBottom + gapY + dy;
        lineBottom = y;
    }

    /**
     * @return width of parent
     */
    public float getParentWidth() {
        return parentWidth;
    }

    /**
     * @return width of parent
     */
    public float getParentHeight() {
        return parentHeight;
    }

    /**
     * @return current X
     */
    public float getX() {
        return x;
    }

    /**
     * @return current Y
     */
    public float getY() {
        return y;
    }

    /**
     * @return gap X
     */
    public float getGapX() {
        return gapX;
    }

    /**
     * @return gap Y
     */
    public float getGapY() {
        return gapY;
    }
}
