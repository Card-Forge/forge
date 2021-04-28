package forge;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.Raster;
import java.io.File;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.google.common.cache.CacheLoader;

import forge.gui.error.BugReporter;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

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
                return ImageIO.read(file);
            } catch (Exception e) {
                try {
                    //Find a suitable ImageReader
                    Iterator readers = ImageIO.getImageReadersByFormatName("JPEG");
                    ImageReader reader = null;
                    while (readers.hasNext()) {
                        reader = (ImageReader) readers.next();
                        if (reader.canReadRaster()) {
                            break;
                        }
                    }
                    //Stream the image file (the original CMYK image)
                    ImageInputStream input;
                    input = ImageIO.createImageInputStream(file);
                    if (input == null) {
                        System.err.println("ImageIO.createImageInputStream return null");
                        return null;
                    }
                    reader.setInput(input);
                    //Read the image raster
                    Raster raster = reader.readRaster(0, null);
                    //Create a new RGB image
                    BufferedImage bi = new BufferedImage(raster.getWidth(), raster.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
                    //Fill the new image with the old raster
                    bi.getRaster().setRect(raster);
                    BufferedImage colorConverted = colorConvert(bi);
                    return colorConverted;
                } catch (Exception ex) {
                    BugReporter.reportException(ex, "Could not read image file " + file.getAbsolutePath() + " ");
                }
            }
        }
        return null;
    }
    private static BufferedImage colorConvert(BufferedImage img) {
        BufferedImage bufferedImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        bufferedImage.getGraphics().drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
        bufferedImage = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), null).filter(img, bufferedImage);
        return bufferedImage;
    }
}
