/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ComputationException;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.mortennobel.imagescaling.ResampleOp;

import forge.gui.GuiDisplayUtil;
import forge.item.InventoryItem;
import forge.properties.ForgePreferences.FPref;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.view.arcane.util.ImageUtil;

/**
 * This class stores ALL card images in a cache with soft values. this means
 * that the images may be collected when they are not needed any more, but will
 * be kept as long as possible.
 * <p/>
 * The keys are the following:
 * <ul>
 * <li>Keys start with the file name, extension is skipped</li>
 * <li>The key without suffix belongs to the unmodified image from the file</li>
 * <li>If the key belongs to a token, "#Token" is appended</li>
 * <li>If the key belongs to the unrotated image, "#Normal" is appended</li>
 * <li>If the key belongs to the rotated image, "#Tapped" is appended</li>
 * <li>If the key belongs to the large preview image, "#<i>scale</i>" is
 * appended, where scale is a double precision floating point number</li>
 * </ul>
 * 
 * @author Forge
 * @version $Id$
 */
public class ImageCache {
    /** Constant <code>imageCache</code>. */
    private static final LoadingCache<String, BufferedImage> IMAGE_CACHE;
    /** Constant <code>FULL_SIZE</code>. */
    private static final Pattern FULL_SIZE = Pattern.compile("(.*)#([0-9.]+)");

    private static final String NORMAL = "#Normal", TAPPED = "#Tapped";
    public static final String SEALED_PRODUCT = "sealed://";
    private static final String TOKEN = "token://";

    static {
        IMAGE_CACHE = CacheBuilder.newBuilder()
                                  .softValues()
                                  .build(new CacheLoader<String, BufferedImage>() {
            @Override
            public BufferedImage load(String key) {
                try {
                    // DEBUG
                    /*
                    System.out.printf("Currently %d %s in the cache%n",
                    IMAGE_CACHE.size(), IMAGE_CACHE.size() == 1 ?
                     "image":"images");
                    System.out.println("Contents: "+IMAGE_CACHE.toString());
                    System.out.println("Stats: " + IMAGE_CACHE.stats());
                    */
                    // DEBUG
                    // System.out.printf("New Image for key: %s%n", key);
                    if (key.endsWith(ImageCache.NORMAL)) {
                        // normal
                        key = key.substring(0, key.length() - ImageCache.NORMAL.length());
                        return ImageCache.getNormalSizeImage(ImageCache.IMAGE_CACHE.get(key));
                    } else if (key.endsWith(ImageCache.TAPPED)) {
                        // tapped
                        key = key.substring(0, key.length() - ImageCache.TAPPED.length());
                        return ImageCache.getTappedSizeImage(ImageCache.IMAGE_CACHE.get(key));
                    }
                    final Matcher m = ImageCache.FULL_SIZE.matcher(key);

                    if (m.matches()) {
                        // full size
                        key = m.group(1);
                        return ImageCache.getFullSizeImage(ImageCache.IMAGE_CACHE.get(key),
                                Double.parseDouble(m.group(2)));
                    } else {
                        // original
                        File path;
                        String filename = key;
                        if (key.startsWith(ImageCache.TOKEN)) {
                            filename = key.substring(ImageCache.TOKEN.length());
                            path = ForgeProps.getFile(NewConstants.IMAGE_TOKEN);
                        } else if (key.startsWith(SEALED_PRODUCT)) {
                            filename = key.substring(SEALED_PRODUCT.length());
                            path = ForgeProps.getFile(NewConstants.IMAGE_SEALED_PRODUCT);
                        } else {
                            path = ForgeProps.getFile(NewConstants.IMAGE_BASE);
                        }

                        File file = null;
                        final String fName = filename.endsWith(".png") || filename.endsWith(".jpg") ? filename : filename + ".jpg";
                        file = new File(path, fName);
                        if (!file.exists()) {
                            // DEBUG
                            //System.out.println("File not found, no image created: "
                            //+ file);
                            return null;
                        }
                        final BufferedImage image = ImageUtil.getImage(file);
                        IMAGE_CACHE.put(key, image);
                        return image;
                    }
                } catch (final Exception ex) {
                    // DEBUG
                    // System.err.println("Exception, no image created");
                    if (ex instanceof ComputationException) {
                        throw (ComputationException) ex;
                    } else {
                        throw new ComputationException(ex);
                    }
                }
            }
        });
    }

