package forge.gui.framework;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;

import javax.swing.JPanel;

import forge.gui.MouseUtil;
import forge.toolbox.FAbsolutePositioner;
import forge.toolbox.FOverlay;
import forge.view.FDialog;
import forge.view.FFrame;
import forge.view.FNavigationBar;
import forge.view.FView;

/**
 * Package-private utilities for resizing drag behavior using
 * the draggable panels registered in FView.
 * 
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public final class SResizingUtil {
    private static final List<DragCell> LEFT_PANELS = new ArrayList<>();
    private static final List<DragCell> RIGHT_PANELS = new ArrayList<>();
    private static final List<DragCell> TOP_PANELS = new ArrayList<>();
    private static final List<DragCell> BOTTOM_PANELS = new ArrayList<>();

    private static int dX;
    private static int evtX;
    private static int dY;
    private static int evtY;

    /** True while a resize drag is in progress (for Esc/RMB cancel + cursor). */
    private static boolean resizing;

    /** Snap grid (px) used when Ctrl is held during a resize drag (quarter of the dragged cell). */
    private static int snapGrid;

    /** Absolute position of the dragged edge, for Ctrl-snap math. */
    private static int curEdgeX;
    private static int curEdgeY;

    /** True while resizing with the mouse over a shared edge (Shift = move all aligned edges). */
    private static boolean linkedEdges;

    /** Snapshot of cell bounds taken at drag start, for Esc/RMB cancel restore. */
    private static final Map<DragCell, Rectangle> preResizeBounds = new HashMap<>();

    /** Minimum cell width. */
    public static final int W_MIN = 100;
    /** Minimum cell height. */
    public static final int H_MIN = 50;

    private static final MouseListener MAD_RESIZE_X = new MouseAdapter() {
        @Override
        public void mouseEntered(final MouseEvent e) {
            if (!resizing) { MouseUtil.setCursor(Cursor.E_RESIZE_CURSOR); }
        }

        @Override
        public void mouseExited(final MouseEvent e) {
            if (!resizing) { MouseUtil.resetCursor(); }
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) { SResizingUtil.cancelResize(); return; }
            SResizingUtil.startResizeX(e);
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            SResizingUtil.endResize();
        }
    };

    private static final MouseListener MAD_RESIZE_Y = new MouseAdapter() {
        @Override
        public void mouseEntered(final MouseEvent e) {
            if (!resizing) { MouseUtil.setCursor(Cursor.N_RESIZE_CURSOR); }
        }

        @Override
        public void mouseExited(final MouseEvent e) {
            if (!resizing) { MouseUtil.resetCursor(); }
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) { SResizingUtil.cancelResize(); return; }
            SResizingUtil.startResizeY(e);
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            SResizingUtil.endResize();
        }
    };

    /** Esc aborts a resize drag and restores the pre-drag bounds. */
    private static final KeyEventDispatcher ESC_CANCEL = e -> { // doc:12b DONE
        if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE && resizing) {
            cancelResize();
            return true;
        }
        return false;
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

    public static void resizeWindow() {
        final List<DragCell> cells = FView.SINGLETON_INSTANCE.getDragCells();
        final FFrame frame = FView.SINGLETON_INSTANCE.getFrame();
        final FNavigationBar navigationBar = FView.SINGLETON_INSTANCE.getNavigationBar();
        final JPanel pnlContent = FView.SINGLETON_INSTANCE.getPnlContent();
        final JPanel pnlInsets = FView.SINGLETON_INSTANCE.getPnlInsets();

        Rectangle mainBounds = frame.getContentPane().getBounds();

        FDialog.getBackdropPanel().setBounds(mainBounds);

        int navigationBarHeight = navigationBar.getPreferredSize().height;
        navigationBar.setSize(mainBounds.width, navigationBarHeight);
        navigationBar.validate();

        if (!frame.isTitleBarHidden()) { //adjust bounds for titlebar if not hidden
            mainBounds.y += navigationBarHeight;
            mainBounds.height -= navigationBarHeight;
        }

        FAbsolutePositioner.SINGLETON_INSTANCE.containerResized(mainBounds);
        FOverlay.SINGLETON_INSTANCE.getPanel().setBounds(mainBounds);

        pnlInsets.setBounds(mainBounds);
        pnlInsets.validate();

        final int w = pnlContent.getWidth();
        final int h = pnlContent.getHeight();

        double roughVal = 0;
        int smoothVal = 0;

        Set<Component> existingComponents = new HashSet<>(Arrays.asList(pnlContent.getComponents()));

        // This is the core of the pixel-perfect layout. To avoid ±1 px errors on borders
        // from rounding individual panels, the intermediate values (exactly accurate, in %)
        // for width and height are rounded based on comparison to other panels in the
        // layout.  This is to avoid errors such as:
        // 10% = 9.8px -> 10px -> x 3 = 30px
        // 30% = 29.4px -> 29px (!)
        for (final DragCell cellA : cells) {
            RectangleOfDouble cellSizeA = cellA.getRoughBounds();
            roughVal = cellSizeA.getX() * w + cellSizeA.getW() * w;

            smoothVal = (int) Math.round(roughVal);
            for (final DragCell cellB : cells) {
                RectangleOfDouble cellSizeB = cellB.getRoughBounds();
                if ((cellSizeB.getX() * w + cellSizeB.getW() * w) == roughVal) {
                    cellB.setSmoothW(smoothVal - (int) Math.round(cellSizeB.getX() * w));
                }
            }
            cellA.setSmoothW(smoothVal - (int) Math.round(cellSizeA.getX() * w));

            roughVal = cellSizeA.getY() * h + cellSizeA.getH() * h;
            smoothVal = (int) Math.round(roughVal);
            for (final DragCell cellB : cells) {
                RectangleOfDouble cellSizeB = cellB.getRoughBounds();
                if (cellSizeB.getY() * h + cellSizeB.getH() * h == roughVal) {
                    cellB.setSmoothH(smoothVal - (int) Math.round(cellSizeB.getY() * h));
                }
            }
            cellA.setSmoothH(smoothVal - (int) Math.round(cellSizeA.getY() * h));

            // X and Y coordinate can be rounded as usual.
            cellA.setSmoothX((int) Math.round(cellSizeA.getX() * w));
            cellA.setSmoothY((int) Math.round(cellSizeA.getY() * h));

            // only add component if not already in container; otherwise the keyboard focus
            // jumps around to the most recenly added component 
            if (!existingComponents.contains(cellA)) {
                pnlContent.add(cellA);
            }
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

        int candidateEdgeX;
        if (e.isControlDown()) { // snap the dragged edge to a quarter-cell grid
            candidateEdgeX = snapGrid * Math.round((curEdgeX + dX) / (float) snapGrid);
            dX = candidateEdgeX - curEdgeX;
        } else {
            candidateEdgeX = curEdgeX + dX;
        }

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

        curEdgeX = candidateEdgeX;

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

        int candidateEdgeY;
        if (e.isControlDown()) { // snap the dragged edge to a quarter-cell grid
            candidateEdgeY = snapGrid * Math.round((curEdgeY + dY) / (float) snapGrid);
            dY = candidateEdgeY - curEdgeY;
        } else {
            candidateEdgeY = curEdgeY + dY;
        }

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

        curEdgeY = candidateEdgeY;

        for (final DragCell t : TOP_PANELS) {
            t.setBounds(t.getX(), t.getY(), t.getW(), t.getH() + dY);
            t.revalidate();
            t.repaintSelf();
        }

        for (final DragCell t : BOTTOM_PANELS) {
            t.setBounds(t.getX(), t.getY() + dY, t.getW(), t.getH() - dY);
            t.revalidate();
            t.repaintSelf();
        }
    }

    /** @param e &emsp; {@link java.awt.event.MouseEvent} */
    public static void startResizeX(final MouseEvent e) {
        evtX = (int) e.getLocationOnScreen().getX();
        resizing = true;
        linkedEdges = e.isShiftDown();
        preResizeBounds.clear();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ESC_CANCEL);

        LEFT_PANELS.clear();
        RIGHT_PANELS.clear();

        final DragCell src = (DragCell) ((JPanel) e.getSource()).getParent();
        final int srcX2 = src.getAbsX2();
        snapGrid = Math.max(1, src.getW() / 4); // quarter-cell snap increments
        curEdgeX = srcX2;

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
        // (Shift = linked edges: keep all panels sharing the edge line.)
        final Iterator<DragCell> itrLeft = LEFT_PANELS.iterator();
        while (itrLeft.hasNext()) {
            final DragCell t = itrLeft.next();

            if (!linkedEdges && (t.getAbsY() >= limBottom || t.getAbsY2() <= limTop)) {
                itrLeft.remove();
            }
        }

        // Remove non-contiguous panels from right side using limits.
          final Iterator<DragCell> itrRight = RIGHT_PANELS.iterator();
        while (itrRight.hasNext()) {
            final DragCell t = itrRight.next();

            if (!linkedEdges && (t.getAbsY() >= limBottom || t.getAbsY2() <= limTop)) {
                itrRight.remove();
            }
        }

        snapshotBounds();
    }

    /** @param e &emsp; {@link java.awt.event.MouseEvent} */
    public static void startResizeY(final MouseEvent e) {
        evtY = (int) e.getLocationOnScreen().getY();
        resizing = true;
        linkedEdges = e.isShiftDown();
        preResizeBounds.clear();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ESC_CANCEL);

        TOP_PANELS.clear();
        BOTTOM_PANELS.clear();

        final DragCell src = (DragCell) ((JPanel) e.getSource()).getParent();
        final int srcY2 = src.getAbsY2();
        snapGrid = Math.max(1, src.getH() / 4); // quarter-cell snap increments
        curEdgeY = srcY2;

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
        // (Shift = linked edges: keep all panels sharing the edge line.)
        final Iterator<DragCell> itrTop = TOP_PANELS.iterator();
        while (itrTop.hasNext()) {
            final DragCell t = itrTop.next();
            if (!linkedEdges && (t.getAbsX() >= limRight || t.getAbsX2() <= limLeft)) {
                itrTop.remove();
            }
        }

        // Remove non-contiguous panels from right side using limits.
          final Iterator<DragCell> itBottom = BOTTOM_PANELS.iterator();
        while (itBottom.hasNext()) {
            final DragCell t = itBottom.next();
            if (!linkedEdges && (t.getAbsX() >= limRight || t.getAbsX2() <= limLeft)) {
                itBottom.remove();
            }
        }

        snapshotBounds();
    }

    private static void snapshotBounds() {
        for (final DragCell t : LEFT_PANELS) { preResizeBounds.put(t, t.getBounds()); }
        for (final DragCell t : RIGHT_PANELS) { preResizeBounds.put(t, t.getBounds()); }
        for (final DragCell t : TOP_PANELS) { preResizeBounds.put(t, t.getBounds()); }
        for (final DragCell t : BOTTOM_PANELS) { preResizeBounds.put(t, t.getBounds()); }
    }

    /** */
    public static void endResize() {
        if (!resizing) { return; }
        resetting();
        SLayoutIO.saveLayout(null);
    }

    /** Aborts a resize drag, restoring the pre-drag bounds (no layout save). */
    public static void cancelResize() {
        if (!resizing) { return; }
        for (final Map.Entry<DragCell, Rectangle> entry : preResizeBounds.entrySet()) {
            final DragCell cell = entry.getKey();
            cell.setBounds(entry.getValue().x, entry.getValue().y, entry.getValue().width, entry.getValue().height);
            cell.refresh();
        }
        preResizeBounds.clear();
        resetting();
    }

    private static void resetting() {
        resizing = false;
        linkedEdges = false;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(ESC_CANCEL);
        MouseUtil.resetCursor();
    }

    private static DragCell maximizedCell;
    private static final Map<DragCell, RectangleOfDouble> savedMaximizeBounds = new HashMap<>();

    /** Toggles the cell under the mouse (else the focused cell) to full battlefield and back. */
    public static void toggleMaximize() {
        final List<DragCell> cells = FView.SINGLETON_INSTANCE.getDragCells();
        if (maximizedCell != null) {
            for (final DragCell cell : cells) {
                final RectangleOfDouble bounds = savedMaximizeBounds.get(cell);
                if (bounds != null) { cell.setRoughBounds(bounds); }
            }
            maximizedCell = null;
            savedMaximizeBounds.clear();
        } else {
            final DragCell target = findTargetCell();
            if (target == null) { return; }
            maximizedCell = target;
            savedMaximizeBounds.clear();
            for (final DragCell cell : cells) {
                savedMaximizeBounds.put(cell, cell.getRoughBounds());
                if (cell.equals(target)) { cell.setRoughBounds(new RectangleOfDouble(0, 0, 1, 1)); }
                else { cell.setRoughBounds(new RectangleOfDouble(0, 0, 0, 0)); } // Hide by setting zero-size bounds; explicit visibility management handles non-maximized cells
            }
        }
        resizeWindow();
    }

    /** @return the cell under the mouse cursor, else the focused cell's owner, else null. */
    private static DragCell findTargetCell() {
        final java.awt.Point p = MouseInfo.getPointerInfo().getLocation();
        for (final DragCell cell : FView.SINGLETON_INSTANCE.getDragCells()) {
            if (p.x >= cell.getAbsX() && p.x <= cell.getAbsX2() && p.y >= cell.getAbsY() && p.y <= cell.getAbsY2()) {
                return cell;
            }
        }
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        while (c != null) {
            if (c instanceof DragCell) { return (DragCell) c; }
            c = c.getParent();
        }
        return null;
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
