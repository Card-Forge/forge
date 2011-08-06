
package forge;


import static java.lang.Double.*;
import static java.lang.Math.*;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import arcane.ui.util.ImageUtil;

import com.google.common.base.Function;
import com.google.common.collect.ComputationException;
import com.google.common.collect.MapMaker;
import com.mortennobel.imagescaling.ResampleOp;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;


/**
 * This class stores ALL card images in a cache with soft values. this means that the images may be collected when
 * they are not needed any more, but will be kept as long as possible.
 * 
 * The keys are the following:
 * <ul>
 * <li>Keys start with the file name, extension is skipped</li>
 * <li>The key without suffix belongs to the unmodified image from the file</li>
 * <li>If the key belongs to a token, "#Token" is appended</li>
 * <li>If the key belongs to the unrotated image, "#Normal" is appended</li>
 * <li>If the key belongs to the rotated image, "#Tapped" is appended</li>
 * <li>If the key belongs to the large preview image, "#<i>scale</i>" is appended, where scale is a double
 * precision floating point number</li>
 * </ul>
 */
public class ImageCache implements NewConstants {
    private static final Map<String, BufferedImage> imageCache;
    private static final Pattern                    FULL_SIZE = Pattern.compile("(.*)#(\\d+.\\d+)");
    private static final String                     TOKEN     = "#Token", NORMAL = "#Normal", TAPPED = "#Tapped";
    
    public static boolean scaleLargerThanOriginal = true;
    
    static {
        imageCache = new MapMaker().softValues().makeComputingMap(new Function<String, BufferedImage>() {
            public BufferedImage apply(String key) {
                try {
                    //DEBUG
                    /*System.out.printf("Currently %d %s in the cache%n", imageCache.size(),
                            imageCache.size() == 1? "image":"images");*/
                	//DEBUG
                    //System.out.printf("New Image for key: %s%n", key);
                    if(key.endsWith(NORMAL)) {
                        //normal
                        key = key.substring(0, key.length() - NORMAL.length());
                        return getNormalSizeImage(imageCache.get(key));
                    } else if(key.endsWith(TAPPED)) {
                        //tapped
                        key = key.substring(0, key.length() - TAPPED.length());
                        return getTappedSizeImage(imageCache.get(key));
                    }
                    Matcher m = FULL_SIZE.matcher(key);
                    
                    if(m.matches()) {
                        //full size
                        key = m.group(1);
                        return getFullSizeImage(imageCache.get(key), parseDouble(m.group(2)));
                    } else {
                        //original
                        File path;
                        if(key.endsWith(TOKEN)) {
                            key = key.substring(0, key.length() - TOKEN.length());
                            path = ForgeProps.getFile(IMAGE_TOKEN);
                        } else path = ForgeProps.getFile(IMAGE_BASE);
                        
                        File file = null;
                        file = new File(path, key + ".jpg");
                        if(!file.exists()) {
                        	//DEBUG
                            //System.out.println("File not found, no image created: " + file);
                            return null;
                        }
                        BufferedImage image = ImageUtil.getImage(file);
                        return image;
                    }
                } catch(Exception ex) {
                	//DEBUG
                    //System.err.println("Exception, no image created");
                    if(ex instanceof ComputationException) throw (ComputationException) ex;
                    else throw new ComputationException(ex);
                }
            }
        });
    }
    
    /**
     * Returns the image appropriate to display the card in a zone
     */
    public static BufferedImage getImage(Card card) {
        String key = card.isFaceDown()? "Morph":getKey(card);
        if(card.isTapped()) key += TAPPED;
        else key += NORMAL;
        return getImage(key);
    }
    
    /**
     * Returns the image appropriate to display the card in the picture panel
     */
    public static BufferedImage getImage(Card card, int width, int height) {
        String key = (card.isFaceDown() && card.getController() == AllZone.ComputerPlayer)? "Morph":getKey(card);
        BufferedImage original = getImage(key);
        if(original == null) return null;
        
        double scale = min((double) width / original.getWidth(), (double) height / original.getHeight());
        //here would be the place to limit the scaling, scaling option in menu ?
        if(scale > 1 && !scaleLargerThanOriginal) scale = 1;
        
        return getImage(key + "#" + scale);
    }
    
