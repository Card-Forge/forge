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
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.badlogic.gdx.graphics.Pixmap;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import forge.deck.DeckProxy;
import forge.gui.GuiBase;
import forge.util.FileUtil;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

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
    private static ImageCache imageCache;
    private HashSet<String> _missingIconKeys;
    private HashSet<String> missingIconKeys() {
        HashSet<String> result = _missingIconKeys;
        if (result == null) {
            synchronized (this) {
                result = _missingIconKeys;
                if (result == null) {
                    result = new HashSet<>();
                    _missingIconKeys = result;
                }
            }
        }
        return _missingIconKeys;
    }
    private final List<String> borderlessCardlistKey = FileUtil.readFile(ForgeConstants.BORDERLESS_CARD_LIST_FILE);
    public int counter = 0;
    private int maxCardCapacity = 300; //default card capacity
    private EvictingQueue<String> q;
    private Set<String> cardsLoaded;
    private Queue<String> syncQ;
    public static ImageCache getInstance() {
        if (imageCache == null) {
            imageCache = new ImageCache();
            imageCache.initCache(Forge.cacheSize);
        }
        return imageCache;
    }
    private void initCache(int capacity) {
        //override maxCardCapacity
        maxCardCapacity = capacity;
        //init q
        q = EvictingQueue.create(capacity);
        //init syncQ for threadsafe use
        syncQ = Queues.synchronizedQueue(q);
        //cap
        int cl = GuiBase.isAndroid() ? maxCardCapacity + (capacity / 3) : 400;
        cardsLoaded = new HashSet<>(cl);
    }

    private Set<String> getCardsLoaded() {
        if (cardsLoaded == null) {
            cardsLoaded = new HashSet<>(400);
        }
        return cardsLoaded;
    }

    private EvictingQueue<String> getQ() {
        if (q == null) {
            q = EvictingQueue.create(400);
        }
        return q;
    }

    private Queue<String> getSyncQ() {
        if (syncQ == null)
            syncQ = Queues.synchronizedQueue(getQ());
        return syncQ;
    }

    public Texture getDefaultImage() {
        return Forge.getAssets().getDefaultImage();
    }

    private HashMap<String, ImageRecord> _imageRecord;
    private HashMap<String, ImageRecord> imageRecord() {
        HashMap<String, ImageRecord> result = _imageRecord;
        if (result == null) {
            synchronized (this) {
                result = _imageRecord;
                if (result == null) {
                    result = new HashMap<>(maxCardCapacity + (maxCardCapacity / 3));
                    _imageRecord = result;
                }
            }
        }
        return _imageRecord;
    }
    private boolean imageLoaded, delayLoadRequested;

    public void allowSingleLoad() {
        imageLoaded = false; //reset at the beginning of each render
        delayLoadRequested = false;
    }

    public void clear() {
        missingIconKeys().clear();
        ImageKeys.clearMissingCards();
    }

    public void disposeTextures() {
        CardRenderer.clearcardArtCache();
        //unload all cardsLoaded
        try {
            for (String fileName : getCardsLoaded()) {
                if (Forge.getAssets().manager().get(fileName, Texture.class, false) != null) {
                    Forge.getAssets().manager().unload(fileName);
                }
            }
        } catch (Exception ignored) {}
        getCardsLoaded().clear();
        ((Forge) Gdx.app.getApplicationListener()).needsUpdate = true;
    }

    /**
     * Update counter for use with adventure mode since it uses direct loading for assetmanager for loot and shops
     */
    public void updateSynqCount(File file, int count) {
        if (file == null)
            return;
        getSyncQ().add(file.getPath());
        getCardsLoaded().add(file.getPath());
        counter += count;
    }

    public Texture getImage(InventoryItem ii) {
        boolean useDefault = ii instanceof DeckProxy;
        String imageKey = ii.getImageKey(false);
        if (imageKey != null) {
            if (imageKey.startsWith(ImageKeys.CARD_PREFIX) || imageKey.startsWith(ImageKeys.TOKEN_PREFIX))
                return getImage(ii.getImageKey(false), useDefault, false);
        }
        return getImage(ii.getImageKey(false), true, true);
    }

    /**
     * retrieve an icon from the cache.  returns the current skin's ICO_UNKNOWN if the icon image is not found
     * in the cache and cannot be loaded from disk.
     */
    public FImage getIcon(IHasIcon ihi) {
        String imageKey = ihi.getIconImageKey();
        final Texture icon;
        if (missingIconKeys().contains(imageKey) || (icon = getImage(ihi.getIconImageKey(), false, true)) == null) {
            missingIconKeys().add(imageKey);
            return FSkinImage.UNKNOWN;
        }
        return new FTextureImage(icon);
    }

    /**
     * checks the card image exists from the disk.
     */
    public boolean imageKeyFileExists(String imageKey) {
        if (StringUtils.isEmpty(imageKey))
            return false;

        if (imageKey.length() < 2)
            return false;

        final String prefix = imageKey.substring(0, 2);

        PaperCard paperCard;
        if (prefix.equals(ImageKeys.CARD_PREFIX)) {
            try {
                paperCard = ImageUtil.getPaperCardFromImageKey(imageKey);
            } catch (Exception e) {
                return false;
            }
            if (paperCard == null)
                return false;

            if (!FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER)) {
                return paperCard.hasImage();
            } else {
                final boolean backFace = imageKey.endsWith(ImageKeys.BACKFACE_POSTFIX);
                final String cardfilename = backFace ? paperCard.getCardAltImageKey() : paperCard.getCardImageKey();
                return ImageKeys.getCachedCardsFile(cardfilename) != null;
            }
        } else if (prefix.equals(ImageKeys.TOKEN_PREFIX)) {
            final String tokenfilename = imageKey.substring(2) + ".jpg";
            File tokenFile = new File(ForgeConstants.CACHE_TOKEN_PICS_DIR, tokenfilename);
            return tokenFile.exists();
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
    public Texture getImage(String imageKey, boolean useDefaultIfNotFound) {
        return getImage(imageKey, useDefaultIfNotFound, false);
    }

    public Texture getImage(String imageKey, boolean useDefaultIfNotFound, boolean others) {
        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DISABLE_CARD_IMAGES))
            return null;

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
                if (useDefaultIfNotFound)
                    return getDefaultImage();
                else
                    return null;
            }
        }

        Texture image;
        File imageFile = ImageKeys.getImageFile(imageKey);
        if (useDefaultIfNotFound) {
            // Load from file and add to cache if not found in cache initially.
            image = getAsset(imageFile);

            if (image != null) {
                return image;
            }

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
            image = loadAsset(imageKey, imageFile, others);
        } catch (final Exception ex) {
            image = null;
        }

        // No image file exists for the given key so optionally associate with
        // a default "not available" image.
        if (image == null) {
            if (useDefaultIfNotFound) {
                image = getDefaultImage();
                /*fix not loading image file since we intentionally not to update the cache in order for the
                  image fetcher to update automatically after the card image/s are downloaded*/
                imageLoaded = false;
                if (image != null && imageRecord().get(image.toString()) == null)
                    imageRecord().put(image.toString(), new ImageRecord(Color.valueOf("#171717").toString(), false, getRadius(image))); //black border
            }
        }
        return image;
    }

    private Texture getAsset(File file) {
        if (file == null)
            return null;
        return Forge.getAssets().manager().get(file.getPath(), Texture.class, false);
    }

    private Texture loadAsset(String imageKey, File file, boolean others) {
        if (file == null)
            return null;
        Texture check = getAsset(file);
        if (check != null)
            return check;
        if (!others) {
            //update first before clearing
            getSyncQ().add(file.getPath());
            getCardsLoaded().add(file.getPath());
            unloadCardTextures(false);
        }
        String fileName = file.getPath();
        //load to assetmanager
        try {
            if (Forge.getAssets().manager().get(fileName, Texture.class, false) == null) {
                Forge.getAssets().manager().load(fileName, Texture.class, Forge.getAssets().getTextureFilter());
                Forge.getAssets().manager().finishLoadingAsset(fileName);
                counter += 1;
            }
        } catch (Exception e) {
            System.err.println("Failed to load image: " + fileName);
        }

        //return loaded assets
        if (others) {
            return Forge.getAssets().manager().get(fileName, Texture.class, false);
        } else {
            Texture cardTexture = Forge.getAssets().manager().get(fileName, Texture.class, false);
            //if full bordermasking is enabled, update the border color
            if (cardTexture != null) {
                boolean borderless = isBorderless(imageKey);
                String setCode = imageKey.split("/")[0].trim().toUpperCase();
                int radius;
                if (setCode.equals("A") || setCode.equals("LEA") || setCode.equals("B") || setCode.equals("LEB"))
                    radius = 28;
                else if (setCode.equals("MED") || setCode.equals("ME2") || setCode.equals("ME3") || setCode.equals("ME4") || setCode.equals("TD0") || setCode.equals("TD1"))
                    radius = 25;
                else
                    radius = 22;
                updateImageRecord(cardTexture.toString(),
                        borderless ? Color.valueOf("#171717").toString() : isCloserToWhite(getpixelColor(cardTexture)).getLeft(),
                        !borderless && isCloserToWhite(getpixelColor(cardTexture)).getRight(), radius);
            }
            return cardTexture;
        }
    }

    public void unloadCardTextures(boolean removeAll) {
        if (removeAll) {
            try {
                for (String asset : Forge.getAssets().manager().getAssetNames()) {
                    if (asset.contains(".full")) {
                        Forge.getAssets().manager().unload(asset);
                    }
                }
                getSyncQ().clear();
                getCardsLoaded().clear();
                counter = 0;
                CardRenderer.clearcardArtCache();
            } catch (Exception ignored) {}
            return;
        }
        if (getCardsLoaded().size() <= maxCardCapacity)
            return;
        //get latest images from syncQ
        Set<String> newQ = Sets.newHashSet(getSyncQ());
        //get all images not in newQ (cards to unload)
        Set<String> toUnload = Sets.difference(getCardsLoaded(), newQ);
        //unload from assetmanager to save RAM
        try {
            for (String asset : toUnload) {
                if (Forge.getAssets().manager().get(asset, Texture.class, false) != null) {
                    Forge.getAssets().manager().unload(asset);
                }
                getCardsLoaded().remove(asset);
            }
            //clear cachedArt since this is dependant to the loaded texture
            CardRenderer.clearcardArtCache();
            ((Forge) Gdx.app.getApplicationListener()).needsUpdate = true;
        } catch (Exception ignored) {}
    }

    public void preloadCache(Iterable<String> keys) {
        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DISABLE_CARD_IMAGES))
            return;
        for (String imageKey : keys) {
            if (getImage(imageKey, false) == null)
                System.err.println("could not load card image:" + imageKey);
        }
    }

    public void preloadCache(Deck deck) {
        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DISABLE_CARD_IMAGES))
            return;
        if (deck == null || !Forge.enablePreloadExtendedArt)
            return;
        if (deck.getAllCardsInASinglePool().toFlatList().size() <= 100) {
            for (PaperCard p : deck.getAllCardsInASinglePool().toFlatList()) {
                if (getImage(p.getImageKey(false), false) == null)
                    System.err.println("could not load card image:" + p);
            }
        }
    }

    public TextureRegion croppedBorderImage(Texture image) {
        if (!image.toString().contains(".fullborder."))
            return new TextureRegion(image);
        float rscale = 0.96f;
        int rw = Math.round(image.getWidth() * rscale);
        int rh = Math.round(image.getHeight() * rscale);
        int rx = Math.round((image.getWidth() - rw) / 2f);
        int ry = Math.round((image.getHeight() - rh) / 2f) - 2;
        return new TextureRegion(image, rx, ry, rw, rh);
    }

    public Color borderColor(Texture t) {
        if (t == null)
            return Color.valueOf("#171717");
        try {
            return Color.valueOf(imageRecord().get(t.toString()).colorValue);
        } catch (Exception e) {
            return Color.valueOf("#171717");
        }
    }

    public int getFSkinBorders(CardView c) {
        if (c == null)
            return 0;

        CardView.CardStateView state = c.getCurrentState();
        CardEdition ed = FModel.getMagicDb().getEditions().get(state.getSetCode());
        // TODO: Treatment for silver here
        if (ed != null && ed.getBorderColor() == CardEdition.BorderColor.WHITE && state.getFoilIndex() == 0)
            return 1;
        return 0;
    }
    public void updateImageRecord(String textureString, String colorValue, Boolean isClosertoWhite, int radius) {
        imageRecord().put(textureString, new ImageRecord(colorValue, isClosertoWhite, radius));
    }

    public int getRadius(Texture t) {
        if (t == null)
            return 20;
        ImageRecord record = imageRecord().get(t.toString());
        if (record == null)
            return 20;
        Integer i = record.cardRadius;
        if (i == null)
            return 20;
        return i;
    }

    public FImage getBorder(String textureString) {
        ImageRecord record = imageRecord().get(textureString);
        if (record == null)
            return FSkinImage.IMG_BORDER_BLACK;
        Boolean border = record.isCloserToWhite;
        if (border == null)
            return FSkinImage.IMG_BORDER_BLACK;
        return border ? FSkinImage.IMG_BORDER_WHITE : FSkinImage.IMG_BORDER_BLACK;
    }

    public FImage getBorderImage(String textureString, boolean canshow) {
        if (!canshow)
            return FSkinImage.IMG_BORDER_BLACK;
        return getBorder(textureString);
    }

    public FImage getBorderImage(String textureString) {
        return getBorder(textureString);
    }

    public Color getTint(CardView c, Texture t) {
        if (c == null)
            return borderColor(t);
        if (c.isFaceDown())
            return Color.valueOf("#171717");

        CardView.CardStateView state = c.getCurrentState();
        if (state.getColors().isColorless()) { //Moonlace -> target spell or permanent becomes colorless.
            if (state.hasDevoid()) //devoid is colorless at all zones so return its corresponding border color...
                return borderColor(t);
            return Color.valueOf("#A0A6A4");
        } else if (state.getColors().isMonoColor()) {
            if (state.getColors().hasBlack())
                return Color.valueOf("#263238");
            else if (state.getColors().hasBlue())
                return Color.valueOf("#03a9f4");
            else if (state.getColors().hasRed())
                return Color.valueOf("#f44336");
            else if (state.getColors().hasGreen())
                return Color.valueOf("#4caf50 ");
            else if (state.getColors().hasWhite())
                return Color.valueOf("#f4f3e9");
        } else if (state.getColors().isMulticolor())
            return Color.valueOf("#F8DB55");

        return borderColor(t);
    }

    public boolean isBorderless(String imagekey) {
        if (borderlessCardlistKey.isEmpty())
            return false;
        if (imagekey.length() > 7) {
            if ((!imagekey.substring(0, 7).contains("MPS_KLD")) && (imagekey.substring(0, 4).contains("MPS_"))) //MPS_ sets except MPD_KLD
                return true;
        }
        return borderlessCardlistKey.contains(TextUtil.fastReplace(imagekey, ".full", ".fullborder"));
    }

    public String getpixelColor(Texture i) {
        if (!i.getTextureData().isPrepared()) {
            i.getTextureData().prepare(); //prepare texture
        }
        //get pixmap from texture data
        Pixmap pixmap = i.getTextureData().consumePixmap();
        //get pixel color from x,y texture coordinate based on the image fullborder or not
        Color color = new Color(pixmap.getPixel(croppedBorderImage(i).getRegionX() + 1, croppedBorderImage(i).getRegionY() + 1));
        pixmap.dispose();
        return color.toString();
    }

    public Pair<String, Boolean> isCloserToWhite(String c) {
        if (c == null || "".equals(c))
            return Pair.of(Color.valueOf("#171717").toString(), false);
        int c_r = Integer.parseInt(c.substring(0, 2), 16);
        int c_g = Integer.parseInt(c.substring(2, 4), 16);
        int c_b = Integer.parseInt(c.substring(4, 6), 16);
        int brightness = ((c_r * 299) + (c_g * 587) + (c_b * 114)) / 1000;
        return Pair.of(c, brightness > 155);
    }

    private static class ImageRecord {
        String colorValue;
        Boolean isCloserToWhite;
        Integer cardRadius;

        ImageRecord(String colorString, Boolean closetoWhite, int radius) {
            colorValue = colorString;
            isCloserToWhite = closetoWhite;
            cardRadius = radius;
        }
    }
}
