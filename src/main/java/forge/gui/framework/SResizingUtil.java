package forge.gui.framework;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import forge.view.FView;

/**
 * Package-private utilities for resizing drag behavior using
 * the draggable panels registered in FView.
 * 
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public final class SResizingUtil {
    private static final List<DragCell> LEFT_PANELS = new ArrayList<DragCell>();
    private static final List<DragCell> RIGHT_PANELS = new ArrayList<DragCell>();
    private static final List<DragCell> TOP_PANELS = new ArrayList<DragCell>();
    private static final List<DragCell> BOTTOM_PANELS = new ArrayList<DragCell>();

    private static int dX;
    private static int evtX;
    private static int dY;
    private static int evtY;

    /** Minimum cell width. */
    public static final int W_MIN = 100;
    /** Minimum cell height. */
    public static final int H_MIN = 75;

    private static final MouseListener MAD_RESIZE_X = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            SResizingUtil.startResizeX(e);
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            SResizingUtil.endResize();
        }
    };

    private static final MouseListener MAD_RESIZE_Y = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            SResizingUtil.startResizeY(e);
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            SResizingUtil.endResize();
        }
    };

    private static final MouseMotionListener MMA_DRAG_X = new MouseMotionAdapter() {
        @Override
        public void mouseDragged(final MouseEvent e) {
            SResizingUtil.resizeX(e);
        }
    };

    private static final MouseMotionListener MMA_DRAG_Y = new MouseMotionAdapter() {
        @Override
        public void mouseDragged(final MouseEvent e) {
            SResizingUtil.resizeY(e);
        }
    };

    private static final ComponentListener CAD_RESIZE = new ComponentAdapter() {
        @Override
        public void componentResized(final ComponentEvent e) {
            resizeWindow();
            SRearrangingUtil.updateBorders();
        }
    };

    /** */
    public static void resizeWindow() {
        final List<DragCell> cells = FView.SINGLETON_INSTANCE.getDragCells();
        final JPanel pnlContent = FView.SINGLETON_INSTANCE.getPnlContent();
        final JPanel pnlInsets = FView.SINGLETON_INSTANCE.getPnlInsets();

        pnlInsets.setBounds(FView.SINGLETON_INSTANCE.getFrame().getContentPane().getBounds());
        pnlInsets.validate();

        final int w = pnlContent.getWidth();
        final int h = pnlContent.getHeight();

        double roughVal = 0;
        int smoothVal = 0;

        // This is the core of the pixel-perfect layout. To avoid ±1 px errors on borders
        // from rounding individual panels, the intermediate values (exactly accurate, in %)
        // for width and height are rounded based on comparison to other panels in the
        // layout.  This is to avoid errors such as:
        // 10% = 9.8px -> 10px -> x 3 = 30px
        // 30% = 29.4px -> 29px (!)
        for (final DragCell cellA : cells) {
            roughVal = cellA.getRoughX() * w + cellA.getRoughW() * w;

            smoothVal = (int) Math.round(roughVal);
            for (final DragCell cellB : cells) {
                if ((cellB.getRoughX() * w + cellB.getRoughW() * w) == roughVal) {
                    cellB.setSmoothW(smoothVal - (int) Math.round(cellB.getRoughX() * w));
                }
            }
            cellA.setSmoothW(smoothVal - (int) Math.round(cellA.getRoughX() * w));

            roughVal = cellA.getRoughY() * h + cellA.getRoughH() * h;
            smoothVal = (int) Math.round(roughVal);
            for (final DragCell cellB : cells) {
                if (cellB.getRoughY() * h + cellB.getRoughH() * h == roughVal) {
                    cellB.setSmoothH(smoothVal - (int) Math.round(cellB.getRoughY() * h));
                }
            }
            cellA.setSmoothH(smoothVal - (int) Math.round(cellA.getRoughY() * h));

            // X and Y coordinate can be rounded as usual.
            cellA.setSmoothX((int) Math.round(cellA.getRoughX() * w));
            cellA.setSmoothY((int) Math.round(cellA.getRoughY() * h));
            pnlContent.add(cellA);
        }

        // Lock in final bounds and build heads.
        for (final DragCell t : cells) {
            t.setSmoothBounds();
            t.validate();
            t.refresh();
        }

        cells.clear();
    }

    /** @param e &emsp; {@link java.awt.event.MouseEvent} */
    public static void resizeX(final MouseEvent e) {
        dX = (int) e.getLocationOnScreen().getX() - evtX;
        evtX = (int) e.getLocationOnScreen().getX();
        boolean leftLock = false;
        boolean rightLock = false;

        for (final DragCell t : LEFT_PANELS) {
            if ((t.getW() + dX) < W_MIN) { leftLock = true; break; }
        }

        for (final DragCell t : RIGHT_PANELS) {
            if ((t.getW() - dX) < W_MIN) { rightLock = true; break; }
        }

        if (dX < 0 && leftLock) { return; }
        if (dX > 0 && rightLock) { return; }

        for (final DragCell t : LEFT_PANELS) {
            t.setBounds(t.getX(), t.getY(), t.getW() + dX, t.getH());
            t.refresh();
        }

        for (final DragCell t : RIGHT_PANELS) {
            t.setBounds(t.getX() + dX, t.getY(), t.getW() - dX, t.getH());
            t.refresh();
        }
    }

    /** @param e &emsp; {@link java.awt.event.MouseEvent} */
    public static void resizeY(final MouseEvent e) {
        dY = (int) e.getLocationOnScreen().getY() - evtY;
        evtY = (int) e.getLocationOnScreen().getY();
        boolean topLock = false;
        boolean bottomLock = false;

        for (final DragCell t : TOP_PANELS) {
            if ((t.getH() + dY) < H_MIN) { topLock = true; break; }
        }

        for (final DragCell t : BOTTOM_PANELS) {
            if ((t.getH() - dY) < H_MIN) { bottomLock = true; break; }
        }

        if (dY < 0 && topLock) { return; }
        if (dY > 0 && bottomLock) { return; }

        for (final DragCell t : TOP_PANELS) {
            t.setBounds(t.getX(), t.getY(), t.getW(), t.getH() + dY);
            t.revalidate();
            t.repaintThis();
        }

        for (final DragCell t : BOTTOM_PANELS) {
            t.setBounds(t.getX(), t.getY() + dY, t.getW(), t.getH() - dY);
            t.revalidate();
            t.repaintThis();
        }
    }

    /** @param e &emsp; {@link java.awt.event.MouseEvent} */
    public static void startResizeX(final MouseEvent e) {
        evtX = (int) e.getLocationOnScreen().getX();
        LEFT_PANELS.clear();
        RIGHT_PANELS.clear();

        final DragCell src = (DragCell) ((JPanel) e.getSource()).getParent();
        final int srcX2 = src.getAbsX2();

        int limTop = -1;
        int limBottom = Integer.MAX_VALUE;
        int tempX = -1;
        int tempX2 = -1;
        int tempY = -1;
        int tempY2 = -1;

        // Add all panels who share a left or right edge with the
        // same coordinate as the right edge of the source panel.
        for (final DragCell t : FView.SINGLETON_INSTANCE.getDragCells()) {
            tempX = t.getAbsX();
            tempX2 = t.getAbsX2();

            if (srcX2 == tempX)  { RIGHT_PANELS.add(t); }
            else if (srcX2 == tempX2) { LEFT_PANELS.add(t); }
        }

        // Set limits at panels which are level at intersections.
        for (final DragCell pnlL : LEFT_PANELS) {
            if (pnlL.equals(src)) { continue; }
            tempY = pnlL.getAbsY();
            tempY2 = pnlL.getAbsY2();

            for (final DragCell pnlR : RIGHT_PANELS) {
                // Upper edges match? Set a bottom limit.
                if (tempY >= src.getAbsY2() && tempY == pnlR.getAbsY() && tempY < limBottom) {
                    limBottom = tempY;
                }
                // Lower edges match? Set an upper limit.
                else if (tempY2 <= src.getAbsY() && tempY2 == pnlR.getAbsY2() && tempY2 > limTop) {
                    limTop = tempY2;
                }
            }
        }

        // Remove non-contiguous panels from left side using limits.
        final Iterator<DragCell> itrLeft = LEFT_PANELS.iterator();
        while (itrLeft.hasNext()) {
            final DragCell t = itrLeft.next();

            if (t.getAbsY() >= limBottom || t.getAbsY2() <= limTop) {
                itrLeft.remove();
            }
        }

        // Remove non-contiguous panels from right side using limits.
          final Iterator<DragCell> itrRight = RIGHT_PANELS.iterator();
        while (itrRight.hasNext()) {
            final DragCell t = itrRight.next();

            if (t.getAbsY() >= limBottom || t.getAbsY2() <= limTop) {
                itrRight.remove();
            }
        }
    }

    /** @param e &emsp; {@link java.awt.event.MouseEvent} */
    public static void startResizeY(final MouseEvent e) {
        evtY = (int) e.getLocationOnScreen().getY();
        TOP_PANELS.clear();
        BOTTOM_PANELS.clear();

        final DragCell src = (DragCell) ((JPanel) e.getSource()).getParent();
        final int srcY2 = src.getAbsY2();

        int limLeft = -1;
        int limRight = Integer.MAX_VALUE;
        int tempX = -1;
        int tempX2 = -1;
        int tempY = -1;
        int tempY2 = -1;

        // Add all panels who share a top or bottom edge with the
        // same coordinate as the bottom edge of the source panel.
        for (final DragCell t : FView.SINGLETON_INSTANCE.getDragCells()) {
            tempY = t.getAbsY();
            tempY2 = t.getAbsY2();

            if (srcY2 == tempY) { BOTTOM_PANELS.add(t); }
            else if (srcY2 == tempY2) { TOP_PANELS.add(t); }
        }

        // Set limits at panels which are level at intersections.
        for (final DragCell pnlT : TOP_PANELS) {
            if (pnlT.equals(src)) { continue; }
            tempX = pnlT.getAbsX();
            tempX2 = pnlT.getAbsX2();

            for (final DragCell pnlB : BOTTOM_PANELS) {
                // Right edges match? Set a right limit.
                if (tempX >= src.getAbsX2() && tempX == pnlB.getAbsX() && tempX < limRight) {
                    limRight = tempX;
                }
                // Left edges match? Set an left limit.
                else if (tempX2 <= src.getAbsX() && tempX2 == pnlB.getAbsX2() && tempX2 > limLeft) {
                    limLeft = tempX2;
                }
            }
        }

        // Remove non-contiguous panels from left side using limits.
        final Iterator<DragCell> itrTop = TOP_PANELS.iterator();
        while (itrTop.hasNext()) {
            final DragCell t = itrTop.next();
            if (t.getAbsX() >= limRight || t.getAbsX2() <= limLeft) {
                itrTop.remove();
            }
        }

        // Remove non-contiguous panels from right side using limits.
          final Iterator<DragCell> itrBottom = BOTTOM_PANELS.iterator();
        while (itrBottom.hasNext()) {
            final DragCell t = itrBottom.next();
            if (t.getAbsX() >= limRight || t.getAbsX2() <= limLeft) {
                itrBottom.remove();
            }
        }
    }

    /** */
    public static void endResize() {
        final Thread t = new Thread() { @Override
            public void run() { SLayoutIO.saveLayout(null); } };
        t.start();
    }

    /** @return {@link java.awt.event.MouseListener} */
    public static MouseListener getResizeXListener() {
        return MAD_RESIZE_X;
    }

    /** @return {@link java.awt.event.MouseListener} */
    public static MouseListener getResizeYListener() {
        return MAD_RESIZE_Y;
    }

    /** @return {@link java.awt.event.MouseMotionListener} */
    public static MouseMotionListener getDragXListener() {
        return MMA_DRAG_X;
    }

    /** @return {@link java.awt.event.MouseMotionListener} */
    public static MouseMotionListener getDragYListener() {
        return MMA_DRAG_Y;
    }

    /** @return {@link java.awt.event.ComponentListener} */
    public static ComponentListener getWindowResizeListener() {
        return CAD_RESIZE;
    }
}
