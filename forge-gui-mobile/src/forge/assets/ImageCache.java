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
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import forge.gui.FThreads;
import forge.util.FileUtil;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap.Format;
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
    private static final ObjectSet<String> missingIconKeys = new ObjectSet<>();
    private static List<String> borderlessCardlistKey = FileUtil.readFile(ForgeConstants.BORDERLESS_CARD_LIST_FILE);
    static int maxCardCapacity = 400; //default card capacity
    static EvictingQueue<String> q;
    static Queue<String> syncQ;
    static TextureParameter defaultParameter = new TextureParameter();
    static TextureParameter filtered = new TextureParameter();
    public static void initCache(int capacity) {
        //init filter
        filtered.genMipMaps = true;
        filtered.minFilter = Texture.TextureFilter.MipMapLinearLinear;
        filtered.magFilter = Texture.TextureFilter.Linear;
        //override maxCardCapacity
        maxCardCapacity = capacity;
        //init q
        q = EvictingQueue.create(capacity);
        //init syncQ for threadsafe use
        syncQ = Queues.synchronizedQueue(q);
    }
    public static final Texture defaultImage;
    public static FImage BlackBorder = FSkinImage.IMG_BORDER_BLACK;
    public static FImage WhiteBorder = FSkinImage.IMG_BORDER_WHITE;
    private static final ObjectMap<String, Pair<String, Boolean>> imageBorder = new ObjectMap<>(1024);

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
        missingIconKeys.clear();
        ImageKeys.clearMissingCards();
    }
    public static void clearGeneratedCards() {
        Forge.getAssets().generatedCards.clear();
    }
    public static void disposeTextures(){
        CardRenderer.clearcardArtCache();
        Forge.getAssets().cards.clear();
    }

    public static Texture getImage(InventoryItem ii) {
        String imageKey = ii.getImageKey(false);
        if (imageKey != null) {
            if(imageKey.startsWith(ImageKeys.CARD_PREFIX) || imageKey.startsWith(ImageKeys.TOKEN_PREFIX))
                return getImage(ii.getImageKey(false), true, false);
        }
        boolean useDefaultNotFound = imageKey != null && !(imageKey.startsWith(ImageKeys.PRECON_PREFIX) || imageKey.startsWith(ImageKeys.FATPACK_PREFIX)
                || imageKey.startsWith(ImageKeys.BOOSTERBOX_PREFIX) || imageKey.startsWith(ImageKeys.BOOSTER_PREFIX) || imageKey.startsWith(ImageKeys.TOURNAMENTPACK_PREFIX));
        return getImage(ii.getImageKey(false), useDefaultNotFound, true);
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

        PaperCard paperCard = null;
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
                return defaultImage;
            }
        }

        Texture image;
        File imageFile = ImageKeys.getImageFile(imageKey);
        if (useDefaultIfNotFound) {
            // Load from file and add to cache if not found in cache initially.
            image = getAsset(imageKey, imageFile, useOtherCache);

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
            image = loadAsset(imageKey, imageFile, useOtherCache);
        } catch (final Exception ex) {
            image = null;
        }

        // No image file exists for the given key so optionally associate with
        // a default "not available" image.
        if (image == null) {
            if (useDefaultIfNotFound) {
                image = defaultImage;
                /*fix not loading image file since we intentionally not to update the cache in order for the
                  image fetcher to update automatically after the card image/s are downloaded*/
                imageLoaded = false;
                if (image != null && imageBorder.get(image.toString()) == null)
                    imageBorder.put(image.toString(), Pair.of(Color.valueOf("#171717").toString(), false)); //black border
            }
        }
        return image;
    }
    static Texture getAsset(String imageKey, File file, boolean otherCache) {
        if (file == null)
            return null;
        if (!otherCache && Forge.enableUIMask.equals("Full") && isBorderless(imageKey))
            return Forge.getAssets().generatedCards.get(imageKey);
        if (otherCache)
            return Forge.getAssets().others.get(file.getPath(), Texture.class, false);
        return Forge.getAssets().cards.get(file.getPath(), Texture.class, false);
    }
    static Texture loadAsset(String imageKey, File file, boolean otherCache) {
        if (file == null)
            return null;
        syncQ.add(file.getPath());
        if (!otherCache && Forge.getAssets().cards.getLoadedAssets() > maxCardCapacity) {
            unloadCardTextures(Forge.getAssets().cards);
            return null;
        }
        String fileName = file.getPath();
        //load to assetmanager
        if (otherCache) {
            Forge.getAssets().others.load(fileName, Texture.class, Forge.isTextureFilteringEnabled() ? filtered : defaultParameter);
            Forge.getAssets().others.finishLoadingAsset(fileName);
        } else {
            Forge.getAssets().cards.load(fileName, Texture.class, Forge.isTextureFilteringEnabled() ? filtered : defaultParameter);
            Forge.getAssets().cards.finishLoadingAsset(fileName);
        }
        //return loaded assets
        if (otherCache) {
            return Forge.getAssets().others.get(fileName, Texture.class, false);
        } else {
            Texture t = Forge.getAssets().cards.get(fileName, Texture.class, false);
            //if full bordermasking is enabled, update the border color
            if (Forge.enableUIMask.equals("Full")) {
                boolean borderless = isBorderless(imageKey);
                updateBorders(t.toString(), borderless ? Pair.of(Color.valueOf("#171717").toString(), false): isCloserToWhite(getpixelColor(t)));
                //if borderless, generate new texture from the asset and store
                if (borderless) {
                    Forge.getAssets().generatedCards.put(imageKey, generateTexture(new FileHandle(file), t, Forge.isTextureFilteringEnabled()));
                }
            }
            return t;
        }
    }
    static void unloadCardTextures(AssetManager manager) {
        //get latest images from syncQ
        Set<String> newQ = Sets.newHashSet(syncQ);
        //get loaded images from assetmanager
        Set<String> old = Sets.newHashSet(manager.getAssetNames());
        //get all images not in newQ (old images to unload)
        Set<String> toUnload = Sets.difference(old, newQ);
        //unload from assetmanager to save RAM
        for (String asset : toUnload) {
            manager.unload(asset);
        }
        //clear cachedArt since this is dependant to the loaded texture
        CardRenderer.clearcardArtCache();
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
        try {
            return Color.valueOf(imageBorder.get(t.toString()).getLeft());
        } catch (Exception e) {
            return Color.valueOf("#171717");
        }
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
        return isBorderless(t);
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
    public static Texture generateTexture(FileHandle fh, Texture t, boolean textureFilter) {
        if (t == null || fh == null)
            return t;
        final Texture[] n = new Texture[1];
        FThreads.invokeInEdtNowOrLater(() -> {
            Pixmap pImage = new Pixmap(fh);
            int w = pImage.getWidth();
            int h = pImage.getHeight();
            int radius = (h - w) / 8;
            Pixmap pMask = createRoundedRectangle(w, h, radius, Color.RED);
            drawPixelstoMask(pImage, pMask);
            TextureData textureData = new PixmapTextureData(
                    pMask, //pixmap to use
                    Format.RGBA8888,
                    textureFilter, //use mipmaps
                    false, true);
            n[0] = new Texture(textureData);
            if (textureFilter)
                n[0].setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            pImage.dispose();
            pMask.dispose();
        });
        return n[0];
    }
    public static Pixmap createRoundedRectangle(int width, int height, int cornerRadius, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        Pixmap ret = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        //round corners
        pixmap.fillCircle(cornerRadius, cornerRadius, cornerRadius);
        pixmap.fillCircle(width - cornerRadius - 1, cornerRadius, cornerRadius);
        pixmap.fillCircle(cornerRadius, height - cornerRadius - 1, cornerRadius);
        pixmap.fillCircle(width - cornerRadius - 1, height - cornerRadius - 1, cornerRadius);
        //two rectangle parts
        pixmap.fillRectangle(cornerRadius, 0, width - cornerRadius * 2, height);
        pixmap.fillRectangle(0, cornerRadius, width, height - cornerRadius * 2);
        //draw rounded rectangle
        ret.setColor(color);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (pixmap.getPixel(x, y) != 0) ret.drawPixel(x, y);
            }
        }
        pixmap.dispose();
        return ret;
    }
    public static void drawPixelstoMask(Pixmap pixmap, Pixmap mask){
        int pixmapWidth = mask.getWidth();
        int pixmapHeight = mask.getHeight();
        Color pixelColor = new Color();
        for (int x=0; x<pixmapWidth; x++){
            for (int y=0; y<pixmapHeight; y++){
                if (mask.getPixel(x, y) != 0) {
                    Color.rgba8888ToColor(pixelColor, pixmap.getPixel(x, y));
                    mask.setColor(pixelColor);
                    mask.drawPixel(x, y);
                }
            }
        }
    }

    public static boolean isBorderless(String imagekey) {
        if(borderlessCardlistKey.isEmpty())
            return false;
        if (imagekey.length() > 7) {
            if ((!imagekey.substring(0, 7).contains("MPS_KLD"))&&(imagekey.substring(0, 4).contains("MPS_"))) //MPS_ sets except MPD_KLD
                return true;
        }
        return borderlessCardlistKey.contains(TextUtil.fastReplace(imagekey,".full",".fullborder"));
    }

    public static boolean isBorderless(Texture t) {
        if(borderlessCardlistKey.isEmpty())
            return false;
        //generated texture/pixmap?
        if (t.toString().contains("com.badlogic.gdx.graphics.Texture@"))
            return true;
        return borderlessCardlistKey.stream().anyMatch(key -> t.toString().contains(key));
    }

    public static String getpixelColor(Texture i) {
        if (!i.getTextureData().isPrepared()) {
            i.getTextureData().prepare(); //prepare texture
        }
        //get pixmap from texture data
        Pixmap pixmap = i.getTextureData().consumePixmap();
        //get pixel color from x,y texture coordinate based on the image fullborder or not
        Color color = new Color(pixmap.getPixel(croppedBorderImage(i).getRegionX()+1, croppedBorderImage(i).getRegionY()+1));
        pixmap.dispose();
        return color.toString();
    }
    public static Pair<String, Boolean> isCloserToWhite(String c){
        if (c == null || c == "")
            return Pair.of(Color.valueOf("#171717").toString(), false);
        int c_r = Integer.parseInt(c.substring(0,2),16);
        int c_g = Integer.parseInt(c.substring(2,4),16);
        int c_b = Integer.parseInt(c.substring(4,6),16);
        int brightness = ((c_r * 299) + (c_g * 587) + (c_b * 114)) / 1000;
        return  Pair.of(c,brightness > 155);
    }
}