    /**
     * Returns the image appropriate to display the card in a zone.
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a {@link java.awt.image.BufferedImage} object.
     */
    public static BufferedImage getImage(final Card card) {
        String key = card.isFaceDown() ? "Morph" : ImageCache.getKey(card);
        if (card.isTapped()) {
            key += ImageCache.TAPPED;
        } else {
            key += ImageCache.NORMAL;
        }
        return ImageCache.getImage(key);
    }

    /**
     * Returns the image appropriate to display the card in the picture panel.
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param width
     *            a int.
     * @param height
     *            a int.
     * @return a {@link java.awt.image.BufferedImage} object.
     */
    public static BufferedImage getImage(final Card card, final int width, final int height) {
        final String key = (card.isFaceDown() && card.getController().isComputer()) ? "Morph" : ImageCache.getKey(card);
        final BufferedImage original = ImageCache.getImage(key);
        if (original == null) {
            return null;
        }

        double scale = Math.min((double) width / original.getWidth(), (double) height / original.getHeight());
        // here would be the place to limit the scaling, scaling option in menu
        // ?
        if ((scale > 1) && !Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_SCALE_LARGER)) {
            scale = 1;
        }

        return ImageCache.getImage(key + "#" + scale);
    }

    /**
     * Gets the image.
     * 
     * @param card
     *            the card
     * @param width
     *            the width
     * @param height
     *            the height
     * @return the image
     */
    public static BufferedImage getImage(final InventoryItem card, final int width, final int height) {
        final String key = card.getImageFilename();
        final BufferedImage original = ImageCache.getImage(key);
        if (original == null) {
            return null;
        }

        double scale = Math.min((double) width / original.getWidth(), (double) height / original.getHeight());
        // here would be the place to limit the scaling option in menu ?
        if ((scale > 1) && !Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_SCALE_LARGER)) {
            scale = 1;
        }

        return ImageCache.getImage(key + "#" + scale);
    }

    /**
     * <p>
     * getOriginalImage.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a {@link java.awt.image.BufferedImage} object.
     */
    public static BufferedImage getOriginalImage(final Card card) {
        final String key = (card.isFaceDown() && card.getController().isComputer()) ? "Morph" : ImageCache.getKey(card);
        return ImageCache.getImage(key);
    }

    /**
     * Returns the Image corresponding to the key.
     * 
     * @param key
     *            a {@link java.lang.String} object.
     * @return a {@link java.awt.image.BufferedImage} object.
     */
    private static BufferedImage getImage(final String key) {
        try {
            BufferedImage image = ImageCache.IMAGE_CACHE.get(key);
            // if an image is still cached and it was not the expected size,
            // drop it
            if (!ImageCache.isExpectedSize(key, image)) {
                ImageCache.IMAGE_CACHE.invalidate(key);
                image = ImageCache.IMAGE_CACHE.get(key);
            }
            return image;
        } catch (final ExecutionException ex) {
            if (ex.getCause() instanceof NullPointerException) {
                return null;
            }
            ex.printStackTrace();
            return null;
        } catch (final InvalidCacheLoadException ex) {
            // should be when a card legitimately has no image
            return null;
        } catch (final ComputationException ce) {
            if (ce.getCause() instanceof NullPointerException) {
                return null;
            }
            ce.printStackTrace();
            return null;
        } catch (final UncheckedExecutionException uee) {
            //this is for the case where "Player" shows up as a Card in GuiMultipleBlockers
            //when human attacker has Trample
            return null;
        }
    }

    /**
     * Returns if the image for the key is the proper size.
     * 
     * @param key
     *            a {@link java.lang.String} object.
     * @param image
     *            a {@link java.awt.image.BufferedImage} object.
     * @return a boolean.
     */
    private static boolean isExpectedSize(final String key, final BufferedImage image) {
        if (key.endsWith(ImageCache.NORMAL)) {
            // normal
            return (image.getWidth() == Constant.Runtime.WIDTH) && (image.getHeight() == Constant.Runtime.HEIGHT);
        } else if (key.endsWith(ImageCache.TAPPED)) {
            // tapped
            return (image.getWidth() == Constant.Runtime.HEIGHT) && (image.getHeight() == Constant.Runtime.WIDTH);
        } else {
            // original & full is never wrong
            return true;
        }
    }

    /**
     * Returns the map key for a card, without any suffixes for the image size.
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a {@link java.lang.String} object.
     */
    private static String getKey(final Card card) {

        if ((card.isToken() && !card.isCopiedToken()) || card.isFaceDown()) {
            return ImageCache.TOKEN + GuiDisplayUtil.cleanString(card.getImageName());
        }

        return card.getImageFilename(); // key;
    }

    /**
     * Returns an image scaled to the size given in {@link Constant.Runtime}.
     * 
     * @param original
     *            a {@link java.awt.image.BufferedImage} object.
     * @return a {@link java.awt.image.BufferedImage} object.
     */
    private static BufferedImage getNormalSizeImage(final BufferedImage original) {
        final int srcWidth = original.getWidth();
        final int srcHeight = original.getHeight();
        final int tgtWidth = Constant.Runtime.WIDTH;
        final int tgtHeight = Constant.Runtime.HEIGHT;

        if ((srcWidth == tgtWidth) && (srcHeight == tgtHeight)) {
            return original;
        }

        // AffineTransform at = new AffineTransform();
        // at.scale((double) tgtWidth / srcWidth, (double) tgtHeight /
        // srcHeight);
        // // at.translate(srcHeight, 0);
        // // at.rotate(PI / 2);
        // double scale = min((double) tgtWidth / srcWidth, (double) tgtHeight /
        // srcHeight);
        //
        // BufferedImage image = new BufferedImage(tgtWidth, tgtHeight,
        // BufferedImage.TYPE_INT_ARGB);
        // Graphics2D g2d = (Graphics2D) image.getGraphics();
        // g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        // RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        // g2d.drawImage(scale < 0.5? ImageUtil.getBlurredImage(original, 6,
        // 1.0f):original, at, null);
        // g2d.dispose();
        final ResampleOp resampleOp = new ResampleOp(tgtWidth, tgtHeight); // defaults
        // to
        // Lanczos3
        final BufferedImage image = resampleOp.filter(original, null);
        return image;
    }

    /**
     * Returns an image scaled to the size given in {@link Constant.Runtime},
     * but rotated.
     * 
     * @param original
     *            a {@link java.awt.image.BufferedImage} object.
     * @return a {@link java.awt.image.BufferedImage} object.
     */
    private static BufferedImage getTappedSizeImage(final BufferedImage original) {
        /*
         * int srcWidth = original.getWidth(); int srcHeight =
         * original.getHeight();
         */
        final int tgtWidth = Constant.Runtime.WIDTH;
        final int tgtHeight = Constant.Runtime.HEIGHT;

        final AffineTransform at = new AffineTransform();
        // at.scale((double) tgtWidth / srcWidth, (double) tgtHeight /
        // srcHeight);
        at.translate(tgtHeight, 0);
        at.rotate(Math.PI / 2);
        //
        // double scale = min((double) tgtWidth / srcWidth, (double) tgtHeight /
        // srcHeight);
        //
        // BufferedImage image = new BufferedImage(tgtHeight, tgtWidth,
        // BufferedImage.TYPE_INT_ARGB);
        // Graphics2D g2d = (Graphics2D) image.getGraphics();
        // g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        // RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        // g2d.drawImage(scale < 0.5? ImageUtil.getBlurredImage(original, 6,
        // 1.0f):original, at, null);
        // g2d.dispose();
        final ResampleOp resampleOp = new ResampleOp(tgtWidth, tgtHeight); // defaults
        // to
        // Lanczos3
        final BufferedImage image = resampleOp.filter(original, null);
        final BufferedImage rotatedImage = new BufferedImage(tgtHeight, tgtWidth, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = (Graphics2D) rotatedImage.getGraphics();
        g2d.drawImage(image, at, null);
        g2d.dispose();
        return rotatedImage;
    }

    /**
     * Returns an image scaled to the size appropriate for the card picture
     * panel.
     * 
     * @param original
     *            a {@link java.awt.image.BufferedImage} object.
     * @param scale
     *            a double.
     * @return a {@link java.awt.image.BufferedImage} object.
     */
    private static BufferedImage getFullSizeImage(final BufferedImage original, final double scale) {
        if (scale == 1) {
            return original;
        }

        // AffineTransform at = new AffineTransform();
        // at.scale(scale, scale);
        //
        // BufferedImage image = new BufferedImage((int) (original.getWidth() *
        // scale),
        // (int) (original.getHeight() * scale), BufferedImage.TYPE_INT_ARGB);
        // Graphics2D g2d = (Graphics2D) image.getGraphics();
        // g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        // RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        // g2d.drawImage(scale < 0.5? ImageUtil.getBlurredImage(original, 6,
        // 1.0f):original, at, null);
        // g2d.dispose();
        final ResampleOp resampleOp = new ResampleOp((int) (original.getWidth() * scale),
                (int) (original.getHeight() * scale)); // defaults to Lanczos3
        final BufferedImage image = resampleOp.filter(original, null);
        return image;
    }

    /**
     * <p>
     * clear.
     * </p>
     */
    public static void clear() {
        ImageCache.IMAGE_CACHE.invalidateAll();
    }
}
