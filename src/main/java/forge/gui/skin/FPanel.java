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
     * @param lm
     *            the lm
     */
    public FPanel(final LayoutManager lm) {
        this();
        this.setLayout(lm);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(final Graphics g) {
        // System.out.print("\nRepainting. ");
        if (this.bgImg != null) {
            this.w = this.getWidth();
            this.h = this.getHeight();
            this.iw = this.bgImg.getIconWidth();
            this.ih = this.bgImg.getIconHeight();

            while (this.x < this.w) {
                while (this.y < this.h) {
                    g.drawImage(this.bgImg.getImage(), this.x, this.y, null);
                    this.y += this.ih;
                }
                this.x += this.iw;
                this.y = 0;
            }
            this.x = 0;
        }

        super.paintComponent(g);
    }

    /**
     * Sets the bG img.
     * 
     * @param icon
     *            the new bG img
     */
    public void setBGImg(final ImageIcon icon) {
        this.bgImg = icon;
        if (this.bgImg != null) {
            this.setOpaque(false);
        }
    }
}
