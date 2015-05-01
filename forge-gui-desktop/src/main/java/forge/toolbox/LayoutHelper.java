package forge.toolbox;

import javax.swing.JComponent;

/**
 * Helper class for doing custom layout
 *
 */
public final class LayoutHelper {
    private final int parentWidth, parentHeight;
    private int x, y, lineBottom;
    private final int gapX, gapY;

    public LayoutHelper(final JComponent parent) {
        this(parent, 3, 3);
    }
    public LayoutHelper(final JComponent parent, final int gapX0, final int gapY0) {
        parentWidth = parent.getWidth();
        parentHeight = parent.getHeight();
        gapX = gapX0;
        gapY = gapY0;
    }

    /**
     * Layout component to fill remaining vertical space of parent
     * @param comp
     */
    public void fill(final JComponent comp) {
        newLine(); //start new line if needed
        include(comp, parentWidth, parentHeight - y);
    }

    /**
     * Layout component to fill remaining horizontal space of current line
     * @param comp
     * @param height
     */
    public void fillLine(final JComponent comp, final int height) {
        fillLine(comp, height, 0);
    }

    /**
     * Layout component to fill remaining horizontal space of current line
     * @param comp
     * @param height
     * @param rightPadding
     */
    public void fillLine(final JComponent comp, final int height, final int rightPadding) {
        if (x >= parentWidth) {
            newLine();
        }
        include(comp, parentWidth - x - rightPadding, height);
    }

    /**
     * Include component in layout with a percentage width and fixed height
     * @param comp
     * @param widthPercent
     * @param height
     */
    public void include(final JComponent comp, final float widthPercent, final int height) {
        include(comp, Math.round(parentWidth * widthPercent), height);
    }

    /**
     * Include component in layout with a fixed width and percentage height
     * @param comp
     * @param width
     * @param heightPercent
     */
    public void include(final JComponent comp, final int width, final float heightPercent) {
        include(comp, width, Math.round(parentHeight * heightPercent));
    }

    /**
     * Include component in layout with a percentage width and height
     * @param comp
     * @param widthPercent
     * @param heightPercent
     */
    public void include(final JComponent comp, final float widthPercent, final float heightPercent) {
        include(comp, Math.round(parentWidth * widthPercent), Math.round(parentHeight * heightPercent));
    }

    /**
     * Include component in layout with a fixed width and height
     * @param comp
     * @param width
     * @param height
     */
    public void include(final JComponent comp, int width, final int height) {
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
        comp.setBounds(x, y, width, height);
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
    public void offset(final int dx, final int dy) {
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
    public void newLine(final int dy) {
        x = 0;
        y = lineBottom + gapY + dy;
        lineBottom = y;
    }

    /**
     * @return width of parent
     */
    public int getParentWidth() {
        return parentWidth;
    }

    /**
     * @return width of parent
     */
    public int getParentHeight() {
        return parentHeight;
    }

    /**
     * @return current X
     */
    public int getX() {
        return x;
    }

    /**
     * @return current Y
     */
    public int getY() {
        return y;
    }

    /**
     * @return gap X
     */
    public int getGapX() {
        return gapX;
    }

    /**
     * @return gap Y
     */
    public int getGapY() {
        return gapY;
    }
}
