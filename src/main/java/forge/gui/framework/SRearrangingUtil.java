package forge.gui.framework;

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

import forge.gui.toolbox.FSkin;
import forge.view.FView;

/**
 * Package-private utilities for rearranging drag behavior using
 * the draggable panels registered in FView.
 * 
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public final class SRearrangingUtil {

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

    private static JPanel pnlPreview     = FView.SINGLETON_INSTANCE.getPnlPreview();
    private static JLayeredPane pnlDocument     = FView.SINGLETON_INSTANCE.getLpnDocument();
    private static DragCell cellTarget     = null;
    private static DragCell cellSrc         = null;
    private static DragCell cellNew        = null;
    private static Dropzone dropzone         = Dropzone.NONE;
    private static List<IVDoc> docsToMove     = new ArrayList<IVDoc>();
    private static IVDoc srcSelectedDoc = null;

    private static final Toolkit TOOLS = Toolkit.getDefaultToolkit();
    private static final Cursor CUR_L = TOOLS.createCustomCursor(
            FSkin.getImage(FSkin.LayoutImages.IMG_CUR_L), new Point(16, 16), "CUR_L");
    private static final Cursor CUR_T = TOOLS.createCustomCursor(
            FSkin.getImage(FSkin.LayoutImages.IMG_CUR_T), new Point(16, 16), "CUR_T");
    private static final Cursor CUR_B = TOOLS.createCustomCursor(
            FSkin.getImage(FSkin.LayoutImages.IMG_CUR_B), new Point(16, 16), "CUR_B");
    private static final Cursor CUR_R = TOOLS.createCustomCursor(
            FSkin.getImage(FSkin.LayoutImages.IMG_CUR_R), new Point(16, 16), "CUR_R");
    private static final Cursor CUR_TAB = TOOLS.createCustomCursor(
            FSkin.getImage(FSkin.LayoutImages.IMG_CUR_TAB), new Point(16, 16), "CUR_TAB");

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
        for (final DragCell t : FView.SINGLETON_INSTANCE.getDragCells()) {
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

        if (evtX < (tempX + nestingMargin)
                && (cellTarget.getW() / 2) > SResizingUtil.W_MIN) {
            dropzone = Dropzone.LEFT;
            pnlDocument.setCursor(CUR_L);
            pnlPreview.setBounds(
                    cellTarget.getX() + SLayoutConstants.BORDER_T,
                    cellTarget.getY() + SLayoutConstants.BORDER_T,
                    ((tempW - SLayoutConstants.BORDER_T) / 2),
                    tempH - SLayoutConstants.BORDER_T
            );

        }
        else if (evtX > (tempX + tempW - nestingMargin)
                && (cellTarget.getW() / 2) > SResizingUtil.W_MIN) {
            dropzone = Dropzone.RIGHT;
            pnlDocument.setCursor(CUR_R);
            tempW = Math.round(cellTarget.getW() / 2);

            pnlPreview.setBounds(
                    cellTarget.getX() + cellTarget.getW() - tempW,
                    cellTarget.getY() + SLayoutConstants.BORDER_T,
                    tempW,
                    tempH - SLayoutConstants.BORDER_T
            );
        }
        else if (evtY < (tempY + nestingMargin + SLayoutConstants.HEAD_H) && evtY > tempY + SLayoutConstants.HEAD_H
                && (cellTarget.getH() / 2) > SResizingUtil.H_MIN) {
            dropzone = Dropzone.TOP;
            pnlDocument.setCursor(CUR_T);

            pnlPreview.setBounds(
                cellTarget.getX() + SLayoutConstants.BORDER_T,
                cellTarget.getY() + SLayoutConstants.BORDER_T,
                tempW - SLayoutConstants.BORDER_T,
                (tempH / 2)
            );
        }
        else if (evtY > (tempY + tempH - nestingMargin)
                && (cellTarget.getH() / 2) > SResizingUtil.H_MIN) {
            dropzone = Dropzone.BOTTOM;
            pnlDocument.setCursor(CUR_B);
            tempH = Math.round(cellTarget.getH() / 2);

            pnlPreview.setBounds(
                cellTarget.getX() + SLayoutConstants.BORDER_T,
                cellTarget.getY() + cellTarget.getH() - tempH,
                tempW - SLayoutConstants.BORDER_T,
                tempH
            );
        }
        else if (cellTarget.equals(cellSrc)) {
            dropzone = Dropzone.NONE;
            pnlDocument.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            pnlPreview.setBounds(0, 0, 0, 0);
        }
        else {
            dropzone = Dropzone.BODY;
            pnlDocument.setCursor(CUR_TAB);

            pnlPreview.setBounds(
                cellTarget.getX() + SLayoutConstants.BORDER_T,
                cellTarget.getY() + SLayoutConstants.BORDER_T,
                tempW - SLayoutConstants.BORDER_T,
                tempH - SLayoutConstants.BORDER_T
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
        if (cellTarget.equals(cellSrc) && cellSrc.getDocs().size() == 1) { return; }

        // Prep vals for possible resize
        tempX = cellTarget.getX();
        tempY = cellTarget.getY();
        tempW = cellTarget.getW();
        tempH = cellTarget.getH();
        cellNew = new DragCell();

        // Insert a new cell if necessary, change bounds on target as appropriate.
        switch (dropzone) {
            case LEFT:
                cellNew.setBounds(
                    tempX, tempY,
                    Math.round(tempW / 2), tempH);
                cellTarget.setBounds(
                    tempX + cellNew.getW(), tempY,
                    tempW - cellNew.getW(), tempH);
                FView.SINGLETON_INSTANCE.addDragCell(cellNew);
                break;
            case RIGHT:
                cellTarget.setBounds(
                    tempX, tempY,
                    Math.round(tempW / 2), tempH);
                cellNew.setBounds(
                    cellTarget.getX() + cellTarget.getW(), tempY ,
                    tempW - cellTarget.getW(), tempH);
                FView.SINGLETON_INSTANCE.addDragCell(cellNew);
                break;
            case TOP:
                cellNew.setBounds(
                    tempX, tempY,
                    tempW, tempH - Math.round(tempH / 2));
                cellTarget.setBounds(
                    tempX, tempY + cellNew.getH(),
                    tempW, tempH - cellNew.getH());
                FView.SINGLETON_INSTANCE.addDragCell(cellNew);
                break;
            case BOTTOM:
                cellTarget.setBounds(
                    tempX, tempY,
                    tempW, Math.round(tempH / 2));

                cellNew.setBounds(
                    tempX, cellTarget.getY() + cellTarget.getH(),
                    tempW, tempH - cellTarget.getH());
                FView.SINGLETON_INSTANCE.addDragCell(cellNew);
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

        // Remove old cell if necessary, or, enforce rough bounds on new cell.
        if (cellSrc.getDocs().size() == 0) {
            fillGap();
            FView.SINGLETON_INSTANCE.removeDragCell(cellSrc);
        }

        cellNew.setRoughBounds();
        cellTarget.setRoughBounds();

        cellSrc.setSelected(srcSelectedDoc);
        cellSrc.refresh();
        cellTarget.refresh();
        cellNew.validate();
        cellNew.refresh();
        updateBorders();

        final Thread t = new Thread() { @Override
            public void run() { SIOUtil.saveLayout(); } };
        t.start();
    }

    /** The gap created by displaced panels must be filled.
     * from any side which shares corners with the gap.  */
    private static void fillGap() {
        // Variables to help with matching the borders
        final List<DragCell> cellsToResize = new ArrayList<DragCell>();
        final int srcX = cellSrc.getAbsX();
        final int srcX2 = cellSrc.getAbsX2();
        final int srcY = cellSrc.getAbsY();
        final int srcY2 = cellSrc.getAbsY2();
        final int srcW = cellSrc.getW();
        final int srcH = cellSrc.getH();

        // Border check flags
        boolean foundT = false;
        boolean foundB = false;
        boolean foundR = false;
        boolean foundL = false;

        // Start algorithm
        cellsToResize.clear();
        foundT = false;
        foundB = false;
        // Look for matching panels to left of source, expand them to the right.
        for (final DragCell cell : FView.SINGLETON_INSTANCE.getDragCells()) {
            if (cell.getAbsX2() != srcX) { continue; }

            if (cell.getAbsY() == srcY) {
                foundT = true;
                cellsToResize.add(cell);
            }
            if (cell.getAbsY2() == srcY2) {
                foundB = true;
                if (!cellsToResize.contains(cell)) { cellsToResize.add(cell); }
            }
            if (cell.getAbsY() > srcY && cell.getAbsY2() < srcY2) { cellsToResize.add(cell); }
        }

        if (foundT && foundB) {
            for (final DragCell cell : cellsToResize) {
                cell.setBounds(cell.getX(), cell.getY(), cell.getW() + srcW, cell.getH());
                cell.setRoughBounds();
            }
            return;
        }

        cellsToResize.clear();
        foundT = false;
        foundB = false;
        // Look for matching panels to right of source, expand them to the left.
        for (final DragCell cell : FView.SINGLETON_INSTANCE.getDragCells()) {
            if (cell.getAbsX() != srcX2) { continue; }

            if (cell.getAbsY() == srcY) {
                foundT = true;
                cellsToResize.add(cell);
            }
            if (cell.getAbsY2() == srcY2) {
                foundB = true;
                if (!cellsToResize.contains(cell)) { cellsToResize.add(cell); }
            }
            if (cell.getAbsY() > srcY && cell.getAbsY2() < srcY2) { cellsToResize.add(cell); }
        }

        if (foundT && foundB) {
            for (final DragCell cell : cellsToResize) {
                cell.setBounds(cellSrc.getX(), cell.getY(), cell.getW() + srcW, cell.getH());
                cell.setRoughBounds();
            }
            return;
        }

        cellsToResize.clear();
        foundL = false;
        foundR = false;
        // Look for matching panels below source, expand them upwards.
        for (final DragCell cell : FView.SINGLETON_INSTANCE.getDragCells()) {
            if (cell.getAbsY() != srcY2) { continue; }

            if (cell.getAbsX() == srcX) {
                foundL = true;
                cellsToResize.add(cell);
            }
            if (cell.getAbsX2() == srcX2) {
                foundR = true;
                if (!cellsToResize.contains(cell)) { cellsToResize.add(cell); }
            }
            if (cell.getAbsX() > srcX && cell.getAbsX2() < srcX2) { cellsToResize.add(cell); }
        }

        if (foundL && foundR) {
            for (final DragCell cell : cellsToResize) {
                cell.setBounds(cell.getX(), cellSrc.getY(), cell.getW(), cell.getH() + srcH);

                cell.setRoughBounds();
            }
            return;
        }

        cellsToResize.clear();
        foundL = false;
        foundR = false;
        // Look for matching panels above source, expand them downwards.
        for (final DragCell cell : FView.SINGLETON_INSTANCE.getDragCells()) {
            if (cell.getAbsY2() != srcY) { continue; }

            if (cell.getAbsX() == srcX) {
                foundL = true;
                cellsToResize.add(cell);
            }
            if (cell.getAbsX2() == srcX2) {
                foundR = true;
                if (!cellsToResize.contains(cell)) { cellsToResize.add(cell); }
            }
            if (cell.getAbsX() > srcX && cell.getAbsX2() < srcX2) { cellsToResize.add(cell); }
        }

        if (foundL && foundR) {
            for (final DragCell cell : cellsToResize) {
                cell.setBounds(cell.getX(), cell.getY(), cell.getW(), cell.getH() + srcH);

                cell.setRoughBounds();
            }
            return;
        }

        throw new UnsupportedOperationException("For some reason, there's a problem filling the gap.");
    }

    /** Hides outer borders for components on edges,
     * preventing illegal resizing (and misleading cursor). */
    public static void updateBorders() {
        final List<DragCell> cells = FView.SINGLETON_INSTANCE.getDragCells();
        final JPanel pnlContent = FView.SINGLETON_INSTANCE.getPnlContent();

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
