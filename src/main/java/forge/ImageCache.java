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

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutionException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.cache.LoadingCache;
import com.mortennobel.imagescaling.ResampleOp;

import forge.gui.GuiDisplayUtil;
import forge.item.InventoryItem;
import forge.properties.ForgePreferences.FPref;

/**
 * This class stores ALL card images in a cache with soft values. this means
 * that the images may be collected when they are not needed any more, but will
 * be kept as long as possible.
 * <p/>
 * The keys are the following:
 * <ul>
 * <li>Keys start with the file name, extension is skipped</li>
 * <li>The key without suffix belongs to the unmodified image from the file</li>
 * </ul>
 * 
 * @author Forge
 * @version $Id$
 */
public class ImageCache {
    /** Constant <code>imageCache</code>. */
    static final LoadingCache<String, BufferedImage> CACHE = CacheBuilder.newBuilder().softValues().build(new ImageLoader());
    /** Constant <code>FULL_SIZE</code>. */

    public static final String SEALED_PRODUCT = "sealed://";
    public static final String TOKEN = "token://";

    public static BufferedImage getImage(final Card card, final int width, final int height) {
        //SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS"); 
        //System.out.printf("%s - load '%s' (%d x %d)\n", sdf.format(new Date()), card.getImageFilename(), width, height );

        final String key = card.canBeShownTo(Singletons.getControl().getPlayer()) ? ImageCache.getKey(card) : "Morph";
        return scaleImage(key, width, height);
    }


    public static BufferedImage getImage(final InventoryItem card, final int width, final int height) {
        // TODO: move all the path-building logics here from the very objects. They don't have to know where their picture is
        String key = card.getImageFilename();
        
        return scaleImage(key, width, height);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param original
     * @param scale
     * @return
     */
    private static BufferedImage scaleImage(String key, final int width, final int height) {
        if (3 > width || 3 > height) {
            // picture too small; return a blank
            return null;
        }

        StringBuilder rsKey = new StringBuilder(key);
        rsKey.append("#").append(width).append('x').append(height);
        String resizedKey = rsKey.toString();

        final BufferedImage cached = CACHE.getIfPresent(resizedKey);
        if (null != cached) {
            return cached;
        }
        
        boolean mayEnlarge = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_SCALE_LARGER);
        BufferedImage original = getImage(key);
        if ( null == original )
            return null;
        
        double scale = Math.min((double) width / original.getWidth(), (double) height / original.getHeight());
        // here would be the place to limit the scaling option in menu ?
        if ((scale > 1) && !mayEnlarge) {
            scale = 1;
        }

        BufferedImage result;
        if ( 1 == scale ) { 
            result = original;
        } else {
            int destWidth = (int) (original.getWidth() * scale);
            int destHeight = (int) (original.getHeight() * scale);
            ResampleOp resampler = new ResampleOp(destWidth, destHeight);
            
            result = resampler.filter(original, null);
            CACHE.put(resizedKey, result);
        }
        return result;
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
            return ImageCache.CACHE.get(key);
        } catch (final ExecutionException ex) {
            if (ex.getCause() instanceof NullPointerException) {
                return null;
            }
            ex.printStackTrace();
            return null;
        } catch (final InvalidCacheLoadException ex) {
            // should be when a card legitimately has no image
            return null;
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

}
