package forge.gui.layout;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

/**
 * Top-level container in drag layout.  A cell holds
 * tabs, a drag handle, and a tab overflow selector.
 * <br>A cell also has two borders, right and bottom,
 * for resizing.
 */
@SuppressWarnings("serial")
final class DragCell extends JPanel implements ILocalRepaint {
    // Layout creation worker vars
    private double roughX = 0;
    private double roughY = 0;
    private double roughW = 0;
    private double roughH = 0;
    private int smoothX = 0;
    private int smoothY = 0;
    private int smoothW = 0;
    private int smoothH = 0;

    // Core layout stuff
    private final CardLayout cards = new CardLayout();
    private final JPanel pnlHead = new JPanel(new MigLayout("insets 0, gap 0, hidemode 3"));
    private final JPanel pnlBody = new JPanel(cards);
    private final JPanel pnlBorderRight = new JPanel();
    private final JPanel pnlBorderBottom = new JPanel();

    // Tab handling layout stuff
    private final List<IVDoc> allDocs = new ArrayList<IVDoc>();
    private final Map<DragTab, Integer> tabWidths = new HashMap<DragTab, Integer>();
    private final JLabel lblHandle = new DragHandle();
    private final JLabel lblOverflow = new JLabel();
    private IVDoc docSelected = null;

