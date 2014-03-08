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
import com.badlogic.gdx.utils.Base64Coder;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

import forge.ImageKeys;
import forge.card.CardDb;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.game.card.Card;
import forge.game.player.IHasIcon;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.screens.match.FControl;
import forge.utils.Constants;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

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

    private static final Set<String> _missingIconKeys = new HashSet<String>();
    private static final LoadingCache<String, Texture> _CACHE = CacheBuilder.newBuilder().softValues().build(new ImageLoader());
    private static final Texture _defaultImage;
    static {
        Texture defImage = null;
        try {
            defImage = new Texture(Gdx.files.internal(Constants.NO_CARD_FILE));
        } catch (Exception ex) {
            System.err.println("could not load default card image");
        } finally {
            _defaultImage = (null == defImage) ? new Texture(10, 10, Format.RGBA8888) : defImage; 
        }
    }

    public static void clear() {
        _CACHE.invalidateAll();
        _missingIconKeys.clear();
    }

    public static Texture getImage(Card card) {
        final String key;
        if (!FControl.mayShowCard(card) || card.isFaceDown()) {
            key = ImageKeys.TOKEN_PREFIX + ImageKeys.MORPH_IMAGE;
        } else {
            key = card.getImageKey();
        }
        return getImage(key, true);
    }

    public static Texture getImage(InventoryItem ii) {
        return getImage(ImageKeys.getImageKey(ii, false), true);
    }

    /**
     * retrieve an icon from the cache.  returns the current skin's ICO_UNKNOWN if the icon image is not found
     * in the cache and cannot be loaded from disk.
     */
    public static FImage getIcon(IHasIcon ihi) {
        String imageKey = ihi.getIconImageKey();
        final Texture icon;
        if (_missingIconKeys.contains(imageKey) ||
                null == (icon = getImage(ihi.getIconImageKey(), false))) {
            _missingIconKeys.add(imageKey);
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
            imageKey = getImageKey(getPaperCardFromImageKey(imageKey.substring(2)), altState, true);
            if (StringUtils.isBlank(imageKey)) { 
                return _defaultImage;
            }
        }
        
        // Load from file and add to cache if not found in cache initially. 
        Texture image = ImageCache._CACHE.getIfPresent(imageKey);
        
        // No image file exists for the given key so optionally associate with
        // a default "not available" image and add to cache for given key.
        if (image == null) {
            if (useDefaultIfNotFound) { 
                image = _defaultImage;
                _CACHE.put(imageKey, _defaultImage);
            }
            else {
                image = null;
            }
        }
        return image;
    }

    private static PaperCard getPaperCardFromImageKey(String key) {
        if (key == null) {
            return null;
        }

        PaperCard cp = FModel.getMagicDb().getCommonCards().getCard(key);
        if (cp == null) {
            cp = FModel.getMagicDb().getVariantCards().getCard(key);
        }
        return cp;
    }

    private static String getImageRelativePath(PaperCard cp, boolean backFace, boolean includeSet, boolean isDownloadUrl) {
        final String nameToUse = cp == null ? null : getNameToUse(cp, backFace);
        if ( null == nameToUse )
            return null;
        
        StringBuilder s = new StringBuilder();
        
        CardRules card = cp.getRules();
        String edition = cp.getEdition();
        s.append(ImageCache.toMWSFilename(nameToUse));
        
        final int cntPictures;
        final boolean hasManyPictures;
        final CardDb db =  !card.isVariant() ? FModel.getMagicDb().getCommonCards() : FModel.getMagicDb().getVariantCards();
        if (includeSet) {
            cntPictures = db.getPrintCount(card.getName(), edition); 
            hasManyPictures = cntPictures > 1;
        } else {
            // without set number of pictures equals number of urls provided in Svar:Picture
            String urls = card.getPictureUrl(backFace);
            cntPictures = StringUtils.countMatches(urls, "\\") + 1;

            // raise the art index limit to the maximum of the sets this card was printed in
            int maxCntPictures = db.getMaxPrintCount(card.getName());
            hasManyPictures = maxCntPictures > 1;
        }
        
        int artIdx = cp.getArtIndex() - 1;
        if (hasManyPictures) {
            if ( cntPictures <= artIdx ) // prevent overflow
                artIdx = cntPictures == 0 ? 0 : artIdx % cntPictures;
            s.append(artIdx + 1);
        }
        
        // for whatever reason, MWS-named plane cards don't have the ".full" infix
        if (!card.getType().isPlane() && !card.getType().isPhenomenon()) {
            s.append(".full");
        }
        
        final String fname;
        if (isDownloadUrl) {
            s.append(".jpg");
            fname = Base64Coder.encodeString(s.toString());
        }
        else {
            fname = s.toString();
        }
        
        if (includeSet) {
            String editionAliased = isDownloadUrl ? FModel.getMagicDb().getEditions().getCode2ByCode(edition) : getSetFolder(edition);
            return String.format("%s/%s", editionAliased, fname);
        }
        return fname;
    }

    public static boolean hasBackFacePicture(PaperCard cp) {
        CardSplitType cst = cp.getRules().getSplitType();
        return cst == CardSplitType.Transform || cst == CardSplitType.Flip; 
    }

    public static String getSetFolder(String edition) {
        return  !Constants.CACHE_CARD_PICS_SUBDIR.containsKey(edition)
                ? FModel.getMagicDb().getEditions().getCode2ByCode(edition) // by default 2-letter codes from MWS are used
                : Constants.CACHE_CARD_PICS_SUBDIR.get(edition); // may use custom paths though
    }

    private static String getNameToUse(PaperCard cp, boolean backFace) {
        final CardRules card = cp.getRules();
        if (backFace ) {
            if (hasBackFacePicture(cp)) {
                return card.getOtherPart().getName();
            }
            return null;
        }
        if (CardSplitType.Split == cp.getRules().getSplitType()) {
            return card.getMainPart().getName() + card.getOtherPart().getName();
        }
        return cp.getName();
    }

    public static String getImageKey(PaperCard cp, boolean backFace, boolean includeSet) {
        return getImageRelativePath(cp, backFace, includeSet, false);
    }

    public static String getDownloadUrl(PaperCard cp, boolean backFace) {
        return getImageRelativePath(cp, backFace, true, true);
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