    public static BufferedImage getOriginalImage(Card card) {
        String key = (card.isFaceDown() && card.getController() == AllZone.ComputerPlayer)? "Morph":getKey(card);
        return getImage(key);
    }
    
    /**
     * Returns the Image corresponding to the key
     */
    private static BufferedImage getImage(String key) {
        try {
            BufferedImage image = imageCache.get(key);
            //if an image is still cached and it was not the expected size, drop it
            if(!isExpectedSize(key, image)) {
                imageCache.remove(key);
                image = imageCache.get(key);
            }
            return image;
        } catch(NullPointerException ex) {
            //unfortunately NullOutputException, thrown when apply() returns null, is not public
            //NullOutputException is a subclass of NullPointerException
            //legitimate, happens when a card has no image
            return null;
        } catch(ComputationException ex) {
            if(ex.getCause() instanceof NullPointerException) return null;
            ex.printStackTrace();
            return null;
        }
    }
    
    /**
     * Returns if the image for the key is the proper size.
     */
    private static boolean isExpectedSize(String key, BufferedImage image) {
        if(key.endsWith(NORMAL)) {
            //normal
            return image.getWidth() == Constant.Runtime.width[0]
                    && image.getHeight() == Constant.Runtime.height[0];
        } else if(key.endsWith(TAPPED)) {
            //tapped
            return image.getWidth() == Constant.Runtime.height[0]
                    && image.getHeight() == Constant.Runtime.width[0];
        } else {
            //original & full is never wrong
            return true;
        }
    }
    
    /**
     * Returns the map key for a card, without any suffixes for the image size.
     */
    private static String getKey(Card card) {
/*        String key = GuiDisplayUtil.cleanString(card.getImageName());
        //if(card.isBasicLand() && card.getRandomPicture() != 0) key += card.getRandomPicture();
        File path = null;
        String tkn = "";
        if (card.isToken() && !card.isCopiedToken())
        {
        	path = ForgeProps.getFile(IMAGE_TOKEN);
        	tkn = TOKEN;
        }
        else
        	path = ForgeProps.getFile(IMAGE_BASE);
        
        File  f = null;
        if (!card.getCurSetCode().equals(""))
        {
            String nn = "";
        	if (card.getRandomPicture() > 0)
                nn = Integer.toString(card.getRandomPicture() + 1);
        	
        	StringBuilder sbKey = new StringBuilder();
        	
        	//First try 3 letter set code with MWS filename format 
        	sbKey.append(card.getCurSetCode() + "/");
        	sbKey.append(GuiDisplayUtil.cleanStringMWS(card.getName()) + nn + ".full");
        	
        	f = new File(path, sbKey.toString() + ".jpg");
        	if (f.exists())
        		return sbKey.toString();
        	
        	sbKey = new StringBuilder();
        	
        	//Second, try 2 letter set code with MWS filename format
        	sbKey.append(SetInfoUtil.getSetCode2_SetCode3(card.getCurSetCode()) + "/");
        	sbKey.append(GuiDisplayUtil.cleanStringMWS(card.getName()) + nn + ".full");
        	
        	f = new File(path, sbKey.toString() + ".jpg");
        	if (f.exists())
        		return sbKey.toString();
        	
        	sbKey = new StringBuilder();
        	
        	//Third, try 3 letter set code with Forge filename format
        	sbKey.append(card.getCurSetCode() + "/");
        	sbKey.append(GuiDisplayUtil.cleanString(card.getName()) + nn);
        	
        	f = new File(path, sbKey.toString() + ".jpg");
        	if (f.exists())
        		return sbKey.toString();
        	
        	//Last, give up with set images, go with the old picture type
        	f = new File(path, key + nn + ".jpg");
        	if (f.exists())
        		return key;
        	
        	//if still no file, download if option enabled
        }
        
        int n = card.getRandomPicture();
        if (n > 0)
        	key += n;
        
        key += tkn;
//        key = GuiDisplayUtil.cleanString(key);
*/        
    	if (card.isToken() && !card.isCopiedToken())
    		return GuiDisplayUtil.cleanString(card.getImageName()) + TOKEN;

    	return card.getImageFilename(); //key;
    }
    
