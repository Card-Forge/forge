package forge;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

import javax.imageio.ImageIO;

import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * For storing card images in a cache.
 */
public class ImageCache implements NewConstants {
    
    /**
     * Card size.
     */
    //protected static Rectangle cardSize = new Rectangle(93, 130);
    //protected static Rectangle cardSize = new Rectangle(70, 98);
    //public static Rectangle cardSize = new Rectangle(70,98);
    public static Rectangle              cardSize = new Rectangle(Constant.Runtime.width[0],
                                                          Constant.Runtime.height[0]);
    

    /**
     * Cache for storing images.
     */
    public static HashMap<String, Image> cache    = new HashMap<String, Image>();
    
    //protected static HashMap<String, Image> cache = new HashMap<String, Image>();
    
    //~  Image size was not getting updated if the user changed it between games...
    public static void dumpCache() {
        cardSize = new Rectangle(Constant.Runtime.width[0], Constant.Runtime.height[0]);
        cache = new HashMap<String, Image>();
    }
    
    //~
    

    /**
     * Load image from disk and then put it into the cache. If image has been loaded before then no loading is
     * needed.
     * 
     * @param card card to load image for
     * @return {@link Image}
     */
    final public static Image getImage(Card card) {
        
        /**
         * Try to get from cache.
         */
        String name = card.getImageName();
        if(card.isBasicLand()) {
            if(card.getRandomPicture() != 0) name += Integer.toString(card.getRandomPicture());
        } else if(card.isFaceDown()) name = "Morph";
        if(cache.containsKey(name)) {
            return cache.get(name);
        }
        
        /**
         * Load image.
         */
        BufferedImage image = null;
        Image resized = null;
        
        File imagePath = getImagePath(card);
        if(imagePath == null) {
            return null;
        }
        
        try {
            image = ImageIO.read(imagePath);
            
            resized = image.getScaledInstance(cardSize.width, cardSize.height, java.awt.Image.SCALE_SMOOTH);
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
        
        /**
         * Put to cache.
         */
        cache.put(name, resized);
        
        return resized;
    }
    
    /**
     * Get path to image for specific card.
     * 
     * @param c card to get path for
     * @return String if image exists, else null
     */
    final public static File getImagePath(Card c) {
        if(AllZone.NameChanger.shouldChangeCardName()) {
            return null;
        }
        
        String suffix = ".jpg";
        String filename = "";
        if(!c.isFaceDown()) {
            String basicLandSuffix = "";
            if(c.isBasicLand()) {
                if(c.getRandomPicture() != 0) basicLandSuffix = Integer.toString(c.getRandomPicture());
            }
            filename = GuiDisplayUtil.cleanString(c.getImageName()) + basicLandSuffix + suffix;
        } else filename = "morph" + suffix;
        
        String loc = "";
        if (!c.isToken())
        	loc = IMAGE_BASE;
        else
        	loc = IMAGE_TOKEN;
        
        File file = new File(ForgeProps.getFile(loc), filename);
        
        /**
         * try current directory
         */
        if(!file.exists()) {
            filename = GuiDisplayUtil.cleanString(c.getName()) + suffix;
            file = new File(filename);
        }
        
        if(file.exists()) {
            return file;
        } else {
            return null;
        }
    }
}