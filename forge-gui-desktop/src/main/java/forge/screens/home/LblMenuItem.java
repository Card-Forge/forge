package forge.screens.home;

import forge.gui.framework.ICDoc;
import forge.gui.framework.ILocalRepaint;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinnedLabel;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** 
 * Custom JLabel for an item in the menu. Handles listening
 * and repainting for hover and select events.
 */
@SuppressWarnings("serial")
public class LblMenuItem extends SkinnedLabel implements ILocalRepaint {
    private boolean selected = false;
    private boolean hovered = false;

    private final SkinColor clrTheme = FSkin.getColor(FSkin.Colors.CLR_THEME);
    private final SkinColor l00 = clrTheme.stepColor(0);
    private final SkinColor l20 = clrTheme.stepColor(20);
    private final SkinColor d20 = clrTheme.stepColor(-20);
    private final SkinColor d60 = clrTheme.stepColor(-60);
    private final SkinColor d80 = clrTheme.stepColor(-80);
    private final SkinColor alpha100 = l00.alphaColor(100);

    /**
     * Custom JLabel for an item in the menu. Handles listening
     * and repainting for hover and select events.
     * 
     * @param doc0 {@link forge.screens.home.IVSubmenu}
     */
    public LblMenuItem(final IVSubmenu<? extends ICDoc> doc0) {
        super("      " + doc0.getMenuTitle());

        this.setFont(FSkin.getFont(14));
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
            FSkin.setGraphicsColor(g2d, alpha100);
            g2d.fillRect(0, 0, w, h);
            FSkin.setGraphicsColor(g2d, d20);
            g2d.drawLine(0, 0, w - 3, 0);
            FSkin.setGraphicsColor(g2d, l20);
            g2d.drawLine(0, h - 1, w - 3, h - 1);
        }
        else if (this.hovered) {
            FSkin.setGraphicsColor(g2d, d60);
            g2d.fillRect(0, 0, getWidth(), h);

            FSkin.setGraphicsGradientPaint(g2d, 200 - 8, 0, l00, 200, 0, d80);
            g2d.fillRect(w - 2, 0, w, h);

            FSkin.setGraphicsColor(g2d, d20);
            g2d.drawLine(0, 0, w - 3, 0);
            FSkin.setGraphicsColor(g2d, l20);
            g2d.drawLine(0, h - 1, w - 3, h - 1);
        }

        super.paintComponent(g);
        g2d.dispose();
    }
}
