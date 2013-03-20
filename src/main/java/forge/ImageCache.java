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
import java.io.IOException;
import java.io.InputStream;
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
import forge.item.CardPrinted;
import forge.item.CardToken;
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
    
    static private final LoadingCache<String, BufferedImage> CACHE = CacheBuilder.newBuilder().softValues().build(new ImageLoader());
    private static final BufferedImage emptyImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB); 
    private static BufferedImage defaultImage = emptyImage;
    static { 
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            InputStream isNoCardJpg = cl.getResourceAsStream("no_card.jpg");
            defaultImage = ImageIO.read(isNoCardJpg);
        } catch (IOException e) {
            // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
            e.printStackTrace();
        }
    }
    /**
     * retrieve an image from the cache.  returns null if the image is not found in the cache
     * and cannot be loaded from disk.  pass -1 for width and/or height to avoid resizing in that dimension.
     */
    public static BufferedImage getImage(Card card, int width, int height) {
        final String key;
        if (!card.canBeShownTo(Singletons.getControl().getPlayer()) || card.isFaceDown()) {
            key = TOKEN_PREFIX + NewConstants.CACHE_MORPH_IMAGE_FILE;
        } else {
            key = card.getImageKey();
        }
        return scaleImage(key, width, height);
    }

    /**
     * retrieve an image from the cache.  returns null if the image is not found in the cache
     * and cannot be loaded from disk.  pass -1 for width and/or height to avoid resizing in that dimension.
     */
    public static BufferedImage getImage(InventoryItem ii, int width, int height) {
        return scaleImage(getImageKey(ii, false), width, height);
    }
    
    /**
     * retrieve an icon from the cache.  returns the current skin's ICO_UNKNOWN if the icon image is not found
     * in the cache and cannot be loaded from disk.
     */
    public static ImageIcon getIcon(IHasIcon ihi) {
        BufferedImage i = scaleImage(ihi.getIconImageKey(), -1, -1);
        if (null == i) {
            return FSkin.getIcon(FSkin.InterfaceIcons.ICO_UNKNOWN);
        }
        return new ImageIcon(i);
    }

    private static BufferedImage scaleImage(String key, final int width, final int height) {
        if (StringUtils.isEmpty(key) || (3 > width && -1 != width) || (3 > height && -1 != height)) {
            // picture too small or key not defined; return a blank
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

        if (null == original) {
            original = defaultImage;
            CACHE.put(key, defaultImage); // This instructs cache to give up finding a picture if it was not found once
        }

        if (original == emptyImage) { // the found image is a placeholder for missing picture? 
            return null;
        }

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
            ResampleOp resampler = new ResampleOp(destWidth, destHeight);
            
            result = resampler.filter(original, null);
            CACHE.put(resizedKey, result);
        }
        
        return result;
    }

    /**
     * Returns the Image corresponding to the key.
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
    
    // Inventory items don't have to know how a certain client should draw them. 
    // That's why this method is not encapsulated and overloaded in the InventoryItem descendants
    public static String getImageKey(InventoryItem ii, boolean altState) {
        if ( ii instanceof CardPrinted )
            return getImageKey((CardPrinted)ii, altState, true);
        if ( ii instanceof TournamentPack )
            return ImageCache.TOURNAMENTPACK_PREFIX + ((TournamentPack)ii).getEdition();
        if ( ii instanceof BoosterPack )
            return ImageCache.BOOSTER_PREFIX + ((BoosterPack)ii).getEdition();
        if ( ii instanceof FatPack )
            return ImageCache.FATPACK_PREFIX + ((FatPack)ii).getEdition();
        if ( ii instanceof PreconDeck )
            return ImageCache.PRECON_PREFIX + ((PreconDeck)ii).getImageFilename();
        if ( ii instanceof CardToken ) 
            return ImageCache.TOKEN_PREFIX + ((CardToken)ii).getImageFilename();
        return null;
    }

    private static String getImageLocator(CardPrinted cp, boolean backFace, boolean includeSet, boolean isDownloadUrl) {
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
    
    public static boolean hasBackFacePicture(CardPrinted cp) {
        return cp.getRules().getSplitType() == CardSplitType.Transform; // do we take other image for flipped cards? 
    }
    
    public static String getSetFolder(String edition) {
        return  !NewConstants.CACHE_CARD_PICS_SUBDIR.containsKey(edition)
                ? Singletons.getModel().getEditions().getCode2ByCode(edition) // by default 2-letter codes from MWS are used
                : NewConstants.CACHE_CARD_PICS_SUBDIR.get(edition); // may use custom paths though
    }

    private static String getNameToUse(CardPrinted cp, boolean backFace) {
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
    
    public static String getImageKey(CardPrinted cp, boolean backFace, boolean includeSet) {
        return getImageLocator(cp, backFace, includeSet, false);
    }

    public static String getDownloadUrl(CardPrinted cp, boolean backFace) {
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
