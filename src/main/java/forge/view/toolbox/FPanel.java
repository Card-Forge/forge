/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.view.toolbox;

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
 * The core JPanel used throughout the Forge project. Allows tiled texture
 * images and single background images, using setBGTexture() and setBGImage()
 * respectively.
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
        this.w = this.getWidth();
        this.h = this.getHeight();

        // Draw background texture
        if (this.bgTexture != null) {
            this.iw = this.bgTexture.getWidth(null);
            this.ih = this.bgTexture.getHeight(null);
            this.x = 0;
            this.y = 0;

            while (this.x < this.w) {
                while (this.y < this.h) {
                    g.drawImage(this.bgTexture, (int) this.x, (int) this.y, null);
                    this.y += this.ih;
                }
                this.x += this.iw;
                this.y = 0;
            }
            this.x = 0;
        }

        // Draw background image
        if (this.bgImg != null) {
            this.iw = this.bgImg.getWidth(null); // Image Width
            this.ih = this.bgImg.getHeight(null); // Image Height
            this.iar = this.iw / this.ih; // Image Aspect Ratio

            // Image is smaller than panel:
            if ((this.w > this.iw) && (this.h > this.ih)) {
                g.drawImage(this.bgImg, (int) (this.w - this.iw) / 2, (int) (this.h - this.ih) / 2, (int) this.iw,
                        (int) this.ih, null);
            }
            // Image is larger than panel, and tall:
            else if (this.iar < 1) {
                g.drawImage(this.bgImg, (int) ((this.w - (this.h * this.iar)) / 2), 0,
                        (int) ((this.w + (this.h * this.iar)) / 2), (int) this.h, 0, 0, (int) this.iw, (int) this.ih,
                        null);
            }
            // Image is larger than panel, and wide:
            else if (this.iar > 1) {
                g.drawImage(this.bgImg, 0, (int) ((this.h - (this.w / this.iar)) / 2), (int) this.w,
                        (int) ((this.h + (this.w / this.iar)) / 2), 0, 0, (int) this.iw, (int) this.ih, null);
            }
        }

        super.paintComponent(g);
    }

    /**
     * An FPanel can have a tiled texture and an image. The texture will be
     * drawn first. If a background image has been set, it will be drawn on top
     * of the texture, centered and scaled proportional to its aspect ratio.
     * 
     * @param i0
     *            &emsp; ImageIcon
     */
    public void setBGImg(final ImageIcon i0) {
        this.bgImg = i0.getImage();
        if (this.bgImg != null) {
            this.setOpaque(false);
        }
    }

    /**
     * An FPanel can have a tiled texture and an image. The texture will be
     * drawn first. If a background image has been set, it will be drawn on top
     * of the texture, centered and scaled proportional to its aspect ratio.
     * 
     * @param i0
     *            &emsp; ImageIcon
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
     * @param w
     *            the w
     * @param h
     *            the h
     */
    public void setPreferredSize(final int w, final int h) {
        this.setPreferredSize(new Dimension(w, h));
    }

    /**
     * Sets the maximum size.
     * 
     * @param w
     *            the w
     * @param h
     *            the h
     */
    public void setMaximumSize(final int w, final int h) {
        this.setMaximumSize(new Dimension(w, h));
    }

    /**
     * Sets the minimum size.
     * 
     * @param w
     *            the w
     * @param h
     *            the h
     */
    public void setMinimumSize(final int w, final int h) {
        this.setMinimumSize(new Dimension(w, h));
    }
}
