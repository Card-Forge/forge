package forge;

import com.google.common.cache.CacheLoader;

import forge.gui.error.BugReporter;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

final class ImageLoader extends CacheLoader<String, BufferedImage> {
    @Override
    public BufferedImage load(String key) {
        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DISABLE_CARD_IMAGES))
            return null;

        File file = ImageKeys.getImageFile(key);
        if (file != null) {
            if (!file.exists()) {
                return null;
            }
            if (file.isDirectory()) {
                file.delete();
                return null;
            }
            try {
                //it seems twelvemonkeys plugin handles the cmyk and other non standard colorspace jpeg automaticaly :)
                return ImageIO.read(file);
            }
            catch (IOException ex) {
                BugReporter.reportException(ex, "Could not read image file " + file.getAbsolutePath() + " ");
            }
        }
        return null;
    }
}
