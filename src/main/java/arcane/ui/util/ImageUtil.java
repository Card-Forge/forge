package arcane.ui.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

/**
 * <p>
 * ImageUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ImageUtil {
    /**
     * <p>
     * getImage.
     * </p>
     * 
     * @param stream
     *            a {@link java.io.InputStream} object.
     * @return a {@link java.awt.image.BufferedImage} object.
     * @throws java.io.IOException
     *             if any.
     */
    public static BufferedImage getImage(final InputStream stream) throws IOException {
        Image tempImage = ImageIO.read(stream);
        BufferedImage image = new BufferedImage(tempImage.getWidth(null), tempImage.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.drawImage(tempImage, 0, 0, null);
        g2.dispose();
        return image;
    }

    /**
     * <p>
     * getImage.
     * </p>
     * 
     * @param file
     *            a {@link java.io.File} object.
     * @return a {@link java.awt.image.BufferedImage} object.
     * @throws java.io.IOException
     *             if any.
     */
    public static BufferedImage getImage(final File file) throws IOException {
        Image tempImage = ImageIO.read(file);
        BufferedImage image = new BufferedImage(tempImage.getWidth(null), tempImage.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.drawImage(tempImage, 0, 0, null);
        g2.dispose();
        return image;
    }

    /**
     * <p>
     * getBlurredImage.
     * </p>
     * 
     * @param image
     *            a {@link java.awt.image.BufferedImage} object.
     * @param radius
     *            a int.
     * @param intensity
     *            a float.
     * @return a {@link java.awt.image.BufferedImage} object.
     */
    public static BufferedImage getBlurredImage(final BufferedImage image, final int radius, final float intensity) {
        float weight = intensity / (radius * radius);
        float[] elements = new float[radius * radius];
        for (int i = 0, n = radius * radius; i < n; i++) {
            elements[i] = weight;
        }
        ConvolveOp blurOp = new ConvolveOp(new Kernel(radius, radius, elements));
        return blurOp.filter(image, null);
    }
}
