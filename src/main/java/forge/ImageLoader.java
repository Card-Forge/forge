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

        File file = makeImageFile(path, filename);
        boolean fileExists = file.exists();
        if (!fileExists && filename.startsWith("S00") ) {
            file = makeImageFile(path, filename.replace("S00", "6ED"));
            fileExists = file.exists();
        }
        if (!fileExists ) {
            //System.out.println("File not found, no image created: " + file);
            return null;
        }
        final BufferedImage image = getImage(file);
        //ImageCache.IMAGE_CACHE.put(key, image);
        return image;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param path
     * @param filename
     * @return
     */
    private File makeImageFile(File path, String filename) {
        boolean isPng = filename.endsWith(".png");
        final String fName = isPng || filename.endsWith(".jpg") ? filename : filename + ".jpg";
        return new File(path, fName);
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