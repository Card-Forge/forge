package forge.gui.framework;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.google.common.collect.Lists;

import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.FPanel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import forge.view.FView;
import net.miginfocom.swing.MigLayout;

/**
 * Top-level container in drag layout.  A cell holds
 * tabs, a drag handle, and a tab overflow selector.
 * <br>A cell also has two borders, right and bottom,
 * for resizing.
 */
@SuppressWarnings("serial")
public final class DragCell extends JPanel implements ILocalRepaint {
    // Layout creation worker vars
    private RectangleOfDouble roughSize;
    private int smoothX = 0;
    private int smoothY = 0;
    private int smoothW = 0;
    private int smoothH = 0;

    // Core layout stuff
    private final JPanel pnlHead = new JPanel(new MigLayout("insets 0, gap 0, hidemode 3"));
    private final FPanel pnlBody = new FPanel();
    private final JPanel pnlBorderRight = new JPanel();
    private final JPanel pnlBorderBottom = new JPanel();
    private final int tabPaddingPx = 2;
    private final int margin = 2 * tabPaddingPx;

    // Tab handling layout stuff
    private final List<IVDoc<? extends ICDoc>> allDocs = new ArrayList<>();
    private final JLabel lblHandle = new DragHandle();
    private final JLabel lblOverflow = new JLabel();
    private IVDoc<? extends ICDoc> docSelected = null;

    public DragCell() {
        super(new MigLayout("insets 0, gap 0, wrap 2"));

        this.setOpaque(false);
        pnlHead.setOpaque(false);

        pnlHead.setBackground(Color.DARK_GRAY);

        lblOverflow.setForeground(Color.white);
        lblOverflow.setHorizontalAlignment(SwingConstants.CENTER);
        lblOverflow.setHorizontalTextPosition(SwingConstants.CENTER);
        lblOverflow.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
        lblOverflow.setOpaque(true);
        lblOverflow.setBackground(Color.black);
        lblOverflow.setToolTipText("Other tabs");

        pnlBorderRight.setOpaque(false);
        pnlBorderRight.addMouseListener(SResizingUtil.getResizeXListener());
        pnlBorderRight.addMouseMotionListener(SResizingUtil.getDragXListener());

        pnlBorderBottom.setOpaque(false);
        pnlBorderBottom.addMouseListener(SResizingUtil.getResizeYListener());
        pnlBorderBottom.addMouseMotionListener(SResizingUtil.getDragYListener());

        lblOverflow.addMouseListener(SOverflowUtil.getOverflowListener());

        pnlHead.add(lblHandle, "pushx, growx, h 100%!, gap " + tabPaddingPx + "px " + tabPaddingPx + "px 0 0", -1);
        pnlHead.add(lblOverflow, "w 20px!, h 100%!, gap " + tabPaddingPx + "px " + tabPaddingPx + "px 0 0", -1);

        pnlBody.setCornerDiameter(0);
    }

    /**
     * Refreshes the cell layout without affecting contents.
     * <p>
     * Primarily used to toggle visibility of tabs.
     */
    public void doCellLayout(final boolean showTabs) {
        this.removeAll();
        final int borderT = SLayoutConstants.BORDER_T;
        final int headH = ((showTabs || allDocs.size() > 1) ? SLayoutConstants.HEAD_H : 0);
        this.add(pnlHead,
                "w 100% - " + borderT + "px!" + ", " + "h " + headH + "px!");
        this.add(pnlBorderRight,
                "w " + borderT + "px!" + ", " + "h 100% - " + borderT + "px!, span 1 2");
        this.add(pnlBody,
                "w 100% - " + borderT + "px!" + ", " + "h 100% - " + (headH + borderT) + "px!");
        this.add(pnlBorderBottom,
                "w 100% - " + borderT + "px!" + ", " + "h " + borderT + "px!");
        if (this.isShowing()) {
            this.validate();
        }
    }

