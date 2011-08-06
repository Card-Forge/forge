
package forge;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;

import com.google.common.base.Function;
import com.google.common.collect.ComputationException;
import com.google.common.collect.MapMaker;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;


/**
 * This class stores ALL card images in a cache with soft values. this means that the images may be collected when
 * they are not needed any more, but will be kept as long as possible.
 * 
 * The keys are the following:
 * <ul>
 * <li>Keys start with the file name, extension is skipped</li>
 * <li>If the key belongs to a token, "#Token" is appended</li>
 * <li>If the key belongs to the unrotated image, "#Normal" is appended</li>
 * <li>If the key belongs to the rotated image, "#Tapped" is appended</li>
 * <li>If the key belongs to the large preview image, "#Full" is appended</li>
 * <li>The key without suffix belongs to the unmodified image from the file</li>
 * </ul>
 */
public class ImageCache2 implements NewConstants {
    public static void main(String[] args) {
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        jf.setLayout(new GridLayout(1, 0));
        JPanel[] panels = {new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.fillRect(0, 0, 50, 80);
            }
        }, new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.scale(.5, .5);
//                g2d.translate(80, 0);
//                g2d.rotate(PI / 2);
                g2d.fillRect(0, 0, 50, 80);
                g2d.dispose();
            }
        }};
        
        Border b = BorderFactory.createLineBorder(Color.BLACK);
        for(JPanel p:panels) {
            p.setBorder(b);
            jf.add(p);
        }
        
        jf.pack();
        jf.setVisible(true);
    }
    
    private static final Map<String, BufferedImage> imageCache;
    
    private static final String                     TOKEN = "#Token", NORMAL = "#Normal", TAPPED = "#Tapped",
            FULL = "#Full";
    
    static {
        imageCache = new MapMaker().softValues().makeComputingMap(new Function<String, BufferedImage>() {
            public BufferedImage apply(String key) {
                if(key.endsWith(NORMAL)) {
                    //normal
                    key = key.substring(0, key.length() - NORMAL.length());
                    return getNormalSizeImage(imageCache.get(key));
                } else if(key.endsWith(TAPPED)) {
                    //tapped
                    key = key.substring(0, key.length() - TAPPED.length());
                    return getTappedSizeImage(imageCache.get(key));
                } else if(key.endsWith(FULL)) {
                    //full size
                    key = key.substring(0, key.length() - FULL.length());
                    return getFullSizeImage(imageCache.get(key));
                } else {
                    //original
                    File path;
                    if(key.endsWith(TOKEN)) {
                        key = key.substring(0, key.length() - TOKEN.length());
                        path = ForgeProps.getFile(IMAGE_TOKEN);
                    } else path = ForgeProps.getFile(IMAGE_BASE);
                    File file = new File(path, key + ".jpg");
                    
                    try {
                        BufferedImage image = ImageIO.read(file);
                        return image;
                    } catch(IOException ex) {
                        throw new ComputationException(ex);
                    }
                }
            }
        });
    }
    
    /**
     * Returns the image appropriate to display the card in a zone
     */
    public static BufferedImage getImage(Card card) {
        String key = getKey(card);
        if(card.isTapped()) key += TAPPED;
        else key += NORMAL;
        return getImage(key);
    }
    
    /**
     * Returns the image appropriate to display the card in the picture panel
     */
    public static BufferedImage getFullImage(Card card) {
        String key = getKey(card) + FULL;
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
        } catch(ComputationException ex) {
            //legitimate, happens when a card has no image
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
            return image.getHeight() == Constant.Runtime.width[0]
                    && image.getWidth() == Constant.Runtime.height[0];
        } else if(key.endsWith(FULL)) {
            //full size
            //TODO what's good in this case?
            return true;
        } else {
            //original is never wrong
            return true;
        }
    }
    
    /**
     * Returns the map key for a card, without any suffixes for the image size
     */
    private static String getKey(Card card) {
        if(card.isFaceDown()) return "Morph";
        String key = card.getImageName();
        if(card.isBasicLand() && card.getRandomPicture() != 0) key += card.getRandomPicture();
        if(card.isToken()) key += TOKEN;
        return GuiDisplayUtil.cleanString(key);
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
        

        AffineTransform at = new AffineTransform();
        at.scale((double) tgtWidth / srcWidth, (double) tgtHeight / srcHeight);
//        at.translate(srcHeight, 0);
//        at.rotate(PI / 2);
        

        BufferedImage image = new BufferedImage(tgtWidth, tgtHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.drawImage(original, at, null);
        g2d.dispose();
        return image;
    }
    
    /**
     * Returns an image scaled to the size given in {@link Constant.Runtime}, but rotated
     */
    private static BufferedImage getTappedSizeImage(BufferedImage original) {
        int srcWidth = original.getWidth();
        int srcHeight = original.getHeight();
        int tgtWidth = Constant.Runtime.height[0];
        int tgtHeight = Constant.Runtime.width[0];
        
        AffineTransform at = new AffineTransform();
        at.scale((double) tgtWidth / srcWidth, (double) tgtHeight / srcHeight);
        at.translate(srcHeight, 0);
        at.rotate(Math.PI / 2);
        

        BufferedImage image = new BufferedImage(tgtWidth, tgtHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.drawImage(original, at, null);
        g2d.dispose();
        return image;
    }
    
    /**
     * Returns an image scaled to the size appropriate for the card picture panel
     */
    private static BufferedImage getFullSizeImage(BufferedImage original) {
        int srcWidth = original.getWidth();
        int srcHeight = original.getHeight();
        //TODO size for the picture panel?
        int tgtWidth = 0;
        int tgtHeight = 0;
        
        if(srcWidth == tgtWidth && srcHeight == tgtHeight) return original;
        

        AffineTransform at = new AffineTransform();
        at.scale((double) tgtWidth / srcWidth, (double) tgtHeight / srcHeight);
        

        BufferedImage image = new BufferedImage(tgtWidth, tgtHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.drawImage(original, at, null);
        g2d.dispose();
        return image;
    }
}
