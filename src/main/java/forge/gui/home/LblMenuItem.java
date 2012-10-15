package forge.gui.home;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

import forge.gui.framework.ILocalRepaint;
import forge.gui.toolbox.FSkin;

/** 
 * Custom JLabel for an item in the menu. Handles listening
 * and repainting for hover and select events.
 */
@SuppressWarnings("serial")
public class LblMenuItem extends JLabel implements ILocalRepaint {

    private boolean selected = false;
    private boolean hovered = false;

    private final Color clrTheme = FSkin.getColor(FSkin.Colors.CLR_THEME);
    private final Color l00 = FSkin.stepColor(clrTheme, 0);
    private final Color l20 = FSkin.stepColor(clrTheme, 20);
    private final Color d20 = FSkin.stepColor(clrTheme, -20);
    private final Color d60 = FSkin.stepColor(clrTheme, -60);
    private final Color d80 = FSkin.stepColor(clrTheme, -80);

    private final GradientPaint edge = new GradientPaint(200 - 8, 0, l00, 200, 0, d80, false);

    /**
     * Custom JLabel for an item in the menu. Handles listening
     * and repainting for hover and select events.
     * 
     * @param doc0 {@link forge.gui.home.IVSubmenu}
     */
    public LblMenuItem(final IVSubmenu doc0) {
        super("      " + doc0.getMenuTitle());
        this.setFont(FSkin.getFont(16));
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                CHomeUI.SINGLETON_INSTANCE.itemClick(doc0.getDocumentID());
            }

            @Override
            public void mouseEntered(final MouseEvent e) {
                hovered = true;
                repaintSelf();
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                hovered = false;
                repaintSelf();
            }
        });
    }

    /** @param b0 boolean */
    public void setSelected(final boolean b0) {
        this.selected = b0;
    }

    @Override
    public void repaintSelf() {
        final Dimension d = this.getSize();
        repaint(0, 0, d.width, d.height);
    }

    @Override
    public void paintComponent(Graphics g) {
        final Graphics2D g2d = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();

        if (this.selected) {
            g2d.setColor(FSkin.alphaColor(l00, 100));
            g2d.fillRect(0, 0, w, h);
            g2d.setColor(d20);
            g2d.drawLine(0, 0, w - 3, 0);
            g2d.setColor(l20);
            g2d.drawLine(0, h - 1, w - 3, h - 1);
        }
        else if (this.hovered) {
            g2d.setColor(d60);
            g2d.fillRect(0, 0, getWidth(), h);

            g2d.setPaint(edge);
            g2d.fillRect(w - 2, 0, w, h);

            g2d.setColor(d20);
            g2d.drawLine(0, 0, w - 3, 0);
            g2d.setColor(l20);
            g2d.drawLine(0, h - 1, w - 3, h - 1);
        }

        super.paintComponent(g);
        g2d.dispose();
    }
}
