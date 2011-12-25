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
package arcane.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * <p>
 * ScaledImagePanel class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ScaledImagePanel extends JPanel {

    /** Constant <code>serialVersionUID=-5691107238620895385L</code>. */
    private static final long serialVersionUID = -5691107238620895385L;
    /**
     * 
     */
    private volatile Image srcImage;
    /**
     * 
     */
    private volatile Image srcImageBlurred;

    private ScalingType scalingType = ScalingType.bilinear;
    private boolean scaleLarger;
    private MultipassType multiPassType = MultipassType.bilinear;
    private boolean blur;

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
    public final void setImage(final Image srcImage, final Image srcImageBlurred) {
        this.setSrcImage(srcImage);
        this.setSrcImageBlurred(srcImageBlurred);
    }

    /**
     * <p>
     * clearImage.
     * </p>
     */
    public final void clearImage() {
        this.setSrcImage(null);
        this.setSrcImageBlurred(null);
        this.repaint();
    }

    /**
     * <p>
     * setScalingMultiPassType.
     * </p>
     * 
     * @param multiPassType
     *            a {@link arcane.ui.ScaledImagePanel.MultipassType} object.
     */
    public final void setScalingMultiPassType(final MultipassType multiPassType) {
        this.multiPassType = multiPassType;
    }

    /**
     * <p>
     * Setter for the field <code>scalingType</code>.
     * </p>
     * 
     * @param scalingType
     *            a {@link arcane.ui.ScaledImagePanel.ScalingType} object.
     */
    public final void setScalingType(final ScalingType scalingType) {
        this.scalingType = scalingType;
    }

    /**
     * <p>
     * setScalingBlur.
     * </p>
     * 
     * @param blur
     *            a boolean.
     */
    public final void setScalingBlur(final boolean blur) {
        this.blur = blur;
    }

    /**
     * <p>
     * Setter for the field <code>scaleLarger</code>.
     * </p>
     * 
     * @param scaleLarger
     *            a boolean.
     */
    public final void setScaleLarger(final boolean scaleLarger) {
        this.scaleLarger = scaleLarger;
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
     * @return a {@link arcane.ui.ScaledImagePanel.ScalingInfo} object.
     */
    private ScalingInfo getScalingInfo() {
        final int panelWidth = this.getWidth();
        final int panelHeight = this.getHeight();
        final int srcWidth = this.getSrcImage().getWidth(null);
        final int srcHeight = this.getSrcImage().getHeight(null);
        int targetWidth = srcWidth;
        int targetHeight = srcHeight;
        if (this.scaleLarger || (srcWidth > panelWidth) || (srcHeight > panelHeight)) {
            targetWidth = Math.round(panelHeight * (srcWidth / (float) srcHeight));
            if (targetWidth > panelWidth) {
                targetHeight = Math.round(panelWidth * (srcHeight / (float) srcWidth));
                targetWidth = panelWidth;
            } else {
                targetHeight = panelHeight;
            }
        }
        final ScalingInfo info = new ScalingInfo();
        info.targetWidth = targetWidth;
        info.targetHeight = targetHeight;
        info.srcWidth = srcWidth;
        info.srcHeight = srcHeight;
        info.x = (panelWidth / 2) - (targetWidth / 2);
        info.y = (panelHeight / 2) - (targetHeight / 2);
        return info;
    }

    /** {@inheritDoc} */
    @Override
    public final void paint(final Graphics g) {
        if (this.getSrcImage() == null) {
            return;
        }

        final Graphics2D g2 = (Graphics2D) g.create();
        final ScalingInfo info = this.getScalingInfo();

        switch (this.scalingType) {
        case nearestNeighbor:
            this.scaleWithDrawImage(g2, info, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            break;
        case bilinear:
            this.scaleWithDrawImage(g2, info, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            break;
        case bicubic:
            this.scaleWithDrawImage(g2, info, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            break;
        case areaAveraging:
            this.scaleWithGetScaledInstance(g2, info, Image.SCALE_AREA_AVERAGING);
            break;
        case replicate:
            this.scaleWithGetScaledInstance(g2, info, Image.SCALE_REPLICATE);
            break;
        default:
            break;
        }
    }

    /**
     * <p>
     * scaleWithGetScaledInstance.
     * </p>
     * 
     * @param g2
     *            a {@link java.awt.Graphics2D} object.
     * @param info
     *            a {@link arcane.ui.ScaledImagePanel.ScalingInfo} object.
     * @param hints
     *            a int.
     */
    private void scaleWithGetScaledInstance(final Graphics2D g2, final ScalingInfo info, final int hints) {
        final Image srcImage = this.getSourceImage(info);
        final Image scaledImage = srcImage.getScaledInstance(info.targetWidth, info.targetHeight, hints);
        g2.drawImage(scaledImage, info.x, info.y, null);
    }

    /**
     * <p>
     * scaleWithDrawImage.
     * </p>
     * 
     * @param g2
     *            a {@link java.awt.Graphics2D} object.
     * @param info
     *            a {@link arcane.ui.ScaledImagePanel.ScalingInfo} object.
     * @param hint
     *            a {@link java.lang.Object} object.
     */
    private void scaleWithDrawImage(final Graphics2D g2, final ScalingInfo info, final Object hint) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);

        int tempDestWidth = info.srcWidth / 2, tempDestHeight = info.srcHeight / 2;
        if (tempDestWidth < info.targetWidth) {
            tempDestWidth = info.targetWidth;
        }
        if (tempDestHeight < info.targetHeight) {
            tempDestHeight = info.targetHeight;
        }

        final Image srcImage = this.getSourceImage(info);

        // If not doing multipass or multipass only needs a single pass,
        // just scale it once directly to the panel surface.
        if ((this.multiPassType == MultipassType.none)
                || ((tempDestWidth == info.targetWidth) && (tempDestHeight == info.targetHeight))) {
            g2.drawImage(srcImage, info.x, info.y, info.targetWidth, info.targetHeight, null);
            return;
        }

        final BufferedImage tempImage = new BufferedImage(tempDestWidth, tempDestHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2temp = tempImage.createGraphics();
        switch (this.multiPassType) {
        case nearestNeighbor:
            g2temp.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            break;
        case bilinear:
            g2temp.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            break;
        case bicubic:
            g2temp.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            break;
        default:
            break;
        }
        // Render first pass from image to temp.
        g2temp.drawImage(srcImage, 0, 0, tempDestWidth, tempDestHeight, null);
        // Render passes between the first and last pass.
        int tempSrcWidth = tempDestWidth;
        int tempSrcHeight = tempDestHeight;
        while (true) {
            if (tempDestWidth > info.targetWidth) {
                tempDestWidth = tempDestWidth / 2;
                if (tempDestWidth < info.targetWidth) {
                    tempDestWidth = info.targetWidth;
                }
            }

            if (tempDestHeight > info.targetHeight) {
                tempDestHeight = tempDestHeight / 2;
                if (tempDestHeight < info.targetHeight) {
                    tempDestHeight = info.targetHeight;
                }
            }

            if ((tempDestWidth == info.targetWidth) && (tempDestHeight == info.targetHeight)) {
                break;
            }

            g2temp.drawImage(tempImage, 0, 0, tempDestWidth, tempDestHeight, 0, 0, tempSrcWidth, tempSrcHeight, null);

            tempSrcWidth = tempDestWidth;
            tempSrcHeight = tempDestHeight;
        }
        g2temp.dispose();
        // Render last pass from temp to panel surface.
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
        g2.drawImage(tempImage, info.x, info.y, info.x + info.targetWidth, info.y + info.targetHeight, 0, 0,
                tempSrcWidth, tempSrcHeight, null);
    }

    /**
     * <p>
     * getSourceImage.
     * </p>
     * 
     * @param info
     *            a {@link arcane.ui.ScaledImagePanel.ScalingInfo} object.
     * @return a {@link java.awt.Image} object.
     */
    private Image getSourceImage(final ScalingInfo info) {
        if (!this.blur || (this.getSrcImageBlurred() == null)) {
            return this.getSrcImage();
        }
        if (((info.srcWidth / 2) < info.targetWidth) || ((info.srcHeight / 2) < info.targetHeight)) {
            return this.getSrcImage();
        }
        return this.getSrcImageBlurred();
    }

    /**
     * Gets the src image.
     * 
     * @return the srcImage
     */
    public Image getSrcImage() {
        return this.srcImage;
    }

    /**
     * Sets the src image.
     * 
     * @param srcImage0
     *            the srcImage to set
     */
    public void setSrcImage(final Image srcImage0) {
        this.srcImage = srcImage0;
    }

    /**
     * Gets the src image blurred.
     * 
     * @return the srcImageBlurred
     */
    public Image getSrcImageBlurred() {
        return this.srcImageBlurred;
    }

    /**
     * Sets the src image blurred.
     * 
     * @param srcImageBlurred0
     *            the srcImageBlurred to set
     */
    public void setSrcImageBlurred(final Image srcImageBlurred0) {
        this.srcImageBlurred = srcImageBlurred0;
    }

    private static class ScalingInfo {
        private int targetWidth;
        private int targetHeight;
        private int srcWidth;
        private int srcHeight;
        private int x;
        private int y;
    }

    /**
     * 
     * MultipassType.
     * 
     */
    public static enum MultipassType {

        /** The none. */
        none,

        /** The nearest neighbor. */
        nearestNeighbor,

        /** The bilinear. */
        bilinear,

        /** The bicubic. */
        bicubic
    }

    /**
     * 
     * ScalingType.
     * 
     */
    public static enum ScalingType {

        /** The nearest neighbor. */
        nearestNeighbor,

        /** The replicate. */
        replicate,

        /** The bilinear. */
        bilinear,

        /** The bicubic. */
        bicubic,

        /** The area averaging. */
        areaAveraging
    }
}
