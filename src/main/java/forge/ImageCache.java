package forge;


import arcane.ui.util.ImageUtil;
import com.google.common.base.Function;
import com.google.common.collect.ComputationException;
import com.google.common.collect.MapMaker;
import com.mortennobel.imagescaling.ResampleOp;

import forge.item.InventoryItem;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Double.parseDouble;
import static java.lang.Math.min;


/**
 * This class stores ALL card images in a cache with soft values. this means that the images may be collected when
 * they are not needed any more, but will be kept as long as possible.
 * <p/>
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
 *
 * @author Forge
 * @version $Id$
 */
public class ImageCache implements NewConstants {
    /** Constant <code>imageCache</code> */
    private static final Map<String, BufferedImage> imageCache;
    /** Constant <code>FULL_SIZE</code> */
    private static final Pattern FULL_SIZE = Pattern.compile("(.*)#(\\d+.\\d+)");
    /** Constant <code>TOKEN="#Token"</code> */
    /** Constant <code>NORMAL="#Normal"</code> */
    /** Constant <code>TAPPED="#Tapped"</code> */
    /** Constant <code>NORMAL="#Normal"</code> */
    /** Constant <code>TAPPED="#Tapped"</code> */
    private static final String TOKEN = "#Token", NORMAL = "#Normal", TAPPED = "#Tapped";

    /** Constant <code>scaleLargerThanOriginal=true</code> */
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
                    if (key.endsWith(NORMAL)) {
                        //normal
                        key = key.substring(0, key.length() - NORMAL.length());
                        return getNormalSizeImage(imageCache.get(key));
                    } else if (key.endsWith(TAPPED)) {
                        //tapped
                        key = key.substring(0, key.length() - TAPPED.length());
                        return getTappedSizeImage(imageCache.get(key));
                    }
                    Matcher m = FULL_SIZE.matcher(key);

                    if (m.matches()) {
                        //full size
                        key = m.group(1);
                        return getFullSizeImage(imageCache.get(key), parseDouble(m.group(2)));
                    } else {
                        //original
                        File path;
                        if (key.endsWith(TOKEN)) {
                            key = key.substring(0, key.length() - TOKEN.length());
                            path = ForgeProps.getFile(IMAGE_TOKEN);
                        } else path = ForgeProps.getFile(IMAGE_BASE);

                        File file = null;
                        file = new File(path, key + ".jpg");
                        if (!file.exists()) {
                            //DEBUG
                            //System.out.println("File not found, no image created: " + file);
                            return null;
                        }
                        BufferedImage image = ImageUtil.getImage(file);
                        return image;
                    }
                } catch (Exception ex) {
                    //DEBUG
                    //System.err.println("Exception, no image created");
                    if (ex instanceof ComputationException) throw (ComputationException) ex;
                    else throw new ComputationException(ex);
                }
            }
        });
    }

    /**
     * Returns the image appropriate to display the card in a zone
     *
     * @param card a {@link forge.Card} object.
     * @return a {@link java.awt.image.BufferedImage} object.
     */
    public static BufferedImage getImage(Card card) {
        String key = card.isFaceDown() ? "Morph" : getKey(card);
        if (card.isTapped()) key += TAPPED;
        else key += NORMAL;
        return getImage(key);
    }

    /**
     * Returns the image appropriate to display the card in the picture panel
     *
     * @param card a {@link forge.Card} object.
     * @param width a int.
     * @param height a int.
     * @return a {@link java.awt.image.BufferedImage} object.
     */
    public static BufferedImage getImage(Card card, int width, int height) {
        String key = (card.isFaceDown() && card.getController().isComputer()) ? "Morph" : getKey(card);
        BufferedImage original = getImage(key);
        if (original == null) return null;

        double scale = min((double) width / original.getWidth(), (double) height / original.getHeight());
        //here would be the place to limit the scaling, scaling option in menu ?
        if (scale > 1 && !scaleLargerThanOriginal) scale = 1;

        return getImage(key + "#" + scale);
    }

    public static BufferedImage getImage(InventoryItem card, int width, int height) {
        String key = card.getImageFilename();
        BufferedImage original = getImage(key);
        if (original == null) return null;

        double scale = min((double) width / original.getWidth(), (double) height / original.getHeight());
        //here would be the place to limit the scaling, scaling option in menu ?
        if (scale > 1 && !scaleLargerThanOriginal) scale = 1;

        return getImage(key + "#" + scale);
    }
    
    
    /**
     * <p>getOriginalImage.</p>
     *
     * @param card a {@link forge.Card} object.
     * @return a {@link java.awt.image.BufferedImage} object.
     */
    public static BufferedImage getOriginalImage(Card card) {
        String key = (card.isFaceDown() && card.getController().isComputer()) ? "Morph" : getKey(card);
        return getImage(key);
    }

    /**
     * Returns the Image corresponding to the key
     *
     * @param key a {@link java.lang.String} object.
     * @return a {@link java.awt.image.BufferedImage} object.
     */
    private static BufferedImage getImage(String key) {
        try {
            BufferedImage image = imageCache.get(key);
            //if an image is still cached and it was not the expected size, drop it
            if (!isExpectedSize(key, image)) {
                imageCache.remove(key);
                image = imageCache.get(key);
            }
            return image;
        } catch (NullPointerException ex) {
            //unfortunately NullOutputException, thrown when apply() returns null, is not public
            //NullOutputException is a subclass of NullPointerException
            //legitimate, happens when a card has no image
            return null;
        } catch (ComputationException ex) {
            if (ex.getCause() instanceof NullPointerException) return null;
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Returns if the image for the key is the proper size.
     *
     * @param key a {@link java.lang.String} object.
     * @param image a {@link java.awt.image.BufferedImage} object.
     * @return a boolean.
     */
    private static boolean isExpectedSize(String key, BufferedImage image) {
        if (key.endsWith(NORMAL)) {
            //normal
            return image.getWidth() == Constant.Runtime.width[0]
                    && image.getHeight() == Constant.Runtime.height[0];
        } else if (key.endsWith(TAPPED)) {
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
     *
     * @param card a {@link forge.Card} object.
     * @return a {@link java.lang.String} object.
     */
    private static String getKey(Card card) {

        if (card.isToken() && !card.isCopiedToken())
            return GuiDisplayUtil.cleanString(card.getImageName()) + TOKEN;

        return card.getImageFilename(); //key;
    }

    /**
     * Returns an image scaled to the size given in {@link Constant.Runtime}
     *
     * @param original a {@link java.awt.image.BufferedImage} object.
     * @return a {@link java.awt.image.BufferedImage} object.
     */
    private static BufferedImage getNormalSizeImage(BufferedImage original) {
        int srcWidth = original.getWidth();
        int srcHeight = original.getHeight();
        int tgtWidth = Constant.Runtime.width[0];
        int tgtHeight = Constant.Runtime.height[0];

        if (srcWidth == tgtWidth && srcHeight == tgtHeight) return original;


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
     *
     * @param original a {@link java.awt.image.BufferedImage} object.
     * @return a {@link java.awt.image.BufferedImage} object.
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
     *
     * @param original a {@link java.awt.image.BufferedImage} object.
     * @param scale a double.
     * @return a {@link java.awt.image.BufferedImage} object.
     */
    private static BufferedImage getFullSizeImage(BufferedImage original, double scale) {
        if (scale == 1) return original;

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


    /**
     * <p>clear.</p>
     */
    public static void clear() {
        imageCache.clear();
    }
}
