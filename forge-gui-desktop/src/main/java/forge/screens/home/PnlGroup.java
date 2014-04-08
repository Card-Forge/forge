package forge.screens.home;

import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinnedPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

import java.awt.*;

/** 
 * Custom JPanel for containing LblMenuItem components.
 * Mostly just handles repainting.
 */
@SuppressWarnings("serial")
public class PnlGroup extends SkinnedPanel {
    private final SkinColor clrTheme = FSkin.getColor(FSkin.Colors.CLR_THEME);
    private final SkinColor l00 = clrTheme.stepColor(0);
    private final SkinColor l10 = clrTheme.stepColor(10);
    private final SkinColor d20 = clrTheme.stepColor(-20);
    private final SkinColor d60 = clrTheme.stepColor(-60);
    private final SkinColor d80 = clrTheme.stepColor(-80);

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
        final JLabel lbl = CHomeUI.SINGLETON_INSTANCE.getLblSelected();
        int yTop = (lbl.getY() + lbl.getParent().getY());

        //super.paintComponent(g);
        final Graphics2D g2d = (Graphics2D) g.create();
        final int w = getWidth();
        final int h = getHeight();

        FSkin.setGraphicsColor(g2d, d20);

        // Selected in this group, don't draw background under selected label.
        if (getY() < yTop && yTop < getY() + h) {
            g2d.fillRect(0, 0, w, lbl.getY());
            g2d.fillRect(0, lbl.getY() + lbl.getHeight(), w, h);

            FSkin.setGraphicsGradientPaint(g2d, w - 8, 0, l00, w, 0, d80);
            g2d.fillRect(w - 6, 0, w, lbl.getY());
            g2d.fillRect(w - 6, lbl.getY() + lbl.getHeight(), w, h);
        }
        // Selected not in this group; draw full background.
        else {
            g2d.fillRect(0, 0, w, h);

            FSkin.setGraphicsGradientPaint(g2d, w - 8, 0, l00, w, 0, d80);
            g2d.fillRect(w - 6, 0, w, h);
        }

        FSkin.setGraphicsColor(g2d, l10);
        g2d.drawLine(0, h - 1, w - 1, h - 1);

        FSkin.setGraphicsColor(g2d, d60);
        g2d.drawLine(0, 0, w - 1, 0);

        g2d.dispose();
    }
}
