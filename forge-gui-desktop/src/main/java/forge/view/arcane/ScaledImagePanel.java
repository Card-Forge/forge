/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.view.arcane;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import forge.gui.GuiBase;

/**
 * <p>
 * ScaledImagePanel class.
 * </p>
 * 
 * @author Forge
 * @version $Id: ScaledImagePanel.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class ScaledImagePanel extends JPanel {

    /** Constant <code>serialVersionUID=-5691107238620895385L</code>. */
    private static final long serialVersionUID = -5691107238620895385L;
    /**
     * 
     */
    private volatile BufferedImage srcImage;

    /**
     * <p>
     * Constructor for ScaledImagePanel.
     * </p>
     */
    public ScaledImagePanel() {
        super(false);
        this.setOpaque(false);
    }

    /**
     * <p>
     * setImage.
     * </p>
     * 
     * @param srcImage
     *            a {@link java.awt.Image} object.
     * @param srcImageBlurred
     *            a {@link java.awt.Image} object.
     * 
     */
    public final void setImage(final BufferedImage srcImage) {
        this.srcImage = srcImage;
    }

    /**
     * <p>
     * clearImage.
     * </p>
     */
    public final void clearImage() {
        this.setImage(null);
        this.repaint();
    }


    /**
     * <p>
     * hasImage.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasImage() {
        return this.getSrcImage() != null;
    }

    /**
     * <p>
     * getScalingInfo.
     * </p>
     * 
     * @return a {@link forge.view.arcane.ScaledImagePanel.ScalingInfo} object.
     */

    /** {@inheritDoc} */
    @Override
    public final void paint(final Graphics g) {
        Dimension sz = getSize();
        BufferedImage src = this.getSrcImage(); 
        if (src == null) {
            return;
        }
        
        //ResampleOp resizer = new ResampleOp(DimensionConstrain.createMaxDimension(this.getWidth(), this.getHeight(), !scaleLarger));
        //resizer.setUnsharpenMask(UnsharpenMask.Soft);
        BufferedImage img = getSrcImage(); //resizer.filter(getSrcImage(), null);
        float screenScale = GuiBase.getInterface().getScreenScale();

        boolean needsScale = Math.round(img.getWidth() / screenScale) < sz.width;
        float scaleFactor = ((float)img.getWidth() / screenScale) / sz.width;
        if (needsScale && ( scaleFactor < 0.95 || scaleFactor > 1.05 )) { // This should very low-quality scaling to draw during animation
            float maxZoomX = ((float)sz.width) / (img.getWidth() / screenScale);
            float maxZoomY = ((float)sz.height) / (img.getHeight() / screenScale);
            float zoom = Math.min(maxZoomX, maxZoomY);

            int zoomedWidth = (int) (img.getWidth() / screenScale * zoom);
            int zoomedHeight = (int) (img.getHeight() / screenScale * zoom);
            int x = (sz.width - zoomedWidth) / 2;
            int y = (sz.height - zoomedHeight) / 2;

            g.drawImage(img, x, y, x + zoomedWidth, y + zoomedHeight, 0, 0, img.getWidth(), img.getHeight(), null);
        } else {
            int x = Math.round((sz.width / 2) - (img.getWidth() / screenScale / 2));
            int y = Math.round((sz.height / 2) - (img.getHeight() / screenScale / 2));
            g.drawImage(img, x, y, x + sz.width, y + sz.height, 0, 0, img.getWidth(), img.getHeight(), null);
        }
    }

    /**
     * Gets the src image.
     * 
     * @return the srcImage
     */
    public BufferedImage getSrcImage() {
        return this.srcImage;
    }

}
