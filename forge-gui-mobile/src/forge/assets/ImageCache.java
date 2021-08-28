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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import forge.Forge;
import forge.ImageKeys;
import forge.card.CardEdition;
import forge.card.CardRenderer;
import forge.deck.Deck;
import forge.game.card.CardView;
import forge.game.player.IHasIcon;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.ImageUtil;
import forge.util.TextUtil;

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

    private static final Set<String> missingIconKeys = new HashSet<>();
    private static LoadingCache<String, Texture> cache;
    public static void initCache(int capacity) {
        cache = CacheBuilder.newBuilder()
                .maximumSize(capacity)
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<String, Texture>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, Texture> removalNotification) {
                        if (removalNotification.wasEvicted()) {
                            if (removalNotification.getValue() != ImageCache.defaultImage)
                                removalNotification.getValue().dispose();

                            CardRenderer.clearcardArtCache();
                        }
                    }
                })
                .build(new ImageLoader());
        System.out.println("Card Texture Cache Size: "+capacity);
    }
    private static final LoadingCache<String, Texture> otherCache = CacheBuilder.newBuilder().build(new OtherImageLoader());
    public static final Texture defaultImage;
    public static FImage BlackBorder = FSkinImage.IMG_BORDER_BLACK;
    public static FImage WhiteBorder = FSkinImage.IMG_BORDER_WHITE;
    private static final Map<String, Pair<String, Boolean>> imageBorder = new HashMap<>(1024);

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
        cache.cleanUp();
        missingIconKeys.clear();
    }

    public static void disposeTexture(){
        CardRenderer.clearcardArtCache();
        clear();
    }

    public static Texture getImage(InventoryItem ii) {
        String imageKey = ii.getImageKey(false);
        if (imageKey != null) {
            if(imageKey.startsWith(ImageKeys.CARD_PREFIX) || imageKey.startsWith(ImageKeys.TOKEN_PREFIX))
                return getImage(ii.getImageKey(false), true, false);
        }
        return getImage(ii.getImageKey(false), true, true);
    }

    /**
     * retrieve an icon from the cache.  returns the current skin's ICO_UNKNOWN if the icon image is not found
     * in the cache and cannot be loaded from disk.
     */
    public static FImage getIcon(IHasIcon ihi) {
        String imageKey = ihi.getIconImageKey();
        final Texture icon;
        if (missingIconKeys.contains(imageKey) || (icon = getImage(ihi.getIconImageKey(), false, true)) == null) {
            missingIconKeys.add(imageKey);
            return FSkinImage.UNKNOWN;
        }
        return new FTextureImage(icon);
    }

    /**
     * checks the card image exists from the disk.
     */
    public static boolean imageKeyFileExists(String imageKey) {
        if (StringUtils.isEmpty(imageKey))
            return false;

        if (imageKey.length() < 2)
            return false;

        final String prefix = imageKey.substring(0, 2);

        if (prefix.equals(ImageKeys.CARD_PREFIX)) {
            PaperCard paperCard = ImageUtil.getPaperCardFromImageKey(imageKey);
            if (paperCard == null)
                return false;

            final boolean backFace = imageKey.endsWith(ImageKeys.BACKFACE_POSTFIX);
            final String cardfilename = backFace ? paperCard.getCardAltImageKey() : paperCard.getCardImageKey();
            if (!new File(ForgeConstants.CACHE_CARD_PICS_DIR + "/" + cardfilename + ".jpg").exists())
                if (!new File(ForgeConstants.CACHE_CARD_PICS_DIR + "/" + cardfilename + ".png").exists())
                    if (!new File(ForgeConstants.CACHE_CARD_PICS_DIR + "/" + TextUtil.fastReplace(cardfilename,".full", ".fullborder") + ".jpg").exists())
                        return false;
        } else if (prefix.equals(ImageKeys.TOKEN_PREFIX)) {
            final String tokenfilename = imageKey.substring(2) + ".jpg";

            if (!new File(ForgeConstants.CACHE_TOKEN_PICS_DIR, tokenfilename).exists())
                return false;
        }

        return true;
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
        return getImage(imageKey, useDefaultIfNotFound, false);
    }
    public static Texture getImage(String imageKey, boolean useDefaultIfNotFound, boolean useOtherCache) {
        if (StringUtils.isEmpty(imageKey)) {
            return null;
        }

        boolean altState = imageKey.endsWith(ImageKeys.BACKFACE_POSTFIX);
        if (altState) {
            imageKey = imageKey.substring(0, imageKey.length() - ImageKeys.BACKFACE_POSTFIX.length());
        }
        if (imageKey.startsWith(ImageKeys.CARD_PREFIX)) {
            PaperCard card = ImageUtil.getPaperCardFromImageKey(imageKey);
            if (card != null)
                imageKey = altState ? card.getCardAltImageKey() : card.getCardImageKey();
            if (StringUtils.isBlank(imageKey)) {
                return defaultImage;
            }
        }

        Texture image;
        if (useDefaultIfNotFound) {
            // Load from file and add to cache if not found in cache initially.
            image = useOtherCache ? otherCache.getIfPresent(imageKey) : cache.getIfPresent(imageKey);

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

        try { image = useOtherCache ? otherCache.get(imageKey) : cache.get(imageKey); }
        catch (final Exception ex) {
            image = null;
        }

        // No image file exists for the given key so optionally associate with
        // a default "not available" image and add to cache for given key.
        if (image == null) {
            if (useDefaultIfNotFound) {
                image = defaultImage;
                if (useOtherCache)
                    otherCache.put(imageKey, defaultImage);
                else
                    cache.put(imageKey, defaultImage);
                if (imageBorder.get(image.toString()) == null)
                    imageBorder.put(image.toString(), Pair.of(Color.valueOf("#171717").toString(), false)); //black border
            }
        }
        return image;
    }
    public static void preloadCache(Iterable<String> keys) {
        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DISABLE_CARD_IMAGES))
            return;
        for (String imageKey : keys){
            if(getImage(imageKey, false) == null)
                System.err.println("could not load card image:"+imageKey);
        }
    }
    public static void preloadCache(Deck deck) {
        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DISABLE_CARD_IMAGES))
            return;
        if(deck == null||!Forge.enablePreloadExtendedArt)
            return;
        if (deck.getAllCardsInASinglePool().toFlatList().size() <= 100) {
            for (PaperCard p : deck.getAllCardsInASinglePool().toFlatList()) {
                if (getImage(p.getImageKey(false),false) == null)
                    System.err.println("could not load card image:"+p.toString());
            }
        }
    }
    public static TextureRegion croppedBorderImage(Texture image) {
        if (!image.toString().contains(".fullborder."))
            return new TextureRegion(image);
        float rscale = 0.96f;
        int rw = Math.round(image.getWidth()*rscale);
        int rh = Math.round(image.getHeight()*rscale);
        int rx = Math.round((image.getWidth() - rw)/2f);
        int ry = Math.round((image.getHeight() - rh)/2f)-2;
        return new TextureRegion(image, rx, ry, rw, rh);
    }
    public static Color borderColor(Texture t) {
        if (t == null)
            return Color.valueOf("#171717");
        return Color.valueOf(imageBorder.get(t.toString()).getLeft());
    }
    public static int getFSkinBorders(CardView c) {
        if (c == null)
            return 0;

        CardView.CardStateView state = c.getCurrentState();
        CardEdition ed = FModel.getMagicDb().getEditions().get(state.getSetCode());
        // TODO: Treatment for silver here
        if (ed != null && ed.getBorderColor() == CardEdition.BorderColor.WHITE && state.getFoilIndex() == 0)
            return 1;
        return 0;
    }
    public static boolean isBorderlessCardArt(Texture t) {
        return ImageLoader.isBorderless(t);
    }
    public static void updateBorders(String textureString, Pair<String, Boolean> colorPair){
        imageBorder.put(textureString, colorPair);
    }
    public static FImage getBorder(String textureString) {
        if (imageBorder.get(textureString) == null)
            return BlackBorder;
        return imageBorder.get(textureString).getRight() ? WhiteBorder : BlackBorder;
    }
    public static FImage getBorderImage(String textureString, boolean canshow) {
        if (!canshow)
            return BlackBorder;
        return getBorder(textureString);
    }
    public static FImage getBorderImage(String textureString) {
        return getBorder(textureString);
    }
    public static Color getTint(CardView c, Texture t) {
        if (c == null)
            return borderColor(t);
        if (c.isFaceDown())
            return Color.valueOf("#171717");

        CardView.CardStateView state = c.getCurrentState();
        if (state.getColors().isColorless()) { //Moonlace -> target spell or permanent becomes colorless.
            if (state.hasDevoid()) //devoid is colorless at all zones so return its corresponding border color...
                return borderColor(t);
            return Color.valueOf("#A0A6A4");
        }
        else if (state.getColors().isMonoColor()) {
            if (state.getColors().hasBlack())
                return Color.valueOf("#48494a");
            else if (state.getColors().hasBlue())
                return Color.valueOf("#62b5f8");
            else if (state.getColors().hasRed())
                return Color.valueOf("#f6532d");
            else if (state.getColors().hasGreen())
                return Color.valueOf("#66cb35");
            else if (state.getColors().hasWhite())
                return Color.valueOf("#EEEBE1");
        }
        else if (state.getColors().isMulticolor())
            return Color.valueOf("#F9E084");

        return borderColor(t);
    }
}