    /**
     * Determines visibility of tabs on game screen.
     */
    private static boolean showGameTabs() {
        final ForgePreferences prefs = FModel.getPreferences();
        return !prefs.getPrefBoolean(FPref.UI_HIDE_GAME_TABS);
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
     * @return {@link java.util.List}<{@link forge.gui.framework.IVDoc}>
     */
    public List<IVDoc<? extends ICDoc>> getDocs() {
        return Lists.newArrayList(allDocs);
    }

    @Override
    public void repaintSelf() {
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
        int i = 0;

        try { i = (int) this.getLocationOnScreen().getX(); }
        catch (final Exception e) { }

        return i;
    }

    /**
     * Screen location of right edge of cell.
     * @return int
     */
    public int getAbsX2() {
        int i = 0;

        try { i = this.getAbsX() + this.getW(); }
        catch (final Exception e) { }

        return i;
    }

    /**
     * Screen location of upper edge of cell.
     * @return int
     */
    public int getAbsY() {
        int i = 0;

        try { i = (int) this.getLocationOnScreen().getY(); }
        catch (final Exception e) { }

        return i;
    }

    /**
     * Screen location of lower edge of cell.
     * @return int
     */
    public int getAbsY2() {
        int i = 0;

        try { i = this.getAbsY() + this.getH(); }
        catch (final Exception e) { }

        return i;
    }

    /**
     * Automatically calculates rough bounds of this cell.
     */
    public void updateRoughBounds() {
        final double contentW = FView.SINGLETON_INSTANCE.getPnlContent().getWidth();
        final double contentH = FView.SINGLETON_INSTANCE.getPnlContent().getHeight();

        this.roughSize = new RectangleOfDouble(this.getX() / contentW, this.getY()  / contentH,
                this.getW() / contentW, this.getH() / contentH);
    }

    /** Explicitly sets percent bounds of this cell.  Will be smoothed
     *  later to avoid pixel rounding errors.
     */
    public void setRoughBounds(final RectangleOfDouble rectangleOfDouble) {
        this.roughSize = rectangleOfDouble;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public RectangleOfDouble getRoughBounds() {
        return roughSize;
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

    /** Adds a document to the tabs.
     * @param doc0 &emsp; {@link forge.gui.framework.IVDoc} */
    public void addDoc(final IVDoc<? extends ICDoc> doc0) {
        if (doc0 instanceof VEmptyDoc) { return; }
        allDocs.add(doc0);
        doc0.setParentCell(this);
        pnlHead.add(doc0.getTabLabel(), "h 100%!, gap " + tabPaddingPx + "px " + tabPaddingPx + "px 0 0", allDocs.size() - 1);

        // Ensure that a tab is selected
        setSelected(getSelected());

        doCellLayout(showGameTabs());
    }

    /** Removes a document from the layout and tabs.
     * @param doc0 &emsp; {@link forge.gui.framework.IVDoc} */
    public void removeDoc(final IVDoc<? extends ICDoc> doc0) {
        final boolean wasSelected = (docSelected == doc0);
        allDocs.remove(doc0);
        pnlHead.remove(doc0.getTabLabel());
        if (wasSelected) { //after removing selected doc, select most recent doc if possible
            setSelected(null);
        }

        doCellLayout(showGameTabs());
    }

    /** - Deselects previous selection, if there is one<br>
     *  - Decrements the priorities of all other tabs<br>
     *  - Sets selected as priority 1<br>
     *
     * <br><b>null</b> will reset
     * (deselect all tabs, and then select the first in the group).
     *
     * <br><br>Unless there are no tab docs in this cell, there
     * will always be a selection.
     *
     * @param doc0 &emsp; {@link forge.gui.framework.IVDoc} tab document.
     */
    public void setSelected(final IVDoc<? extends ICDoc> doc0) {
        if (null != doc0 && docSelected == doc0) {
            // already selected
            return;
        }

        docSelected = null;
        pnlBody.removeAll();

        // Priorities are used to "remember" tab selection history.
        for (final IVDoc<? extends ICDoc> doc : allDocs) {
            if (doc.equals(doc0)) {
                docSelected = doc0;
                doc.getTabLabel().priorityOne();
                doc.getTabLabel().setSelected(true);
                doc.populate();
                doc.getLayoutControl().update();
            }
            else {
                doc.getTabLabel().setSelected(false);
                doc.getTabLabel().priorityDecrease();
            }
        }

        pnlBody.revalidate();
        pnlBody.repaint();

        // Reached the end without a selection? Select the first in the group.
        if (docSelected == null && allDocs.size() > 0) { setSelected(allDocs.get(0)); }
    }

    /** Returns currently selected document in this cell.
     * @return {@link forge.gui.framework.IVDoc} */
    public IVDoc<? extends ICDoc> getSelected() {
        return docSelected;
    }

    /**
     * Refreshes visual display of head bar.
     */
    public void refresh() {
        final int headW = pnlHead.getWidth();
        if (docSelected == null)    { return; }
        if (headW <= 0)             { return; }
        if (allDocs.isEmpty())      { return; }

        // Order tabs by priority
        final List<DragTab> priority = new ArrayList<>();
        final DragTab selectedTab = docSelected.getTabLabel();
        DragTab nextTab = selectedTab;

        while (nextTab != null) {
            priority.add(nextTab);
            nextTab = getNextTabInPriority(nextTab.getPriority());
        }

        // Like Einstein's cosmological constant, the extra "8" here
        // makes the whole thing work, but the reason for its existence is unknown.
        // Part of it is 4px of padding from lblHandle...
        int tempW = lblOverflow.getWidth() + margin + 8;
        int docOverflowCounter = 0;

        // Hide/show all other tabs.
        for (final DragTab tab : priority) {
            tempW += tab.getWidth() + margin;
            tab.setVisible(false);
            tab.setMaximumSize(null);

            if (tab.equals(selectedTab) || tempW < headW) { tab.setVisible(true); }
            else { docOverflowCounter++; }
        }

        // Resize selected tab if necessary.
        tempW = (docOverflowCounter == 0 ? headW - margin : headW - lblOverflow.getWidth() - margin - 10);
        selectedTab.setMaximumSize(new Dimension(tempW, 20));

        // Update overflow label
        lblOverflow.setText("+" + docOverflowCounter);
        if (docOverflowCounter == 0) { lblOverflow.setVisible(false); }
        else { lblOverflow.setVisible(true); }
    }

    private DragTab getNextTabInPriority(final int currentPriority0) {
        DragTab neo = null;
        DragTab temp;
        int lowest = Integer.MAX_VALUE;

        for (final IVDoc<? extends ICDoc> d : allDocs) {
            temp = d.getTabLabel();

            // This line prevents two tabs from having the same priority.
            if (neo != null && temp.getPriority() == lowest) {
                temp.priorityDecrease();
            }

            if (d.equals(docSelected))                 { continue; }
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
        private final SkinImage img = FSkin.getImage(FSkinProp.IMG_HANDLE);
        private boolean hovered = false;

        public DragHandle() {
            this.addMouseListener(SRearrangingUtil.getRearrangeClickEvent());
            this.addMouseMotionListener(SRearrangingUtil.getRearrangeDragEvent());

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    hovered = true; repaintSelf();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    hovered = false; repaintSelf();
                }
            });
        }

        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);
            if (!hovered) { return; }

            final Dimension imgSize = img.getSizeForPaint(g);
            final int imgW = imgSize.width;
            if (imgW < 1) { return; }
            final int imgH = imgSize.height;

            for (int x = 0; x < getWidth(); x += imgW) {
                FSkin.drawImage(g, img, x, ((getHeight() - imgH) / 2));
            }
        }
    }
}
