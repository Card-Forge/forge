package arcane.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * <p>ScaledImagePanel class.</p>
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
    public volatile Image srcImage;
    /**
     * 
     */
    public volatile Image srcImageBlurred;

    private ScalingType scalingType = ScalingType.bilinear;
    private boolean scaleLarger;
    private MultipassType multiPassType = MultipassType.bilinear;
    private boolean blur;

    /**
     * <p>Constructor for ScaledImagePanel.</p>
     */
    public ScaledImagePanel() {
        super(false);
        setOpaque(false);
    }

    /**
     * <p>setImage.</p>
     *
     * @param srcImage a {@link java.awt.Image} object.
     * @param srcImageBlurred a {@link java.awt.Image} object.
     *
     */
    public final void setImage(final Image srcImage, Image srcImageBlurred) {
        this.srcImage = srcImage;
        this.srcImageBlurred = srcImageBlurred;
    }

    /**
     * <p>clearImage.</p>
     */
    public final void clearImage() {
        srcImage = null;
        srcImageBlurred = null;
        repaint();
    }

    /**
     * <p>setScalingMultiPassType.</p>
     *
     * @param multiPassType a {@link arcane.ui.ScaledImagePanel.MultipassType} object.
     */
    public final void setScalingMultiPassType(final MultipassType multiPassType) {
        this.multiPassType = multiPassType;
    }

    /**
     * <p>Setter for the field <code>scalingType</code>.</p>
     *
     * @param scalingType a {@link arcane.ui.ScaledImagePanel.ScalingType} object.
     */
    public final void setScalingType(final ScalingType scalingType) {
        this.scalingType = scalingType;
    }

    /**
     * <p>setScalingBlur.</p>
     *
     * @param blur a boolean.
     */
    public final void setScalingBlur(final boolean blur) {
        this.blur = blur;
    }

    /**
     * <p>Setter for the field <code>scaleLarger</code>.</p>
     *
     * @param scaleLarger a boolean.
     */
    public final void setScaleLarger(final boolean scaleLarger) {
        this.scaleLarger = scaleLarger;
    }

    /**
     * <p>hasImage.</p>
     *
     * @return a boolean.
     */
    public final boolean hasImage() {
        return srcImage != null;
    }

    /**
     * <p>getScalingInfo.</p>
     *
     * @return a {@link arcane.ui.ScaledImagePanel.ScalingInfo} object.
     */
    private ScalingInfo getScalingInfo() {
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int srcWidth = srcImage.getWidth(null);
        int srcHeight = srcImage.getHeight(null);
        int targetWidth = srcWidth;
        int targetHeight = srcHeight;
        if (scaleLarger || srcWidth > panelWidth || srcHeight > panelHeight) {
            targetWidth = Math.round(panelHeight * (srcWidth / (float) srcHeight));
            if (targetWidth > panelWidth) {
                targetHeight = Math.round(panelWidth * (srcHeight / (float) srcWidth));
                targetWidth = panelWidth;
            } else {
                targetHeight = panelHeight;
            }
        }
        ScalingInfo info = new ScalingInfo();
        info.targetWidth = targetWidth;
        info.targetHeight = targetHeight;
        info.srcWidth = srcWidth;
        info.srcHeight = srcHeight;
        info.x = panelWidth / 2 - targetWidth / 2;
        info.y = panelHeight / 2 - targetHeight / 2;
        return info;
    }

    /** {@inheritDoc} */
    public final void paint(final Graphics g) {
        if (srcImage == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        ScalingInfo info = getScalingInfo();

        switch (scalingType) {
            case nearestNeighbor:
                scaleWithDrawImage(g2, info, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                break;
            case bilinear:
                scaleWithDrawImage(g2, info, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                break;
            case bicubic:
                scaleWithDrawImage(g2, info, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                break;
            case areaAveraging:
                scaleWithGetScaledInstance(g2, info, Image.SCALE_AREA_AVERAGING);
                break;
            case replicate:
                scaleWithGetScaledInstance(g2, info, Image.SCALE_REPLICATE);
                break;
        default:
            break;
        }
    }

    /**
     * <p>scaleWithGetScaledInstance.</p>
     *
     * @param g2 a {@link java.awt.Graphics2D} object.
     * @param info a {@link arcane.ui.ScaledImagePanel.ScalingInfo} object.
     * @param hints a int.
     */
    private void scaleWithGetScaledInstance(final Graphics2D g2, final ScalingInfo info, final int hints) {
        Image srcImage = getSourceImage(info);
        Image scaledImage = srcImage.getScaledInstance(info.targetWidth, info.targetHeight, hints);
        g2.drawImage(scaledImage, info.x, info.y, null);
    }

    /**
     * <p>scaleWithDrawImage.</p>
     *
     * @param g2 a {@link java.awt.Graphics2D} object.
     * @param info a {@link arcane.ui.ScaledImagePanel.ScalingInfo} object.
     * @param hint a {@link java.lang.Object} object.
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

        Image srcImage = getSourceImage(info);

        // If not doing multipass or multipass only needs a single pass,
        // just scale it once directly to the panel surface.
        if (multiPassType == MultipassType.none
                || (tempDestWidth == info.targetWidth && tempDestHeight == info.targetHeight))
        {
            g2.drawImage(srcImage, info.x, info.y, info.targetWidth, info.targetHeight, null);
            return;
        }

        BufferedImage tempImage = new BufferedImage(tempDestWidth, tempDestHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2temp = tempImage.createGraphics();
        switch (multiPassType) {
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

            if (tempDestWidth == info.targetWidth && tempDestHeight == info.targetHeight) {
                break;
            }

            g2temp.drawImage(tempImage, 0, 0, tempDestWidth, tempDestHeight, 0, 0, tempSrcWidth, tempSrcHeight, null);

            tempSrcWidth = tempDestWidth;
            tempSrcHeight = tempDestHeight;
        }
        g2temp.dispose();
        // Render last pass from temp to panel surface.
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
        g2.drawImage(tempImage, info.x,
                info.y,
                info.x + info.targetWidth,
                info.y + info.targetHeight, 0, 0, tempSrcWidth,
                tempSrcHeight, null);
    }

    /**
     * <p>getSourceImage.</p>
     *
     * @param info a {@link arcane.ui.ScaledImagePanel.ScalingInfo} object.
     * @return a {@link java.awt.Image} object.
     */
    private Image getSourceImage(final ScalingInfo info) {
        if (!blur || srcImageBlurred == null) {
            return srcImage;
        }
        if (info.srcWidth / 2 < info.targetWidth || info.srcHeight / 2 < info.targetHeight) {
            return srcImage;
        }
        return srcImageBlurred;
    }

    private static class ScalingInfo {
        public int targetWidth;
        public int targetHeight;
        public int srcWidth;
        public int srcHeight;
        public int x;
        public int y;
    }

    /**
     * 
     * MultipassType.
     *
     */
    public static enum MultipassType {
        /**
         * 
         */
        none,
        /**
         * 
         */
        nearestNeighbor,
        /**
         * 
         */
        bilinear,
        /**
         * 
         */
        bicubic
    }

    /**
     * 
     * ScalingType.
     *
     */
    public static enum ScalingType {
        /**
         * 
         */
        nearestNeighbor,
        /**
         * 
         */
        replicate,
        /**
         * 
         */
        bilinear,
        /**
         * 
         */
        bicubic,
        /**
         * 
         */
        areaAveraging
    }
}
