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

/** */
public enum FViewNew {
    /** */
    SINGLETON_INSTANCE;

    // Layout vars
    private final JFrame frmDocument = new JFrame();
    private final JPanel pnlContent = new JPanel();
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
        frmDocument.setTitle("Drag Layout: 4");

        final JPanel pnlInsets = new JPanel(new BorderLayout());
        pnlInsets.add(pnlContent);
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
        SIOUtil.saveLayout();
        SIOUtil.loadLayout();
    }

    /** @return {@link javax.swing.JLayeredPane} */
    public JLayeredPane getLpnDocument() {
        return lpnDocument;
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
