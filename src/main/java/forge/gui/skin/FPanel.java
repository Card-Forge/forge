package forge.gui.skin;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.LayoutManager;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 * <p>
 * FPanel.
 * </p>
 * The core JPanel used throughout the Forge project. Allows tiled texture images
 * and single background images, using setBGTexture() and setBGImage() respectively.
 * 
 */
@SuppressWarnings("serial")
public class FPanel extends JPanel {
    private Image bgTexture, bgImg = null;
    // Panel Width, panel Height, Image Width, Image Height, Image Aspect Ratio
    private double w, h, iw, ih, iar, x, y = 0;

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
        w = this.getWidth();
        h = this.getHeight();

        // Draw background texture
        if (bgTexture != null) {
            iw = bgTexture.getWidth(null);
            ih = bgTexture.getHeight(null);
            x = 0;
            y = 0;

            while (x < w) {
                while (y < h) {
                    g.drawImage(bgTexture, (int) x, (int) y, null);
                    y += ih;
                }
                x += iw;
                y = 0;
            }
            x = 0;
        }

        // Draw background image
        if (bgImg != null) {
            iw = bgImg.getWidth(null);      // Image Width
            ih = bgImg.getHeight(null);     // Image Height
            iar = iw / ih;           // Image Aspect Ratio

            // Image is smaller than panel:
            if (w > iw && h > ih) {
                g.drawImage(bgImg, (int) (w - iw) / 2, (int) (h - ih) / 2, (int) iw, (int) ih, null);
            }
            // Image is larger than panel, and tall:
            else if (iar < 1) {
                g.drawImage(bgImg,
                        (int) ((w - h * iar) / 2), 0,
                        (int) ((w + h * iar) / 2), (int) h,
                        0, 0, (int) iw, (int) ih, null);
            }
            // Image is larger than panel, and wide:
            else if (iar > 1) {
                g.drawImage(bgImg,
                        0, (int) ((h - w / iar) / 2),
                        (int) w, (int) ((h + w / iar) / 2),
                        0, 0, (int) iw, (int) ih, null);
            }
        }

        super.paintComponent(g);
    }

    /**
     *  An FPanel can have a tiled texture and an image. The texture will be drawn
     *  first.  If a background image has been set, it will be drawn on top of the
     *  texture, centered and scaled proportional to its aspect ratio.
     *
     *  @param i0 &emsp; ImageIcon
     */
    public void setBGImg(final ImageIcon i0) {
        this.bgImg = i0.getImage();
        if (this.bgImg != null) {
            this.setOpaque(false);
        }
    }

    /**
     *  An FPanel can have a tiled texture and an image. The texture will be drawn
     *  first.  If a background image has been set, it will be drawn on top of the
     *  texture, centered and scaled proportional to its aspect ratio.
     *
     *  @param i0 &emsp; ImageIcon
     */
   public void setBGTexture(final ImageIcon i0) {
       this.bgTexture = i0.getImage();
       if (this.bgTexture != null) {
           this.setOpaque(false);
       }
   }

    /**
     * Sets the preferred size.
     *
     * @param w the w
     * @param h the h
     */
    public void setPreferredSize(final int w, final int h) {
        this.setPreferredSize(new Dimension(w, h));
    }

    /**
     * Sets the maximum size.
     *
     * @param w the w
     * @param h the h
     */
    public void setMaximumSize(final int w, final int h) {
        this.setMaximumSize(new Dimension(w, h));
    }

    /**
     * Sets the minimum size.
     *
     * @param w the w
     * @param h the h
     */
    public void setMinimumSize(final int w, final int h) {
        this.setMinimumSize(new Dimension(w, h));
    }
}
