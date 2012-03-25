package forge.gui.layout;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Package-private utilities for rearranging drag behavior using
 * the draggable panels registered in FViewNew.
 * 
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
final class SRearrangingUtil {

    private enum Dropzone {
        BODY,
        RIGHT,
        NONE,
        TOP,
        BOTTOM,
        LEFT
    };

    private static int evtX;
    private static int evtY;

    private static int tempX;
    private static int tempY;
    private static int tempW;
    private static int tempH;

    private static JPanel pnlPreview     = FViewNew.SINGLETON_INSTANCE.getPnlPreview();
    private static JLayeredPane pnlDocument     = FViewNew.SINGLETON_INSTANCE.getLpnDocument();
    private static DragCell cellTarget     = null;
    private static DragCell cellSrc         = null;
    private static DragCell cellNew        = null;
    private static Dropzone dropzone         = Dropzone.NONE;
    private static List<IVDoc> docsToMove     = new ArrayList<IVDoc>();
    private static IVDoc srcSelectedDoc = null;

    private static final Toolkit TOOLS = Toolkit.getDefaultToolkit();
    private static final Cursor CUR_L = TOOLS.createCustomCursor(
            TOOLS.getImage("L.png"), new Point(16, 16), "CUR_L");
    private static final Cursor CUR_T = TOOLS.createCustomCursor(
            TOOLS.getImage("T.png"), new Point(16, 16), "CUR_T");
    private static final Cursor CUR_B = TOOLS.createCustomCursor(
            TOOLS.getImage("B.png"), new Point(16, 16), "CUR_B");
    private static final Cursor CUR_R = TOOLS.createCustomCursor(
            TOOLS.getImage("R.png"), new Point(16, 16), "CUR_R");
    private static final Cursor CUR_TAB = TOOLS.createCustomCursor(
            TOOLS.getImage("TAB.png"), new Point(16, 16), "CUR_TAB");

