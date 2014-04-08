/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2013  Forge Team
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

package forge.toolbox.imaging;

import com.mortennobel.imagescaling.DimensionConstrain;
import com.mortennobel.imagescaling.ResampleOp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Displays a {@code BufferedImage} at its center.
 * <p>
 * Options to scale and rotate the image are available if required.
 * 
 * @version $Id: FImagePanel.java 24769 2014-02-09 13:56:04Z Hellfish $
 * 
 */
@SuppressWarnings("serial")
public class FImagePanel extends JPanel {

    // See {@code setAutosizeMode} for descriptions.
    public enum AutoSizeImageMode {OFF, PANEL, SOURCE};
    AutoSizeImageMode autoSizeMode = AutoSizeImageMode.PANEL;

    // The original unscaled, unrotated image.
    // Remains the same regardless of any transformations that might be applied to it.
    private BufferedImage sourceImage = null;

    // Resampling is an expensive operation so keep a copy of last resampled image and
    // use this for repaints if image has not been resized or changed.
    private BufferedImage scaledImage = null;
    private boolean isResampleEnabled = true;

    private double imageScale = 1;
    private int degreesOfRotation = 0;

    // Ensures that when resizing only {@code doPerformancePaint} is used.
    private boolean isResizing = false;

    private Timer resizingTimer = createResizingTimer(100);

    // ctr
    public FImagePanel() {
        setOpaque(false);
        setResizeListener();
    };

