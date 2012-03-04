package forge.gui.toolbox;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

/** Standardized tab for submenus in home screen. */
public class SubTab extends JPanel {
    private static final long serialVersionUID = -2193833603356739321L;
    private final Color clrBorders, clrHover;
    private final MouseAdapter madHover;

    private boolean enabled = false;
    private boolean hovering = false;
    private int w, h;

    /** @param s0 &emsp; {@link java.lang.String} tab text */
    public SubTab(String s0) {
        super();
        this.setOpaque(false);
        this.clrBorders = FSkin.getColor(FSkin.Colors.CLR_BORDERS);
        this.clrHover = FSkin.getColor(FSkin.Colors.CLR_HOVER);
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));

        this.madHover = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovering = true;
                repaintOnlyThisPanel();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                hovering = false;
                repaintOnlyThisPanel();
            }
        };
        this.removeMouseListener(madHover);
        this.addMouseListener(madHover);

        final JLabel lbl = new JLabel(s0);
        lbl.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        lbl.setFont(FSkin.getFont(12));
        this.add(lbl);
    }

    /** @param b0 &emsp; {@link java.lang.Boolean} */
    public void setEnabled(boolean b0) {
        this.enabled = b0;
        this.repaintOnlyThisPanel();
    }

    /** @return {@link java.lang.Boolean} */
    public boolean isEnabled() {
        return this.enabled;
    }

    /** Prevent panel from repainting the whole screen. */
    public void repaintOnlyThisPanel() {
        final Dimension d = SubTab.this.getSize();
        repaint(0, 0, d.width, d.height);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        w = getWidth();
        h = getHeight();

        g.setColor(clrBorders);

        if (this.enabled) {
            g.drawLine(0, h - 1, 3, h - 1); // SW
            g.drawLine(3, 10, 3, h); // W
            g.drawArc(3, 0, 20, 20, 90, 90); //NW
            g.drawLine(13, 0, w - 13, 0); //N
            g.drawArc(w - 23, 0, 20, 20, 90, -90); //NE
            g.drawLine(w - 3, 10, w - 3, h); //E
            g.drawLine(w - 3, h - 1, w, h - 1); //SE
        }
        else if (this.hovering) {
            g.drawLine(0, h - 1, w, h - 1);
            g.setColor(clrHover);
            g.fillArc(3, 0, 20, 20, 90, 90); //NW
            g.fillArc(w - 23, 0, 20, 20, 90, -90); //NE
            g.fillRect(3, 10, w - 6, h - 12); // Bottom
            g.fillRect(13, 0, w - 26, 10); // Top
        }
        else {
            g.drawLine(0, h - 1, w, h - 1);
        }
    }
}