    private static final MouseListener MAD_REARRANGE = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            SRearrangingUtil.startRearrange(e);
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            SRearrangingUtil.endRearrange();
        }
    };

    private static final MouseMotionListener MMA_REARRANGE = new MouseMotionAdapter() {
        @Override
        public void mouseDragged(final MouseEvent e) {
            SRearrangingUtil.rearrange(e);
        }
    };

    /**
     * Initiates a rearranging of cells or tabs.<br>
     * - Sets up source cell<br>
     * - Determines if it's a tab or a cell being dragged<br>
     * - Resets preview panel
     */
    private static void startRearrange(final MouseEvent e) {
        cellSrc = (DragCell) ((Container) e.getSource()).getParent().getParent();
        docsToMove.clear();
        dropzone = Dropzone.NONE;

        // Save selected tab in case this tab will be dragged.
        srcSelectedDoc = cellSrc.getSelected();

        // If only a single tab, select it, and add it to docsToMove.
        if (e.getSource() instanceof DragTab) {
            for (final IVDoc vDoc : cellSrc.getDocs()) {
                if (vDoc.getTabLabel() == (DragTab) (e.getSource())) {
                    cellSrc.setSelected(vDoc);
                    docsToMove.add(vDoc);
                }
            }
        }
        // Otherwise, add all of the documents.
        else {
            for (final IVDoc vDoc : cellSrc.getDocs()) {
                docsToMove.add(vDoc);
            }
        }

        // Reset and show preview panel
        pnlDocument.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        pnlPreview.setVisible(true);
        pnlPreview.setBounds(0, 0, 0, 0);
    }

    /**
     * Tracks the mouse during a rearrange drag.  Updates preview
     * panel as necessary, to show the user where their doc
     * or cell will be dropped. dropzone is set to direct resizing
     * operations when rearrange is finished [see endRearrange()].
     * 
     *  @param e &emsp; {@link java.awt.event.MouseEvent}
     */
    private static void rearrange(final MouseEvent e) {
        // nestingMargin controls the thickness of the "zones" bordering
        // the center body.
        final int nestingMargin = 30;
        evtX = (int) e.getLocationOnScreen().getX();
        evtY = (int) e.getLocationOnScreen().getY();

        // Find out over which panel the event occurred.
        for (final DragCell t : FViewNew.SINGLETON_INSTANCE.getDragCells()) {
            tempX = t.getAbsX();
            tempY = t.getAbsY();
            tempW = t.getW();
            tempH = t.getH();

            if (evtX < tempX) { continue; }            // Left
            if (evtY < tempY) { continue; }             // Top
            if (evtX > tempX + tempW) { continue; }    // Right
            if (evtY > tempY + tempH) { continue; }    // Bottom

            cellTarget = t;
            break;
        }

        if (cellTarget.equals(cellSrc)) {
            dropzone = Dropzone.NONE;
            pnlDocument.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            pnlPreview.setBounds(0, 0, 0, 0);
        }
        else if (evtX < (tempX + nestingMargin)
                && (cellTarget.getW() / 2) > SResizingUtil.W_MIN) {
            dropzone = Dropzone.LEFT;
            pnlDocument.setCursor(CUR_L);
            pnlPreview.setBounds(
                    cellTarget.getX() + FViewNew.BORDER_T,
                    cellTarget.getY() + FViewNew.BORDER_T,
                    (int) ((tempW - FViewNew.BORDER_T) / 2),
                    tempH - FViewNew.BORDER_T
            );

        }
        else if (evtX > (tempX + tempW - nestingMargin)
                && (cellTarget.getW() / 2) > SResizingUtil.W_MIN) {
            dropzone = Dropzone.RIGHT;
            pnlDocument.setCursor(CUR_R);
            tempW = (int) Math.round(cellTarget.getW() / 2);

            pnlPreview.setBounds(
                    cellTarget.getX() + cellTarget.getW() - tempW,
                    cellTarget.getY() + FViewNew.BORDER_T,
                    tempW,
                    tempH - FViewNew.BORDER_T
            );
        }
        else if (evtY < (tempY + nestingMargin + FViewNew.HEAD_H)
                && (cellTarget.getH() / 2) > SResizingUtil.H_MIN) {
            dropzone = Dropzone.TOP;
            pnlDocument.setCursor(CUR_T);

            pnlPreview.setBounds(
                cellTarget.getX() + FViewNew.BORDER_T,
                cellTarget.getY() + FViewNew.BORDER_T,
                tempW - FViewNew.BORDER_T,
                (int) (tempH / 2)
            );
        }
        else if (evtY > (tempY + tempH - nestingMargin)
                && (cellTarget.getH() / 2) > SResizingUtil.H_MIN) {
            dropzone = Dropzone.BOTTOM;
            pnlDocument.setCursor(CUR_B);
            tempH = (int) Math.round(cellTarget.getH() / 2);

            pnlPreview.setBounds(
                cellTarget.getX() + FViewNew.BORDER_T,
                cellTarget.getY() + cellTarget.getH() - tempH,
                tempW - FViewNew.BORDER_T,
                tempH
            );
        }
        else {
            dropzone = Dropzone.BODY;
            pnlDocument.setCursor(CUR_TAB);

            pnlPreview.setBounds(
                cellTarget.getX() + FViewNew.BORDER_T,
                cellTarget.getY() + FViewNew.BORDER_T,
                tempW - FViewNew.BORDER_T,
                tempH - FViewNew.BORDER_T
            );
        }
    }

    /**
     * Finalizes a drop, using dropzone hint and docsToMove to
     * transfer docs and remove + resize cells as necessary.
     */
    private static void endRearrange() {
        // Resize preview panel in preparation for next event.
        pnlDocument.setCursor(Cursor.getDefaultCursor());
        pnlPreview.setVisible(false);
        pnlPreview.setBounds(0, 0, 0, 0);

        // Source and target are the same?
        if (dropzone.equals(Dropzone.NONE)) { return; }

        // Prep vals for possible resize
        tempX = cellTarget.getX();
        tempY = cellTarget.getY();
        tempW = cellTarget.getW();
        tempH = cellTarget.getH();

        // Insert a new cell if necessary, change bounds on target as appropriate.
        cellNew = new DragCell();
        switch (dropzone) {
            case LEFT:
                cellNew.setBounds(
                    tempX, tempY,
                    (int) Math.round(tempW / 2), tempH);
                cellTarget.setBounds(
                    tempX + cellNew.getW(), tempY,
                    tempW - cellNew.getW(), tempH);
                FViewNew.SINGLETON_INSTANCE.addDragCell(cellNew);
                break;
            case RIGHT:
                cellTarget.setBounds(
                    tempX, tempY,
                    (int) Math.round(tempW / 2), tempH);
                cellNew.setBounds(
                    cellTarget.getX() + cellTarget.getW(), tempY ,
                    tempW - cellTarget.getW(), tempH);
                FViewNew.SINGLETON_INSTANCE.addDragCell(cellNew);
                break;
            case TOP:
                cellNew.setBounds(
                    tempX, tempY,
                    tempW, tempH - (int) Math.round(tempH / 2));
                cellTarget.setBounds(
                    tempX, tempY + cellNew.getH(),
                    tempW, tempH - cellNew.getH());
                FViewNew.SINGLETON_INSTANCE.addDragCell(cellNew);
                break;
            case BOTTOM:
                cellTarget.setBounds(
                    tempX, tempY,
                    tempW, (int) Math.round(tempH / 2));

                cellNew.setBounds(
                    tempX, cellTarget.getY() + cellTarget.getH(),
                    tempW, tempH - cellTarget.getH());
                FViewNew.SINGLETON_INSTANCE.addDragCell(cellNew);
                break;
            case BODY:
                cellNew = cellTarget;
                break;
            default:
        }

        for (final IVDoc vDoc : docsToMove) {
            cellSrc.removeDoc(vDoc);
            cellNew.addDoc(vDoc);
            cellNew.setSelected(vDoc);
        }

        cellSrc.setSelected(srcSelectedDoc);
        cellNew.rebuild();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //cellSrc.refresh();
                if (cellSrc.getDocs().size() == 0) {
                    fillGap();
                    FViewNew.SINGLETON_INSTANCE.removeDragCell(cellSrc);
                    updateBorders();
                }
            }
        });
    }

    /** The gap created by displaced panels must be filled.
     * from any side which shares corners with the gap.  */
    private static void fillGap() {
        // TODO should be in SResizingUtil?
        boolean foundT = false;
        boolean foundB = false;
        boolean foundR = false;
        boolean foundL = false;
        JPanel targetBorder = null;
        final int srcX = cellSrc.getAbsX();
        final int srcX2 = cellSrc.getAbsX2();
        final int srcY = cellSrc.getAbsY();
        final int srcY2 = cellSrc.getAbsY2();

        // Look for matching panels on left side.
        for (final DragCell pnl : FViewNew.SINGLETON_INSTANCE.getDragCells()) {
            if (pnl.getAbsX2() == srcX && pnl.getAbsY() == srcY) {
                foundT = true;
                targetBorder = pnl.getBorderRight();
            }
            if (pnl.getAbsX2() == srcX && pnl.getAbsY2() == srcY2) {
                foundB = true;
            }
        }

        if (foundT && foundB) {
            SResizingUtil.setLock(false);
            SResizingUtil.startResizeX(new MouseEvent(targetBorder, 501, 0L, 0,
                (int) targetBorder.getLocationOnScreen().getX(), (int) targetBorder.getLocationOnScreen().getY(), 1, false));
            SResizingUtil.resizeX(new MouseEvent(targetBorder, 506, 0L, 0,
                srcX2 - FViewNew.BORDER_T, targetBorder.getY(), 1, false));
            SResizingUtil.setLock(true);
            return;
        }

        // Look for matching panels on right side.
        for (final DragCell pnl : FViewNew.SINGLETON_INSTANCE.getDragCells()) {
            if (pnl.getAbsX() == srcX2 && pnl.getAbsY() == srcY) {
                foundT = true;
                targetBorder = cellSrc.getBorderRight();
            }
            if (pnl.getAbsX() == srcX2 && pnl.getAbsY2() == srcY2) {
                foundB = true;
            }
        }

        if (foundT && foundB) {
            SResizingUtil.setLock(false);
            SResizingUtil.startResizeX(new MouseEvent(targetBorder, 501, 0L, 0,
                    (int) targetBorder.getLocationOnScreen().getX(), (int) targetBorder.getLocationOnScreen().getY(), 1, false));
            SResizingUtil.resizeX(new MouseEvent(targetBorder, 506, 0L, 0,
                    srcX - FViewNew.BORDER_T, targetBorder.getY(), 1, false));
            SResizingUtil.setLock(true);
            return;
        }

        // Look for matching panels on bottom side.
        for (final DragCell pnl : FViewNew.SINGLETON_INSTANCE.getDragCells()) {
            if (pnl.getAbsY() == srcY2 && pnl.getAbsX() == srcX) {
                foundL = true;
                targetBorder = cellSrc.getBorderBottom();
            }
            if (pnl.getAbsY() == srcY2 && pnl.getAbsX2() == srcX2) {
                foundR = true;
            }
        }

        if (foundL && foundR) {
            SResizingUtil.startResizeY(new MouseEvent(targetBorder, 501, 0L, 0,
                    (int) targetBorder.getLocationOnScreen().getX(), (int) targetBorder.getLocationOnScreen().getY(), 1, false));
            SResizingUtil.resizeY(new MouseEvent(targetBorder, 506, 0L, 0,
                    targetBorder.getX(), srcY - FViewNew.BORDER_T, 1, false));
            return;
        }

        // Look for matching panels on top side.
        for (final DragCell pnl : FViewNew.SINGLETON_INSTANCE.getDragCells()) {
            if (pnl.getAbsY2() == srcY && pnl.getAbsX() == srcX) {
                foundL = true;
                targetBorder = pnl.getBorderBottom();
            }
            if (pnl.getAbsY2() == srcY && pnl.getAbsX2() == srcX2) {
                foundR = true;
            }
        }

        if (foundL && foundR) {
            SResizingUtil.startResizeY(new MouseEvent(targetBorder, 501, 0L, 0,
                    (int) targetBorder.getLocationOnScreen().getX(), (int) targetBorder.getLocationOnScreen().getY(), 1, false));
            SResizingUtil.resizeY(new MouseEvent(targetBorder, 506, 0L, 0,
                    targetBorder.getX(), srcY2 - FViewNew.BORDER_T, 1, false));
            return;
        }

        throw new UnsupportedOperationException("For some reason, there's a problem filling the gap.");
    }

    /** Hides outer borders for components on edges,
     * preventing illegal resizing (and misleading cursor). */
    public static void updateBorders() {
        final List<DragCell> cells = FViewNew.SINGLETON_INSTANCE.getDragCells();
        final JPanel pnlContent = FViewNew.SINGLETON_INSTANCE.getPnlContent();

        for (final DragCell t : cells) {
            if (t.getAbsX2() == (pnlContent.getLocationOnScreen().getX() + pnlContent.getWidth())) {
                t.getBorderRight().setVisible(false);
            }
            else {
                t.getBorderRight().setVisible(true);
            }

            if (t.getAbsY2() == (pnlContent.getLocationOnScreen().getY() + pnlContent.getHeight())) {
                t.getBorderBottom().setVisible(false);
            }
            else {
                t.getBorderBottom().setVisible(true);
            }
        }

        cells.clear();
    }

    /** @return {@link java.awt.event.MouseListener} */
    public static MouseListener getRearrangeClickEvent() {
        return MAD_REARRANGE;
    }

    /** @return {@link java.awt.event.MouseMotionListener} */
    public static MouseMotionListener getRearrangeDragEvent() {
        return MMA_REARRANGE;
    }
}