    /**
     * 
     */
    public DragCell() {
        super(new MigLayout("insets 0, gap 0, wrap 2"));

        this.add(pnlHead, "w 100% - " + FViewNew.BORDER_T + "px!, "
                + "h " + FViewNew.HEAD_H + "px!");
        this.add(pnlBorderRight, "w " + FViewNew.BORDER_T + "px!, "
                + "h 100% - " + FViewNew.BORDER_T + "px!, span 1 2");
        this.add(pnlBody, "w 100% - " + FViewNew.BORDER_T + "px!, "
                + "h 100% - " + (FViewNew.HEAD_H + FViewNew.BORDER_T) + "px!");
        this.add(pnlBorderBottom, "w 100% - " + FViewNew.BORDER_T + "px!, "
                + "h " + FViewNew.BORDER_T + "px!");

        this.setBackground(Color.black);
        pnlHead.setBackground(Color.DARK_GRAY);
        pnlBody.setBackground(Color.LIGHT_GRAY);
        pnlBorderRight.setOpaque(false);
        pnlBorderBottom.setOpaque(false);

        lblOverflow.setForeground(Color.white);
        lblOverflow.setHorizontalAlignment(SwingConstants.CENTER);
        lblOverflow.setHorizontalTextPosition(SwingConstants.CENTER);
        lblOverflow.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
        lblOverflow.setOpaque(true);
        lblOverflow.setBackground(Color.black);
        lblOverflow.setToolTipText("Other tabs");

        pnlBorderRight.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
        pnlBorderRight.addMouseListener(SResizingUtil.getResizeXListener());
        pnlBorderRight.addMouseMotionListener(SResizingUtil.getDragXListener());

        pnlBorderBottom.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
        pnlBorderBottom.addMouseListener(SResizingUtil.getResizeYListener());
        pnlBorderBottom.addMouseMotionListener(SResizingUtil.getDragYListener());

        lblOverflow.addMouseListener(SOverflowUtil.getOverflowListener());
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getHead() {
        return DragCell.this.pnlHead;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getBody() {
        return DragCell.this.pnlBody;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getBorderRight() {
        return DragCell.this.pnlBorderRight;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getBorderBottom() {
        return DragCell.this.pnlBorderBottom;
    }

    /**
     * Returns a defensive copy list of all documents in this cell.
     * @return {@link java.util.List}<{@link forge.gui.layout.IVDoc}>
     */
    public List<IVDoc> getDocs() {
        final List<IVDoc> clone = new ArrayList<IVDoc>();
        clone.addAll(allDocs);
        return allDocs;
    }

    @Override
    public void repaintThis() {
        final Dimension d = DragCell.this.getSize();
        repaint(0, 0, d.width, d.height);
    }

    /** @return int */
    public int getW() {
        return this.getWidth();
    }

    /** @return int */
    public int getH() {
        return this.getHeight();
    }

    /**
     * Screen location of left edge of cell.
     * @return int
     */
    public int getAbsX() {
        return (int) this.getLocationOnScreen().getX();
    }

    /**
     * Screen location of right edge of cell.
     * @return int
     */
    public int getAbsX2() {
        return this.getAbsX() + this.getW();
    }

    /**
     * Screen location of upper edge of cell.
     * @return int
     */
    public int getAbsY() {
        return (int) this.getLocationOnScreen().getY();
    }

    /**
     * Screen location of lower edge of cell.
     * @return int
     */
    public int getAbsY2() {
        return this.getAbsY() + this.getH();
    }

    /** Percent bounds of this cell.  Will be smoothed
     *  later to avoid pixel rounding errors.
     *  @param x0 &emsp; double
     *  @param y0 &emsp; double
     *  @param w0 &emsp; double
     *  @param h0 &emsp; double
     */
    public void setRoughBounds(final double x0, final double y0, final double w0, final double h0) {
        if (x0 > 1) { throw new IllegalArgumentException("X value greater than 100%!"); }
        if (y0 > 1) { throw new IllegalArgumentException("Y value greater than 100%!"); }
        if (w0 > 1) { throw new IllegalArgumentException("W value greater than 100%!"); }
        if (h0 > 1) { throw new IllegalArgumentException("H value greater than 100%!"); }

        this.roughX = x0;
        this.roughY = y0;
        this.roughW = w0;
        this.roughH = h0;
    }

    /** @return double */
    public double getRoughX() {
        return this.roughX;
    }

    /** @return double */
    public double getRoughY() {
        return this.roughY;
    }

    /** @return double */
    public double getRoughW() {
        return this.roughW;
    }

    /** @return double */
    public double getRoughH() {
        return this.roughH;
    }

    /** Sets bounds in superclass using smoothed values from this class. */
    public void setSmoothBounds() {
        super.setBounds(smoothX, smoothY, smoothW, smoothH);
    }

    /** @param x0 &emsp; int */
    public void setSmoothX(final int x0) {
        this.smoothX = x0;
    }

    /** @param y0 &emsp; int */
    public void setSmoothY(final int y0) {
        this.smoothY = y0;
    }

    /** @param w0 &emsp; int */
    public void setSmoothW(final int w0) {
        this.smoothW = w0;
    }

    /** @param h0 &emsp; int */
    public void setSmoothH(final int h0) {
        this.smoothH = h0;
    }

    /** Adds a document to the layout and tabs.
     * @param doc0 &emsp; {@link forge.gui.layout.IVDoc} */
    public void addDoc(final IVDoc doc0) {
        pnlBody.add(doc0.getDocumentID().toString(), doc0.getDocument());
        allDocs.add(doc0);
    }

    /** Removes a document from the layout and tabs.
     * @param doc0 &emsp; {@link forge.gui.layout.IVDoc} */
    public void removeDoc(final IVDoc doc0) {
        pnlBody.remove(doc0.getDocument());
        allDocs.remove(doc0);
    }

    /** Deselects previous selection, if exists, and then
     * selects a tab in the title bar.  <br><br><b>null</b> will reset
     * (deselect all tabs, and then select the first in the group).
     * <br><br>Unless there are no tab docs in this cell, there
     * will always be a selection.
     *
     * @param doc0 &emsp; {@link forge.gui.layout.IVDoc} tab document.
     */
    public void setSelected(final IVDoc doc0) {
        docSelected = null;

        // Priorities are used to "remember" tab selection history.
        for (final IVDoc t : allDocs) {
            if (t.equals(doc0)) {
                docSelected = doc0;
                t.getTabLabel().priorityOne();
                docSelected.getTabLabel().setSelected(true);
                cards.show(pnlBody, docSelected.getDocumentID().toString());
            }
            else {
                t.getTabLabel().setSelected(false);
                t.getTabLabel().priorityDecrease();
            }
        }

        // Reached the end without a selection? Select the first in the group.
        if (docSelected == null && allDocs.size() > 0) { setSelected(allDocs.get(0)); }
    }

    /** Returns currently selected document in this cell.
     * @return {@link forge.gui.layout.IVDoc} */
    public IVDoc getSelected() {
        return docSelected;
    }

    /** Rebuilds all tabs in head bar. Used when a tab is added or removed. */
    public void rebuild() {
        pnlHead.removeAll();

        for (final IVDoc t : allDocs) {
            pnlHead.add(t.getTabLabel(), "h 100%!, gap 2px 2px 0 0");
        }

        setSelected(getSelected());

        pnlHead.add(lblHandle, "pushx, growx, h 100%!, gap 2px 2px 0 0");
        pnlHead.add(lblOverflow, "w 20px!, h 100%!, gap 2px 2px 0 0");
        pnlHead.revalidate();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tabWidths.clear();
                refresh();
            }
        });
    }

    /** Refreshes visual display of head bar. */
    public void refresh() {
        final int wHead = pnlHead.getWidth();
        if (docSelected == null)     { return; }
        if (wHead == 0)             { return; }
        if (allDocs.isEmpty())        { return; }

        if (tabWidths.isEmpty()) {
            for (final IVDoc vDoc : allDocs) {
                tabWidths.put(vDoc.getTabLabel(), vDoc.getTabLabel().getWidth());
            }
        }

        // Order tabs by priority
        final List<DragTab> priority = new ArrayList<DragTab>();
        DragTab nextTab = docSelected.getTabLabel();

        while (nextTab != null) {
            priority.add(nextTab);
            nextTab = getNextTabInPriority(nextTab.getPriority());
        }

        int wProjected = 50;
        int docOverflowCounter = 0;

        for (final DragTab tab : priority) {
            tab.setVisible(false);
            tab.setMaximumSize(null);
            tab.setPreferredSize(null);
            tab.setMinimumSize(null);

            // Extra +4 is for gap pixels (2 on left and right).
            if (wProjected + tabWidths.get(tab) + 4 < wHead) {
                wProjected += tabWidths.get(tab) + 4;
                tab.setVisible(true);
            }
            else if (wHead - wProjected > 40) {
                final Dimension d = new Dimension(wHead - wProjected - 4, tab.getHeight());
                tab.setMaximumSize(d);
                tab.setPreferredSize(d);
                tab.setMinimumSize(d);
                tab.setVisible(true);
                wProjected = wHead + 1;
            }
            else {
                docOverflowCounter++;
            }
        }

        lblOverflow.setText("+" + docOverflowCounter);
        if (docOverflowCounter == 0) { lblOverflow.setVisible(false); }
        else { lblOverflow.setVisible(true); }
    }

    private DragTab getNextTabInPriority(final int currentPriority0) {
        DragTab neo = null;
        DragTab temp;
        int lowest = Integer.MAX_VALUE;

        for (final IVDoc d : allDocs) {
            temp = d.getTabLabel();

            // This line prevents two tabs from having the same priority.
            if (neo != null && temp.getPriority() == lowest) {
                temp.priorityDecrease();
            }

            if (temp.equals(docSelected))                 { continue; }
            if (temp.getPriority() > lowest)             { continue; }
            if (temp.getPriority() <= currentPriority0)     { continue; }

            // If he's The One, he's made it through the tests.
            lowest = temp.getPriority();
            neo = temp;
        }

        return neo;
    }

    /** Paints dragging handle image the length of the label. */
    private class DragHandle extends JLabel {
        private final Image img = new ImageIcon("handle.png").getImage();
        private final int imgW = img.getWidth(null);
        private final int imgH = img.getHeight(null);
        private boolean hovered = false;

        public DragHandle() {
            this.addMouseListener(SRearrangingUtil.getRearrangeClickEvent());
            this.addMouseMotionListener(SRearrangingUtil.getRearrangeDragEvent());

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    hovered = true; repaintThis();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    hovered = false; repaintThis();
                }
            });
        }

        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);
            if (!hovered) { return; }

            for (int x = 0; x < getWidth(); x += imgW) {
                g.drawImage(img, x, (int) ((getHeight() - imgH) / 2), null);
            }
        }
    }
}
