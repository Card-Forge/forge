package forge.gui.layout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

/**
 * The tab label object in drag layout.
 * No modification should be necessary to this object.
 * Simply call the constructor with a title string argument.
 */
@SuppressWarnings("serial")
public final class DragTab extends JLabel implements ILocalRepaint {
    private boolean selected = false;
    private int priority = 10;

    /**
     * The tab label object in drag layout.
     * No modification should be necessary to this object.
     * Simply call the constructor with a title string argument.
     * 
     * @param title0 &emsp; {java.lang.String}
     */
    public DragTab(final String title0) {
        super(title0);
        setToolTipText(title0);
        setOpaque(false);
        setSelected(false);
        setBorder(new EmptyBorder(2, 5, 2, 5));

        this.addMouseListener(SRearrangingUtil.getRearrangeClickEvent());
        this.addMouseMotionListener(SRearrangingUtil.getRearrangeDragEvent());
    }

    /** @param isSelected0 &emsp; boolean */
    public void setSelected(final boolean isSelected0) {
        selected = isSelected0;
        repaintThis();
    }

    /** Decreases display priority of this tab in relation to its siblings in an overflow case. */
    public void priorityDecrease() {
        priority++;
    }

    /** Sets this tab as first to be displayed if siblings overflow. */
    public void priorityOne() {
        priority = 1;
    }

    /**
     * Returns display priority of this tab in relation to its siblings in an overflow case.
     * @return int
     */
    public int getPriority() {
        return priority;
    }

    @Override
    public void repaintThis() {
        final Dimension d = DragTab.this.getSize();
        repaint(0, 0, d.width, d.height);
    }

    @Override
    public void paintComponent(final Graphics g) {
        if (!selected) {
            g.setColor(Color.white);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() * 2, 6, 6);
            g.setColor(Color.LIGHT_GRAY);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() * 2, 6, 6);
        }
        else {
            g.setColor(Color.orange);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() * 2, 6, 6);
            g.setColor(Color.red);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() * 2, 6, 6);
        }

        super.paintComponent(g);
    }
}
