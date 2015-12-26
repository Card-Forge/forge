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
package forge.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

import forge.ImageKeys;
import forge.game.card.CardView;
import forge.game.player.IHasIcon;
import forge.item.InventoryItem;
import forge.properties.ForgeConstants;
import forge.screens.match.MatchController;
import forge.util.ImageUtil;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

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
 * @version $Id: ImageCache.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class ImageCache {
    // short prefixes to save memory

    private static final Set<String> missingIconKeys = new HashSet<String>();
    private static final LoadingCache<String, Texture> cache = CacheBuilder.newBuilder().softValues().build(new ImageLoader());
    public static final Texture defaultImage;

    private static boolean imageLoaded, delayLoadRequested;
    public static void allowSingleLoad() {
        imageLoaded = false; //reset at the beginning of each render
        delayLoadRequested = false;
    }

    static {
        Texture defImage = null;
        try {
            defImage = new Texture(Gdx.files.absolute(ForgeConstants.NO_CARD_FILE));
        } catch (Exception ex) {
            System.err.println("could not load default card image");
        } finally {
            defaultImage = (null == defImage) ? new Texture(10, 10, Format.RGBA8888) : defImage; 
        }
    }

    public static void clear() {
        cache.invalidateAll();
        missingIconKeys.clear();
    }

    public static Texture getImage(final CardView card) {
        final String key = card.getCurrentState().getImageKey(MatchController.instance.getLocalPlayers());
        return getImage(key, true);
    }

    public static Texture getImage(InventoryItem ii) {
        return getImage(ii.getImageKey(false), true);
    }

    /**
     * retrieve an icon from the cache.  returns the current skin's ICO_UNKNOWN if the icon image is not found
     * in the cache and cannot be loaded from disk.
     */
    public static FImage getIcon(IHasIcon ihi) {
        String imageKey = ihi.getIconImageKey();
        final Texture icon;
        if (missingIconKeys.contains(imageKey) ||
                null == (icon = getImage(ihi.getIconImageKey(), false))) {
            missingIconKeys.add(imageKey);
            return FSkinImage.UNKNOWN;
        }
        return new FTextureImage(icon);
    }

    /**
     * This requests the original unscaled image from the cache for the given key.
     * If the image does not exist then it can return a default image if desired.
     * <p>
     * If the requested image is not present in the cache then it attempts to load
     * the image from file (slower) and then add it to the cache for fast future access. 
     * </p>
     */
    public static Texture getImage(String imageKey, boolean useDefaultIfNotFound) {
        if (StringUtils.isEmpty(imageKey)) { 
            return null;
        }

        boolean altState = imageKey.endsWith(ImageKeys.BACKFACE_POSTFIX);
        if (altState) {
            imageKey = imageKey.substring(0, imageKey.length() - ImageKeys.BACKFACE_POSTFIX.length());
        }
        if (imageKey.startsWith(ImageKeys.CARD_PREFIX)) {
            imageKey = ImageUtil.getImageKey(ImageUtil.getPaperCardFromImageKey(imageKey), altState, true);
            if (StringUtils.isBlank(imageKey)) { 
                return defaultImage;
            }
        }

        Texture image;
        if (useDefaultIfNotFound) {
            // Load from file and add to cache if not found in cache initially.
            image = cache.getIfPresent(imageKey);
            if (image != null) { return image; }
    
            if (imageLoaded) { //prevent loading more than one image each render for performance
                if (!delayLoadRequested) {
                    //ensure images continue to load even if no input is being received
                    delayLoadRequested = true;
                    Gdx.graphics.requestRendering();
                }
                return null;
            }
            imageLoaded = true;
        }

        try {
            image = cache.get(imageKey);
        }
        catch (final ExecutionException ex) {
            if (!(ex.getCause() instanceof NullPointerException)) {
                ex.printStackTrace();
            }
            image = null;
        }
        catch (final Exception ex) {
            image = null;
        }

        // No image file exists for the given key so optionally associate with
        // a default "not available" image and add to cache for given key.
        if (image == null) {
            if (useDefaultIfNotFound) { 
                image = defaultImage;
                cache.put(imageKey, defaultImage);
            }
        }
        return image;
    }
}
