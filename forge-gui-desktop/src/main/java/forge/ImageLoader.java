package forge;

import com.google.common.cache.CacheLoader;

import forge.error.BugReporter;
import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

final class ImageLoader extends CacheLoader<String, BufferedImage> {
    @Override
    public BufferedImage load(String key) {
        File file = ImageKeys.getImageFile(key);
        if (file != null) {
            try {
                return ImageIO.read(file);
            }
            catch (IOException ex) {
                BugReporter.reportException(ex, "Could not read image file " + file.getAbsolutePath() + " ");
            }
        }
        return null;
    }
}
