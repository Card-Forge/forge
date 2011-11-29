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
     * @param stream a {@link java.io.InputStream} object.
     * @return a {@link java.awt.image.BufferedImage} object.
     * @throws IOException Signals that an I/O exception has occurred.
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
     * @param file a {@link java.io.File} object.
     * @return a {@link java.awt.image.BufferedImage} object.
     * @throws IOException Signals that an I/O exception has occurred.
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
