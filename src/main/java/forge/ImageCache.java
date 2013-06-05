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
import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.cache.LoadingCache;
import com.mortennobel.imagescaling.ResampleOp;

import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.game.player.IHasIcon;
import forge.gui.toolbox.FSkin;
import forge.item.BoosterPack;
import forge.item.PaperCard;
import forge.item.PaperToken;
import forge.item.FatPack;
import forge.item.InventoryItem;
import forge.item.PreconDeck;
import forge.item.TournamentPack;
import forge.properties.ForgePreferences.FPref;
import forge.properties.NewConstants;
import forge.util.Base64Coder;

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
    // short prefixes to save memory
    public static final String TOKEN_PREFIX          = "t:";
    public static final String ICON_PREFIX           = "i:";
    public static final String BOOSTER_PREFIX        = "b:";
    public static final String FATPACK_PREFIX        = "f:";
    public static final String PRECON_PREFIX         = "p:";
    public static final String TOURNAMENTPACK_PREFIX = "o:";
    
    private static final Set<String> _missingIconKeys = new HashSet<String>();
    private static final LoadingCache<String, BufferedImage> _CACHE = CacheBuilder.newBuilder().softValues().build(new ImageLoader());
    private static final BufferedImage _defaultImage;
    static {
        BufferedImage defImage = null;
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            InputStream isNoCardJpg = cl.getResourceAsStream("no_card.jpg");
            defImage = ImageIO.read(isNoCardJpg);
        } catch (Exception e) {
            // resource not found; perhaps we're running straight from source
            try {
                defImage = ImageIO.read(new File("src/main/resources/no_card.jpg"));
            } catch (Exception ex) {
                System.err.println("could not load default card image");
            }
        } finally {
            _defaultImage = (null == defImage) ? new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB) : defImage; 
        }
    }
    
    public static void clear() {
        _CACHE.invalidateAll();
        _missingIconKeys.clear();
    }
    
    /**
     * retrieve an image from the cache.  returns null if the image is not found in the cache
     * and cannot be loaded from disk.  pass -1 for width and/or height to avoid resizing in that dimension.
     */
    public static BufferedImage getImage(Card card, int width, int height) {
        final String key;
        if (!Singletons.getControl().mayShowCard(card) || card.isFaceDown()) {
            key = TOKEN_PREFIX + NewConstants.CACHE_MORPH_IMAGE_FILE;
        } else {
            key = card.getImageKey();
        }
        return scaleImage(key, width, height, true);
    }

    /**
     * retrieve an image from the cache.  returns null if the image is not found in the cache
     * and cannot be loaded from disk.  pass -1 for width and/or height to avoid resizing in that dimension.
     */
    public static BufferedImage getImage(InventoryItem ii, int width, int height) {
        return scaleImage(getImageKey(ii, false), width, height, true);
    }
    
    /**
     * retrieve an icon from the cache.  returns the current skin's ICO_UNKNOWN if the icon image is not found
     * in the cache and cannot be loaded from disk.
     */
    public static ImageIcon getIcon(IHasIcon ihi) {
        String imageKey = ihi.getIconImageKey();
        final BufferedImage i;
        if (_missingIconKeys.contains(imageKey) ||
                null == (i = scaleImage(ihi.getIconImageKey(), -1, -1, false))) {
            _missingIconKeys.add(imageKey);
            return FSkin.getIcon(FSkin.InterfaceIcons.ICO_UNKNOWN);
        }
        return new ImageIcon(i);
    }

    private static BufferedImage scaleImage(String key, final int width, final int height, boolean useDefaultImage) {
        if (StringUtils.isEmpty(key) || (3 > width && -1 != width) || (3 > height && -1 != height)) {
            // picture too small or key not defined; return a blank
            return null;
        }

        String resizedKey = String.format("%s#%dx%d", key, width, height);

        final BufferedImage cached = _CACHE.getIfPresent(resizedKey);
        if (null != cached) {
            //System.out.println("found cached image: " + resizedKey);
            return cached;
        }
        
        BufferedImage original = getImage(key);
        if (null == original) {
            if (!useDefaultImage) {
                return null;
            }
            
            // henceforth use a default picture for this key if image not found
            original = _defaultImage;
            _CACHE.put(key, _defaultImage);
        }

        boolean mayEnlarge = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_SCALE_LARGER);
        double scale = Math.min(
                -1 == width ? 1 : (double)width / original.getWidth(),
                -1 == height? 1 : (double)height / original.getHeight());
        if ((scale > 1) && !mayEnlarge) {
            scale = 1;
        }

        BufferedImage result;
        if (1 == scale) { 
            result = original;
        } else {
            int destWidth  = (int)(original.getWidth()  * scale);
            int destHeight = (int)(original.getHeight() * scale);
            
            // if this scale has been used before, get the cached version instead of rescaling
            String effectiveResizedKey = String.format("%s#%dx%d", key, destWidth, destHeight);
            result = _CACHE.getIfPresent(effectiveResizedKey);
            if (null == result) {
                ResampleOp resampler = new ResampleOp(destWidth, destHeight);
                result = resampler.filter(original, null);
                //System.out.println("caching resized image: " + effectiveResizedKey);
                _CACHE.put(effectiveResizedKey, result);
            //} else {
            //    System.out.println("retrieved resized image: " + effectiveResizedKey);
            }
        }
        
        //System.out.println("caching image: " + resizedKey);
        _CACHE.put(resizedKey, result);
        return result;
    }

    /**
     * Returns the Image corresponding to the key.
     */
    private static BufferedImage getImage(final String key) {
        FThreads.assertExecutedByEdt(true);
        try {
            return ImageCache._CACHE.get(key);
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
    
    // Inventory items don't have to know how a certain client should draw them. 
    // That's why this method is not encapsulated and overloaded in the InventoryItem descendants
    public static String getImageKey(InventoryItem ii, boolean altState) {
        if ( ii instanceof PaperCard )
            return getImageKey((PaperCard)ii, altState, true);
        if ( ii instanceof TournamentPack )
            return ImageCache.TOURNAMENTPACK_PREFIX + ((TournamentPack)ii).getEdition();
        if ( ii instanceof BoosterPack ) {
            BoosterPack bp = (BoosterPack)ii;
            String suffix = (1 >= bp.getBoosterData().getArtIndices()) ? "" : ("_" + bp.getArtIndex());
            return ImageCache.BOOSTER_PREFIX + bp.getEdition() + suffix;
        }
        if ( ii instanceof FatPack )
            return ImageCache.FATPACK_PREFIX + ((FatPack)ii).getEdition();
        if ( ii instanceof PreconDeck )
            return ImageCache.PRECON_PREFIX + ((PreconDeck)ii).getImageFilename();
        if ( ii instanceof PaperToken ) 
            return ImageCache.TOKEN_PREFIX + ((PaperToken)ii).getImageFilename();
        return null;
    }

    private static String getImageLocator(PaperCard cp, boolean backFace, boolean includeSet, boolean isDownloadUrl) {
        final String nameToUse = getNameToUse(cp, backFace);
        if ( null == nameToUse )
            return null;
        
        StringBuilder s = new StringBuilder();
        
        CardRules card = cp.getRules();
        String edition = cp.getEdition();
        s.append(ImageCache.toMWSFilename(nameToUse));
        
        final int cntPictures;
        final boolean hasManyPictures;
        if (includeSet) {
            cntPictures = card.getEditionInfo(edition).getCopiesCount();
            hasManyPictures = cntPictures > 1;
        } else {
            // without set number of pictures equals number of urls provided in Svar:Picture
            String urls = card.getPictureUrl(backFace);
            cntPictures = StringUtils.countMatches(urls, "\\") + 1;

            // raise the art index limit to the maximum of the sets this card was printed in
            int maxCntPictures = 1;
            for (String set : card.getSets()) {
                maxCntPictures = Math.max(maxCntPictures, card.getEditionInfo(set).getCopiesCount());
            }
            hasManyPictures = maxCntPictures > 1;
        }
        
        int artIdx = cp.getArtIndex();
        if (hasManyPictures) {
            if ( cntPictures <= artIdx ) // prevent overflow
                artIdx = cntPictures == 1 ? 0 : artIdx % cntPictures;
            s.append(artIdx + 1);
        }
        
        // for whatever reason, MWS-named plane cards don't have the ".full" infix
        if (!card.getType().isPlane() && !card.getType().isPhenomenon()) {
            s.append(".full");
        }
        
        final String fname;
        if (isDownloadUrl) {
            s.append(".jpg");
            fname = Base64Coder.encodeString(s.toString(), true);
        } else {
            fname = s.toString();
        }
        
        if (includeSet) {
            String editionAliased = isDownloadUrl ? Singletons.getModel().getEditions().getCode2ByCode(edition) : getSetFolder(edition);
            return String.format("%s/%s", editionAliased, fname);
        } else {
            return fname;
        }
    }
    
    public static boolean hasBackFacePicture(PaperCard cp) {
        CardSplitType cst = cp.getRules().getSplitType();
        return cst == CardSplitType.Transform || cst == CardSplitType.Flip; 
    }
    
    public static String getSetFolder(String edition) {
        return  !NewConstants.CACHE_CARD_PICS_SUBDIR.containsKey(edition)
                ? Singletons.getModel().getEditions().getCode2ByCode(edition) // by default 2-letter codes from MWS are used
                : NewConstants.CACHE_CARD_PICS_SUBDIR.get(edition); // may use custom paths though
    }

    private static String getNameToUse(PaperCard cp, boolean backFace) {
        final CardRules card = cp.getRules();
        if (backFace ) {
            if ( hasBackFacePicture(cp) ) 
                return card.getOtherPart().getName();
            else 
                return null;
        } else if(CardSplitType.Split == cp.getRules().getSplitType()) {
            return card.getMainPart().getName() + card.getOtherPart().getName();
        } else {
            return cp.getName();
        }
    }
    
    public static String getImageKey(PaperCard cp, boolean backFace, boolean includeSet) {
        return getImageLocator(cp, backFace, includeSet, false);
    }

    public static String getDownloadUrl(PaperCard cp, boolean backFace) {
        return getImageLocator(cp, backFace, true, true);
    }    
    
    public static String toMWSFilename(String in) {
        final StringBuffer out = new StringBuffer();
        char c;
        for (int i = 0; i < in.length(); i++) {
            c = in.charAt(i);
            if ((c == '"') || (c == '/') || (c == ':') || (c == '?')) {
                out.append("");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
