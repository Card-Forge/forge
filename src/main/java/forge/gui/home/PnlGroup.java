package forge.gui.home;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.gui.toolbox.FSkin;

/** 
 * Custom JPanel for containing LblMenuItem components.
 * Mostly just handles repainting.
 */
@SuppressWarnings("serial")
public class PnlGroup extends JPanel {
    private final Color clrTheme = FSkin.getColor(FSkin.Colors.CLR_THEME);
    private final Color l00 = FSkin.stepColor(clrTheme, 0);
    private final Color l10 = FSkin.stepColor(clrTheme, 10);
    private final Color d20 = FSkin.stepColor(clrTheme, -20);
    private final Color d60 = FSkin.stepColor(clrTheme, -60);
    private final Color d80 = FSkin.stepColor(clrTheme, -80);

    /**
     * Custom JPanel for containing LblMenuItem components.
     * Mostly just handles repainting.
     */
    public PnlGroup() {
        this.setLayout(new MigLayout("insets 10px 0 10px 0, gap 0, wrap"));
        this.setBackground(d20);
        this.setOpaque(false);
    }

    @Override
    public void paintComponent(Graphics g) {
        final JLabel lbl = VHomeUI.SINGLETON_INSTANCE.getLblSelected();
        int yTop = (lbl.getY() + lbl.getParent().getY());

        //super.paintComponent(g);
        final Graphics2D g2d = (Graphics2D) g.create();
        final int w = getWidth();
        final int h = getHeight();

        g2d.setColor(d20);

        // Selected in this group, don't draw background under selected label.
        if (getY() < yTop && yTop < getY() + h) {
            g2d.fillRect(0, 0, w, lbl.getY());
            g2d.fillRect(0, lbl.getY() + lbl.getHeight(), w, h);

            GradientPaint edge = new GradientPaint(w - 8, 0, l00, w, 0, d80, false);
            g2d.setPaint(edge);
            g2d.fillRect(w - 6, 0, w, lbl.getY());
            g2d.fillRect(w - 6, lbl.getY() + lbl.getHeight(), w, h);
        }
        // Selected not in this group; draw full background.
        else {
            g2d.fillRect(0, 0, w, h);

            GradientPaint edge = new GradientPaint(w - 8, 0, l00, w, 0, d80, false);
            g2d.setPaint(edge);
            g2d.fillRect(w - 6, 0, w, h);
        }

        g2d.setColor(l10);
        g2d.drawLine(0, h - 1, w - 1, h - 1);

        g2d.setColor(d60);
        g2d.drawLine(0, 0, w - 1, 0);

        g2d.dispose();
    }
}
