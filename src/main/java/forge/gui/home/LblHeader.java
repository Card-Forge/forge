package forge.gui.home;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

import forge.gui.toolbox.FSkin;

/** 
 * Standardized header label for top of menu display panel.
 */
@SuppressWarnings("serial")
public class LblHeader extends JLabel {
    private final Color clr = FSkin.stepColor(FSkin.getColor(FSkin.Colors.CLR_THEME), 0);
    private final Color a100 = FSkin.alphaColor(clr, 100);
    private final Color d40 = FSkin.stepColor(clr, -40);
    private final Color d80 = FSkin.stepColor(clr, -80);

    /**
     * Constructor.
     * @param txt0 {@link java.lang.String}
     */
    public LblHeader(final String txt0) {
        super(txt0);
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setFont(FSkin.getFont(18));
        this.setBorder(new EmptyBorder(5, 30, 0, 0));
    }

    @Override
    public void paintComponent(Graphics g) {
        final Graphics2D g2d = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();

        g2d.setColor(d80);
        g2d.fillRect(0, 5, w, h - 5);

        g2d.setColor(a100);
        g2d.fillRect(5, 0, w - 5, h - 5);

        g2d.setColor(d40);
        g2d.drawRect(5, 0, w - 6, h - 6);

        super.paintComponent(g);
        g2d.dispose();
    }
}
