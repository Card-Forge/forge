
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;

/**
 * Class for working with images.
 *
 */
public class ImageUtil
{

    public static HashMap<String,BufferedImage> rotatedCache = new HashMap<String,BufferedImage>();
    
    /**
     * Rotates image and puts it into cache.
     * If image has already been rotated before it is read from cache
     *
     * @param source image to rotate
     * @return
     */
    final public static Image getTappedImage(Image original, String name)
    {
    	
        if (rotatedCache.containsKey(name))
        {
            return (Image) rotatedCache.get(name);
        }


        int width = original.getWidth(null);
        int height = original.getHeight(null);

        if (width == -1 || height == -1)
        {
            return null;
        }




        BufferedImage source = new BufferedImage(height, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = source.getGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();



        BufferedImage target = new BufferedImage(height, width, BufferedImage.TYPE_INT_ARGB);
        g = target.getGraphics();
        g.drawImage(rotateImage(source), 0, 0, null);
        g.dispose();

        rotatedCache.put(name, target);

        return target;
    }

    /**
     * Rotate image (90 degrees).
     *
     * @param source image to rotate
     * @return rotated image
     */
    final protected static BufferedImage rotateImage(BufferedImage source)
    {
        BufferedImage target = new BufferedImage(source.getHeight(), source.getWidth(), BufferedImage.TYPE_INT_ARGB);



        AffineTransform at = new AffineTransform();

        /**
         * Rotate 90 degrees around image center
         */
        at.rotate(90.0 * Math.PI / 180.0, source.getWidth() / 2.0, source.getHeight() / 2.0);


        //Draw from source (rotated) to target
        Graphics2D g = (Graphics2D) target.getGraphics();
        g.drawImage(source, at, null);
        g.dispose();


        //Code below is an alternate way to draw using straight Graphics2D - seems to be slower
        
        //Draw from source (rotated) to target
//        Graphics2D g = (Graphics2D) target.getGraphics();
//        g.rotate(Math.PI / 2.0, source.getWidth() / 2.0, source.getHeight() / 2.0);
//        g.drawImage(source, null, 0, 0);
//        g.dispose();

        return target;
    }
}