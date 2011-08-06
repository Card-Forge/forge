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

    public class ImageUtil {
       static public BufferedImage getImage (InputStream stream) throws IOException {
          Image tempImage = ImageIO.read(stream);
          BufferedImage image = new BufferedImage(tempImage.getWidth(null), tempImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
          Graphics2D g2 = image.createGraphics();
          g2.drawImage(tempImage, 0, 0, null);
          g2.dispose();
          return image;
       }
       
       static public BufferedImage getImage (File file) throws IOException {
           Image tempImage = ImageIO.read(file);
           BufferedImage image = new BufferedImage(tempImage.getWidth(null), tempImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
           Graphics2D g2 = image.createGraphics();
           g2.drawImage(tempImage, 0, 0, null);
           g2.dispose();
           return image;
        }

       static public BufferedImage getBlurredImage (BufferedImage image, int radius, float intensity) {
          float weight = intensity / (radius * radius);
          float[] elements = new float[radius * radius];
          for (int i = 0, n = radius * radius; i < n; i++)
             elements[i] = weight;
          ConvolveOp blurOp = new ConvolveOp(new Kernel(radius, radius, elements));
          return blurOp.filter(image, null);
       }
    }