    /**
     * This timer is used to identify when resizing has finished.
     * <p>
     * Each time a resize event is fired, the timer is restarted and waits the
     * specified number of {@code timerDelay} milliseconds before firing. When
     * multiple resize events are fired the timer will never trigger because it
     * will keep getting restarted - see {@code setResizeListener} method.
     */
    private Timer createResizingTimer(int timerDelay) {
        return new Timer(timerDelay,  new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                doResizedFinished();
            }
        });
    }

    private void doResizedFinished() {
        this.resizingTimer.stop();
        this.isResizing = false;
        this.isResampleEnabled = true;
        this.repaint();
    }

    /**
     * Ensures that when resizing only {@code doPerformancePaint} is used.
     */
    private void setResizeListener() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                isResizing = true;
                resizingTimer.restart();
            }
        });
    }

    /**
     * Displays {@code BufferedImage} with the specified rotation and auto-size mode.
     * <p>
     * Note that rotations are currently rounded to the <b>nearest 90</b> degrees.
     * This means the image can only have either a vertical or horizontal orientation.
     */
    public void setImage(BufferedImage image, int initialRotation, AutoSizeImageMode autoSizeMode) {
        if (this.sourceImage != image || this.degreesOfRotation != initialRotation || this.autoSizeMode != autoSizeMode) {
            isResampleEnabled = true;
            this.autoSizeMode = autoSizeMode;
            if (initialRotation > 0) { setRotation(initialRotation); }
            this.sourceImage = image;
            repaint();
        }
    }

    public void setImage(BufferedImage image, AutoSizeImageMode autoSizeMode) {
        setImage(image, 0, autoSizeMode);
    }

    /**
     * Displays {@code BufferedImage} with the specified rotation.
     * <p>
     * Image is auto-resized to fit {@code FImagePanel}.
     * <p>
     * Note that rotations are currently rounded to the <b>nearest 90</b> degrees.
     * This means the image can only have either a vertical or horizontal orientation.
     */
    public void setImage(BufferedImage image, int initialRotation) {
        setImage(image, initialRotation, autoSizeMode);
    }

    /**
     * Displays {@code BufferedImage} with no rotation.
     * <p>
     * Image is auto-resized to fit {@code FImagePanel}.
     */
    public void setImage(BufferedImage image) {
        setImage(image, 0);
    }

    /**
     * Shows dimensions of image panel using a dashed border.
     */
    public void setDashedBorder(boolean showBorder) {
        setBorder(showBorder ? BorderFactory.createDashedBorder(null) : null);
    }

    /**
     * Rotates image to the <b>nearest 90</b> degrees.
     * <p>
     * This means the image can only have either a vertical or horizontal orientation.
     */
    public void setRotation(int degrees) {
        if (this.degreesOfRotation != degrees) {
            this.degreesOfRotation = FImageUtil.getRotationToNearest(degrees, 90);
            isResampleEnabled = true;
            repaint();
        }
    }

    /**
     * Gets the rotation of the displayed image relative to the original image.
     * <p>
     * <b>Note</b><br>
     * The returned value may not be the same as that specified in {@code SetRotation}
     * since the rotation is currently rounded to the nearest 90 degrees.
     */
    public int getRotation() {
        return this.degreesOfRotation;
    }

    /**
     * Gets the dimensions of the original unscaled image taking into account the current rotation.
     * <p>
     * These dimensions are dependent on the orientation relative to the original unrotated image.
     * If display orientation is perpendicular to the original then width and height will be reversed.
     */
    private Dimension getSourceImageSize() {
        Dimension originalSize = new Dimension(this.sourceImage.getWidth(), this.sourceImage.getHeight());
        Dimension rotatedSize = new Dimension(this.sourceImage.getHeight(), this.sourceImage.getWidth());
        boolean isOriginalOrientation = (this.degreesOfRotation % 180 == 0);
        return (isOriginalOrientation ? originalSize  : rotatedSize);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (this.sourceImage != null) {
            setImageScale();
            if (this.isResizing) {
                doPerformancePaint(g);
            } else {
                doQualityPaint(g);
            }
        }
    }

    private void doQualityPaint(Graphics g) {
        BufferedImage resampledImage = getResampledImage();
        if (resampledImage != null) {
            Graphics2D g2d = (Graphics2D)g;
            g2d.drawImage(resampledImage, getAffineTransform(resampledImage, false), null);
        }
    }

    /**
     * Uses Morten Nobel's java-image-scaling library to resize image.
     * <p>
     * This produces superior quality to affine scaling especially as
     * image sizes are reduced but at the cost of performance.
     * <p>
     * You cannot legislate for when this will be called since it depends
     * on how often paintComponent() is invoked and any number of external
     * events can cause this to happen. But resampling is an expensive operation
     * so use an existing copy if the image has not changed or been resized.
     */
    private BufferedImage getResampledImage() {
        if (this.imageScale != 1) {
            if (isResampleEnabled) {
                isResampleEnabled = false;
                DimensionConstrain constrain = DimensionConstrain.createRelativeDimension((float)this.imageScale);
                ResampleOp resampler = new ResampleOp(constrain);
                this.scaledImage = resampler.filter(sourceImage, null);
            }
        } else {
            this.scaledImage = sourceImage;
        }
        return this.scaledImage;
    }

    /**
     * Renders image without using any additional re-sampling.
     * <p>
     * For scales of approximately 0.7 or above this should produce more than acceptable results.
     * As the image size is reduced there is a distinct degradation in quality and a resampling
     * algorithm should be used if image quality is paramount.
     */
    private void doPerformancePaint(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        setRenderingHints(g2d);
        g2d.drawImage(sourceImage, getAffineTransform(sourceImage, true), null);
    }

    /**
     * Sets Rendering hints which can improve quality of image without requiring resampling.
     * <p>
     * Becomes ineffective as image size becomes smaller.
     */
    private void setRenderingHints(Graphics2D g2d) {
        if (!this.isResizing) {
            // These hints make a visible difference...
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // ...not so sure about...
            //g2d.setComposite(AlphaComposite.Src);
            //g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

    /**
     *  Using an Affine transformation, moves the image to the center of FImagePane.
     *  Scales and rotates the image at the same time if required.
     * 
     *  @param image the {@code BufferedImage} to be manipulated.
     *  @param createScaleTransform set to false if no scale transform is required.
     * 
     */
    private AffineTransform getAffineTransform(BufferedImage image, boolean createScaleTransform) {

        // Note that transformations happen in reverse order.
        AffineTransform at = new AffineTransform();

        // 4. move image to the center of FImagePanel. Remember, image center point is (0,0).
        at.translate(this.getWidth() / 2, this.getHeight() / 2);

        // 3. rotate around (0,0).
        at.rotate(Math.toRadians((double) degreesOfRotation));

        // 2. scale image.
        if (createScaleTransform) {
            at.scale(this.imageScale, this.imageScale);
        }

        // 1. move the image so that its center is at (0,0).
        at.translate(-image.getWidth() / 2, -image.getHeight() / 2);

        return at;

    }

    /**
     * Determines how the image should fit into FImagePanel.
     * <p>
     * Any automatic resizing will retain the original image's aspect ratio.
     * 
     * @param autoSizeMode can take one of following values :-
     * <p>
     * <b>OFF</b> - Display image at its original size regardless of FImagePanel size.
     * <p>
     * <b>PANEL</b> - Image will automatically resize to fit FImagePanel.
     * <p>
     * <b>SOURCE</b> - Image will automatically resize to fit FImagePanel unless doing so
     * would exceed the original image size. In this case, the Image will not expand beyond
     * its original size.
     */
    public void setAutosizeMode(AutoSizeImageMode autoSizeMode) {
        this.autoSizeMode = autoSizeMode;
        repaint();
    }

    /**
     * Determines the scale that needs to be applied to the image so
     * that it meets the requirements of the current {@code AutoSizeImageMode}.
     */
    private void setImageScale() {
        if (this.sourceImage != null) {
            if (this.autoSizeMode != AutoSizeImageMode.OFF) {
                Double newScale = FImageUtil.getBestFitScale(getSourceImageSize(), this.getSize());
                if (newScale != this.imageScale) {
                    isResampleEnabled = true;
                    this.imageScale = newScale;
                    if (newScale == 0) { this.imageScale = 1; };
                    if (this.autoSizeMode == AutoSizeImageMode.SOURCE && newScale > 1) {
                        this.imageScale = 1;
                    }
                }
            }
        }
    }

}
