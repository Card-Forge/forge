package forge.gui.skin;

import java.awt.Graphics;
import java.awt.LayoutManager;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 * <p>
 * FPanel.
 * </p>
 * The core JPanel used throughout the Forge project. Allows tiled images and
 * ...
 * 
 */
@SuppressWarnings("serial")
public class FPanel extends JPanel {
    private ImageIcon bgImg = null;
    private int w, h, iw, ih, x, y = 0;

    /**
     * Instantiates a new f panel.
     */
    public FPanel() {
        super();
    }

    /**
     * Instantiates a new f panel.
     *
     * @param lm the lm
     */
    public FPanel(LayoutManager lm) {
        this();
        this.setLayout(lm);
    }

    /* (non-Javadoc)
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    protected void paintComponent(Graphics g) {
        // System.out.print("\nRepainting. ");
        if (this.bgImg != null) {
            w = getWidth();
            h = getHeight();
            iw = this.bgImg.getIconWidth();
            ih = this.bgImg.getIconHeight();

            while (x < w) {
                while (y < h) {
                    g.drawImage(bgImg.getImage(), x, y, null);
                    y += ih;
                }
                x += iw;
                y = 0;
            }
            x = 0;
        }

        super.paintComponent(g);
    }

    /**
     * Sets the bG img.
     *
     * @param icon the new bG img
     */
    public void setBGImg(ImageIcon icon) {
        this.bgImg = icon;
        if (this.bgImg != null) {
            this.setOpaque(false);
        }
    }
}
