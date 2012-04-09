package forge.gui.layout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;
import forge.Singletons;

/** */
public enum FViewNew {
    /** */
    SINGLETON_INSTANCE;

    // Layout vars
    private final JFrame frmDocument = new JFrame();
    private final JPanel pnlContent = new JPanel();
    private final JPanel pnlInsets = new JPanel(new BorderLayout());
    private final JPanel pnlPreview = new PreviewPanel();
    private final JPanel pnlTabOverflow = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
    private final JLayeredPane lpnDocument = new JLayeredPane();
    private static final List<DragCell> CELLS = new ArrayList<DragCell>();

    /** Height of head area in drag panel. */
    public static final int HEAD_H = 20;
    /** Thickness of resize border in drag panel. */
    public static final int BORDER_T = 5;

    private FViewNew() {
        frmDocument.setMinimumSize(new Dimension(800, 600));
        frmDocument.setLocationRelativeTo(null);
        frmDocument.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmDocument.setContentPane(lpnDocument);
        frmDocument.setTitle("Forge: " + Singletons.getModel().getBuildInfo().getVersion());

        pnlInsets.add(pnlContent, BorderLayout.CENTER);
        pnlInsets.setBackground(Color.black);
        pnlInsets.setBorder(new EmptyBorder(
                FViewNew.BORDER_T, FViewNew.BORDER_T, 0, 0));

        pnlContent.setBackground(Color.black);
        pnlContent.setLayout(null);

        lpnDocument.addMouseListener(SOverflowUtil.getHideOverflowListener());
        lpnDocument.addComponentListener(SResizingUtil.getWindowResizeListener());

        lpnDocument.add(pnlInsets, (Integer) 0);
        lpnDocument.add(pnlPreview, (Integer) 1);
        lpnDocument.add(pnlTabOverflow, (Integer) 2);

        frmDocument.setVisible(true);
        pnlInsets.setBounds(lpnDocument.getBounds());
    }

    /** */
    public void populate() {
        DragCell cell0 = new DragCell();
        DragCell cell1 = new DragCell();
        DragCell cell2 = new DragCell();
        DragCell cell3 = new DragCell();
        DragCell cell4 = new DragCell();
        DragCell cell5 = new DragCell();

        cell0.addDoc(EDocID.REPORT_STACK.getDoc());
        cell0.addDoc(EDocID.REPORT_COMBAT.getDoc());
        cell0.addDoc(EDocID.REPORT_LOG.getDoc());
        cell0.addDoc(EDocID.REPORT_PLAYERS.getDoc());
        cell1.addDoc(EDocID.REPORT_MESSAGE.getDoc());
        cell2.addDoc(EDocID.YOUR_BATTLEFIELD.getDoc());
        cell3.addDoc(EDocID.YOUR_HAND.getDoc());
        cell4.addDoc(EDocID.YOUR_DOCK.getDoc());
        cell5.addDoc(EDocID.CARD_DETAIL.getDoc());
        cell5.addDoc(EDocID.CARD_PICTURE.getDoc());
        cell5.addDoc(EDocID.CARD_ANTES.getDoc());

        addDragCell(cell0);
        addDragCell(cell1);
        addDragCell(cell2);
        addDragCell(cell3);
        addDragCell(cell4);
        addDragCell(cell5);

        cell0.setRoughBounds(0, 0, 0.2, 0.7);
        cell1.setRoughBounds(0, 0.7, 0.2, 0.3);
        cell2.setRoughBounds(0.2, 0, 0.6, 0.5);
        cell3.setRoughBounds(0.2, 0.5, 0.6, 0.5);
        cell4.setRoughBounds(0.8, 0, 0.2, 0.25);
        cell5.setRoughBounds(0.8, 0.25, 0.2, 0.75);

        // TODO save a default layout, and then remove these lines and load it every time.
        //SIOUtil.saveLayout();
        //SIOUtil.loadLayout();
    }

    /** @return {@link javax.swing.JFrame} */
    public JFrame getFrame() {
        return frmDocument;
    }

    /** @return {@link javax.swing.JLayeredPane} */
    public JLayeredPane getLpnDocument() {
        return lpnDocument;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlInsets() {
        return pnlInsets;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlContent() {
        return pnlContent;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlPreview() {
        return pnlPreview;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlTabOverflow() {
        return pnlTabOverflow;
    }

    /** @return {@link java.util.List}<{@link forge.gui.layout.DragCell}> */
    public List<DragCell> getDragCells() {
        final List<DragCell> clone = new ArrayList<DragCell>();
        clone.addAll(CELLS);
        return clone;
    }

    /** @param pnl0 &emsp; {@link forge.gui.layout.DragCell} */
    public void addDragCell(final DragCell pnl0) {
        CELLS.add(pnl0);
        pnlContent.add(pnl0);
    }

    /** @param pnl0 &emsp; {@link forge.gui.layout.DragCell} */
    public void removeDragCell(final DragCell pnl0) {
        CELLS.remove(pnl0);
        pnlContent.remove(pnl0);
    }

    /** */
    public void removeAllDragCells() {
        CELLS.clear();
        pnlContent.removeAll();
    }

    /** PreviewPanel shows where a dragged component could
     * come to rest when the mouse is released.<br>
     * This class is an unfortunate necessity to overcome
     * translucency issues for preview panel. */
    @SuppressWarnings("serial")
    class PreviewPanel extends JPanel {
        /** PreviewPanel shows where a dragged component could
         * come to rest when the mouse is released. */
        public PreviewPanel() {
            super();
            setOpaque(false);
            setVisible(false);
            setBorder(new LineBorder(Color.DARK_GRAY, 2));
        }

        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(0, 0, 0, 50));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
