package forge.screens.home;

import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinnedLabel;

import javax.swing.border.EmptyBorder;

import java.awt.*;

/** 
 * Standardized header label for top of menu display panel.
 */
@SuppressWarnings("serial")
public class LblHeader extends SkinnedLabel {
    private final SkinColor clr = FSkin.getColor(FSkin.Colors.CLR_THEME).stepColor(0);
    private final SkinColor a100 = clr.alphaColor(100);
    private final SkinColor d40 = clr.stepColor(-40);
    private final SkinColor d80 = clr.stepColor(-80);

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

        FSkin.setGraphicsColor(g2d, d80);
        g2d.fillRect(0, 5, w, h - 5);

        FSkin.setGraphicsColor(g2d, a100);
        g2d.fillRect(5, 0, w - 5, h - 5);

        FSkin.setGraphicsColor(g2d, d40);
        g2d.drawRect(5, 0, w - 6, h - 6);

        super.paintComponent(g);
        g2d.dispose();
    }
}
