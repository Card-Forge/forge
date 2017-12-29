package forge.util;

import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;

/** 
 * Helper class for doing custom layout
 *
 */
public final class LayoutHelper {
    private final float parentWidth, parentHeight;
    private float x, y, lineBottom, gapX, gapY;

    public LayoutHelper(FContainer parent) {
        this(parent, 3, 3);
    }
    public LayoutHelper(FContainer parent, float gapX0, float gapY0) {
        parentWidth = parent.getWidth();
        parentHeight = parent.getHeight();
        gapX = gapX0;
        gapY = gapY0;
    }

    /**
     * Layout object to fill remaining vertical space of parent
     * @param obj
     */
    public void fill(final FDisplayObject obj) {
        newLine(); //start new line if needed
        include(obj, parentWidth, parentHeight - y);
    }

    /**
     * Layout object to fill remaining horizontal space of current line
     * @param obj
     * @param height
     */
    public void fillLine(final FDisplayObject obj, float height) {
        fillLine(obj, height, 0);
    }

    /**
     * Layout object to fill remaining horizontal space of current line
     * @param obj
     * @param height
     * @param rightPadding
     */
    public void fillLine(final FDisplayObject obj, float height, float rightPadding) {
        if (x >= parentWidth) {
            newLine();
        }
        include(obj, parentWidth - x - rightPadding, height);
    }

    /**
     * Include object in layout with a percentage width and fixed height
     * @param obj
     * @param widthPercent
     * @param height
     */
    public void include(final FDisplayObject obj, double widthPercent, float height) {
        include(obj, Math.round(parentWidth * widthPercent), height);
    }

    /**
     * Include object in layout with a fixed width and percentage height
     * @param obj
     * @param width
     * @param heightPercent
     */
    public void include(final FDisplayObject obj, float width, double heightPercent) {
        include(obj, width, Math.round(parentHeight * heightPercent));
    }

    /**
     * Include object in layout with a percentage width and height
     * @param obj
     * @param widthPercent
     * @param heightPercent
     */
    public void include(final FDisplayObject obj, double widthPercent, double heightPercent) {
        include(obj, Math.round(parentWidth * widthPercent), Math.round(parentHeight * heightPercent));
    }

    /**
     * Include object in layout with a fixed width and height
     * @param obj
     * @param width
     * @param height
     */
    public void include(final FDisplayObject obj, float width, float height) {
        if (width <= 0 || height <= 0) { return; }

        if (x + width > parentWidth + 1) { //+1 to avoid wrapping from rounding error
            newLine();
            if (width > parentWidth) {
                width = parentWidth;
            }
        }
        if (y + height > parentHeight) {
            y = parentHeight - height;
            if (y >= parentHeight) { return; }
        }
        obj.setBounds(x, y, width, height);
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
     * @return remaining width on current line
     */
    public float getRemainingLineWidth() {
        if (x >= parentWidth) {
            newLine();
        }
        return parentWidth - x;
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