    /**
     * Returns an image scaled to the size given in {@link Constant.Runtime}
     */
    private static BufferedImage getNormalSizeImage(BufferedImage original) {
        int srcWidth = original.getWidth();
        int srcHeight = original.getHeight();
        int tgtWidth = Constant.Runtime.width[0];
        int tgtHeight = Constant.Runtime.height[0];
        
        if(srcWidth == tgtWidth && srcHeight == tgtHeight) return original;
        

//        AffineTransform at = new AffineTransform();
//        at.scale((double) tgtWidth / srcWidth, (double) tgtHeight / srcHeight);
////        at.translate(srcHeight, 0);
////        at.rotate(PI / 2);
//        double scale = min((double) tgtWidth / srcWidth, (double) tgtHeight / srcHeight);
//        
//        BufferedImage image = new BufferedImage(tgtWidth, tgtHeight, BufferedImage.TYPE_INT_ARGB);
//        Graphics2D g2d = (Graphics2D) image.getGraphics();
//        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//        g2d.drawImage(scale < 0.5? ImageUtil.getBlurredImage(original, 6, 1.0f):original, at, null);
//        g2d.dispose();
        ResampleOp resampleOp = new ResampleOp(tgtWidth, tgtHeight); //defaults to Lanczos3
        BufferedImage image = resampleOp.filter(original, null);
        return image;
    }
    
    /**
     * Returns an image scaled to the size given in {@link Constant.Runtime}, but rotated
     */
    private static BufferedImage getTappedSizeImage(BufferedImage original) {
        /*int srcWidth = original.getWidth();
        int srcHeight = original.getHeight();*/
        int tgtWidth = Constant.Runtime.width[0];
        int tgtHeight = Constant.Runtime.height[0];
        
        AffineTransform at = new AffineTransform();
//        at.scale((double) tgtWidth / srcWidth, (double) tgtHeight / srcHeight);
        at.translate(tgtHeight, 0);
        at.rotate(Math.PI / 2);
//        
//        double scale = min((double) tgtWidth / srcWidth, (double) tgtHeight / srcHeight);
//        
//        BufferedImage image = new BufferedImage(tgtHeight, tgtWidth, BufferedImage.TYPE_INT_ARGB);
//        Graphics2D g2d = (Graphics2D) image.getGraphics();
//        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//        g2d.drawImage(scale < 0.5? ImageUtil.getBlurredImage(original, 6, 1.0f):original, at, null);
//        g2d.dispose();
        ResampleOp resampleOp = new ResampleOp(tgtWidth, tgtHeight); //defaults to Lanczos3
        BufferedImage image = resampleOp.filter(original, null);
        BufferedImage rotatedImage = new BufferedImage(tgtHeight, tgtWidth, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) rotatedImage.getGraphics();
        g2d.drawImage(image, at, null);
        g2d.dispose();
        return rotatedImage;
    }
    
    /**
     * Returns an image scaled to the size appropriate for the card picture panel
     */
    private static BufferedImage getFullSizeImage(BufferedImage original, double scale) {
        if(scale == 1) return original;
        
//        AffineTransform at = new AffineTransform();
//        at.scale(scale, scale);
//        
//        BufferedImage image = new BufferedImage((int) (original.getWidth() * scale),
//                (int) (original.getHeight() * scale), BufferedImage.TYPE_INT_ARGB);
//        Graphics2D g2d = (Graphics2D) image.getGraphics();
//        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//        g2d.drawImage(scale < 0.5? ImageUtil.getBlurredImage(original, 6, 1.0f):original, at, null);
//        g2d.dispose();
        ResampleOp resampleOp = new ResampleOp((int) (original.getWidth() * scale),
                (int) (original.getHeight() * scale)); //defaults to Lanczos3
        BufferedImage image = resampleOp.filter(original, null);
        return image;
    }
}
