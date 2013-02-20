package forge;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import com.google.common.cache.CacheLoader;

import forge.error.BugReporter;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/** 
 * TODO: Write javadoc for this type.
 *
 */
final class ImageLoader extends CacheLoader<String, BufferedImage> {
    @Override
    public BufferedImage load(String key) {
        // original
        File path;
        String filename;
        if (key.startsWith(ImageCache.TOKEN)) {
            filename = key.substring(ImageCache.TOKEN.length());
            path = ForgeProps.getFile(NewConstants.IMAGE_TOKEN);
        } else if (key.startsWith(ImageCache.SEALED_PRODUCT)) {
            filename = key.substring(ImageCache.SEALED_PRODUCT.length());
            path = ForgeProps.getFile(NewConstants.IMAGE_SEALED_PRODUCT);
        } else {
            filename = key;
            path = ForgeProps.getFile(NewConstants.IMAGE_BASE);
        }

        File file = null;
        boolean isPng = filename.endsWith(".png");
        final String fName = isPng || filename.endsWith(".jpg") ? filename : filename + ".jpg";
        file = new File(path, fName);
        if (!file.exists()) {
            // DEBUG
            //System.out.println("File not found, no image created: "
            //+ file);
            return null;
        }
        final BufferedImage image = getImage(file);
        //ImageCache.IMAGE_CACHE.put(key, image);
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
    public static BufferedImage getImage(final File file) {
        //System.out.printf("Loading from disk: %s\n", file.toString());
        
        BufferedImage image;
        //int format = useAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        try {
            image = ImageIO.read(file);
        } catch (IOException ex) {
            BugReporter.reportException(ex, "Could not read image file " + file.getAbsolutePath() + " ");
            return null;
        }
        return image;
    }
}