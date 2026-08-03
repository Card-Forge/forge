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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.cache.LoadingCache;
import com.mortennobel.imagescaling.ResampleOp;

import forge.card.CardSplitType;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.localinstance.properties.ForgeConstants;
import forge.util.SleeveArt;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinIcon;
import forge.toolbox.imaging.FCardImageRenderer;
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
 * @version $Id: ImageCache.java 25093 2014-03-08 05:36:37Z drdev $
 */
public class ImageCache {
    // short prefixes to save memory

    private static final Set<String> _missingIconKeys = new HashSet<>();
    private static final LoadingCache<String, BufferedImage> _CACHE = CacheBuilder.newBuilder()
            .maximumSize(FModel.getPreferences().getPrefInt((FPref.UI_IMAGE_CACHE_MAXIMUM)))
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .build(new ImageLoader());
    private static final BufferedImage _defaultImage;
    private static final BufferedImage _stars;
    private static final BufferedImage _inv_stars;
    static {
        BufferedImage defImage = null;
        BufferedImage stars = null;
        BufferedImage inv_stars = null;
        try {
            defImage = ImageIO.read(new File(ForgeConstants.NO_CARD_FILE));
        } catch (Exception ex) {
            System.err.println("could not load default card image");
        } finally {
            _defaultImage = (null == defImage) ? new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB) : defImage;
        }
        try {
            stars = ImageIO.read(new File(ForgeConstants.STARS_FILE));
            inv_stars =ImageIO.read(new File(ForgeConstants.STARS_FILE));
            // https://github.com/yusufshakeel/Java-Image-Processing-Project/blob/master/example/Negative.java
            //get image width and height
            int width = inv_stars.getWidth();
            int height = inv_stars.getHeight();

            //convert to negative
            for(int y = 0; y < height; y++){
                for(int x = 0; x < width; x++){
                    int p = inv_stars.getRGB(x,y);

                    int a = (p>>24)&0xff;
                    int r = (p>>16)&0xff;
                    int g = (p>>8)&0xff;
                    int b = p&0xff;

                    //subtract RGB from 255
                    r = 255 - r;
                    g = 255 - g;
                    b = 255 - b;

                    //set new RGB value
                    p = (a<<24) | (r<<16) | (g<<8) | b;
                    inv_stars.setRGB(x, y, p);
                }
            }
        } catch (Exception ex) {
            System.err.println("could not load default stars image");
        } finally {
            _stars = (null == stars) ? new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB) : stars;
            _inv_stars = (null == inv_stars) ? new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB) : inv_stars;
        }
    }

    public static void clear() {
        _CACHE.invalidateAll();
        _missingIconKeys.clear();
        ImageKeys.clearMissingCards();
    }

    /**
     * retrieve an image from the cache.  returns null if the image is not found in the cache
     * and cannot be loaded from disk.  pass -1 for width and/or height to avoid resizing in that dimension.
     */
    public static BufferedImage getImage(final CardView card, final Iterable<PlayerView> viewers, final int width, final int height) {
        final String key = card.getCurrentState().getImageKey(viewers);
        return scaleImage(key, width, height, true, card);
    }

    /**
     * retrieve an image from the cache.  returns null if the image is not found in the cache
     * and cannot be loaded from disk.  pass -1 for width and/or height to avoid resizing in that dimension.
     * Same as getImage() but returns null if the image is not available, instead of a default image.
     */
    public static BufferedImage getImageNoDefault(final CardView card, final Iterable<PlayerView> viewers, final int width, final int height) {
        final String key = card.getCurrentState().getImageKey(viewers);
        return scaleImage(key, width, height, false, card);
    }

    /**
     * retrieve an image from the cache.  returns null if the image is not found in the cache
     * and cannot be loaded from disk.  pass -1 for width and/or height to avoid resizing in that dimension.
     */
    public static BufferedImage getImage(InventoryItem ii, int width, int height) {
        return getImage(ii, width, height, false);
    }
    public static BufferedImage getImage(InventoryItem ii, int width, int height, boolean altState) {
        return scaleImage(ii.getImageKey(altState), width, height, true, null);
    }

    /**
     * retrieve an icon from the cache.  returns the current skin's ICO_UNKNOWN if the icon image is not found
     * in the cache and cannot be loaded from disk.
     */
    public static SkinIcon getIcon(String imageKey) {
        final BufferedImage i;
        if (_missingIconKeys.contains(imageKey) ||
                null == (i = scaleImage(imageKey, -1, -1, false, null))) {
            _missingIconKeys.add(imageKey);
            return FSkin.getIcon(FSkinProp.ICO_UNKNOWN);
        }
        return new FSkin.UnskinnedIcon(i);
    }

    /**
     * This requests the original unscaled image from the cache for the given key.
     * If the image does not exist then it can return a default image if desired.
     * <p>
     * If the requested image is not present in the cache then it attempts to load
     * the image from file (slower) and then add it to the cache for fast future access.
     * </p>
     *
     * @param cardView This is for emblem, since there is no paper card for them
     *
     */
    public static BufferedImage getOriginalImage(String imageKey, boolean useDefaultIfNotFound, CardView cardView) {
        return getOriginalImageInternal(imageKey, useDefaultIfNotFound, cardView).getLeft();
    }

    public static Pair<BufferedImage, Boolean> getCardOriginalImageInfo(String imageKey, boolean useDefaultIfNotFound) {
        return getOriginalImageInternal(imageKey, useDefaultIfNotFound, null);
    }

    private static int sleeveIndexOf(final CardView cardView) {
        final PlayerView owner = cardView != null ? cardView.getOwner() : null;
        return owner != null ? owner.getSleeveIndex() : 0;
    }

    private static String hiddenSleeveCacheKey(final CardView cardView, final int width, final int height) {
        final String artKey = customSleeveArtKey(cardView);
        if (artKey != null) {
            return String.format("__SLEEVEART_%s_%d__#%dx%d", SleeveArt.cacheFileName(artKey),
                    sleeveArtOffsetOf(cardView), width, height);
        }
        return String.format("__SLEEVE_%d__#%dx%d", sleeveIndexOf(cardView), width, height);
    }

    private static int sleeveArtOffsetOf(final CardView cardView) {
        final PlayerView owner = cardView != null ? cardView.getOwner() : null;
        return owner != null ? owner.getSleeveArtOffset() : SleeveArt.DEFAULT_OFFSET;
    }

    private static String customSleeveArtKey(final CardView cardView) {
        final PlayerView owner = cardView != null ? cardView.getOwner() : null;
        if (owner == null) {
            return null;
        }
        final String key = owner.getSleeveArtKey();
        return key == null || key.isEmpty() ? null : key;
    }

    // null when the art is not yet cached; a fetch is started so the built-in sleeve shows meanwhile
    private static BufferedImage customSleeveBack(final CardView cardView) {
        final String key = customSleeveArtKey(cardView);
        if (key == null) {
            return null;
        }
        final BufferedImage art = getSleeveArtCropped(key, sleeveArtOffsetOf(cardView));
        if (art != null) {
            return art;
        }
        fetchSleeveArt(key, () -> { });
        return null;
    }

    // The width:height ratio of the built-in deck sleeves, so card-art sleeves match their shape
    public static double sleeveAspect() {
        final BufferedImage s = FSkin.getSleeveImage(0);
        if (s != null && s.getHeight() > 0) {
            return (double) s.getWidth() / s.getHeight();
        }
        return 360.0 / 500.0; // fallback to the sleeve sprite tile ratio
    }

    // Matches the dark frame baked into the built-in sleeve sprites, ~4% of the short edge
    private static final Color SLEEVE_ART_BORDER = new Color(38, 37, 38);
    private static final double SLEEVE_ART_BORDER_FRACTION = 0.04;
    // A thin diagonally-lit sliver just inside the frame (bright top-right, dark bottom-left),
    // giving the art/border seam the same depth as the built-in sleeves.
    private static final double SLEEVE_ART_BEVEL_FRACTION = 0.008;
    private static final int SLEEVE_ART_BEVEL_HI = 50;
    private static final int SLEEVE_ART_BEVEL_LO = 40;

    // Cover-crop a (usually landscape) art-crop to the built-in sleeve aspect, positioning the crop
    // window along whichever axis has slack by offset (0 = left/top, 1000 = right/bottom, 500 = centre),
    // then frame it so it reads as a sleeve next to the built-in ones in the picker.
    private static BufferedImage cropToCardAspect(final BufferedImage src, final int offset) {
        final double aspect = sleeveAspect();
        final int w = src.getWidth();
        final int h = src.getHeight();
        final double srcAspect = (double) w / h;
        final double f = SleeveArt.clampOffset(offset) / 1000.0;
        int cropW, cropH;
        if (srcAspect > aspect) {
            cropH = h;
            cropW = (int) Math.round(h * aspect);
        } else {
            cropW = w;
            cropH = (int) Math.round(w / aspect);
        }
        // only the cropped (slack) axis moves; the pinned axis has no travel
        final int x = (int) Math.round((w - cropW) * f);
        final int y = (int) Math.round((h - cropH) * f);
        final BufferedImage out = new BufferedImage(cropW, cropH, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = out.createGraphics();
        g.drawImage(src.getSubimage(x, y, cropW, cropH), 0, 0, null);
        final int bw = Math.max(1, (int) Math.round(Math.min(cropW, cropH) * SLEEVE_ART_BORDER_FRACTION));
        g.setColor(SLEEVE_ART_BORDER);
        g.fillRect(0, 0, cropW, bw);
        g.fillRect(0, cropH - bw, cropW, bw);
        g.fillRect(0, 0, bw, cropH);
        g.fillRect(cropW - bw, 0, bw, cropH);
        g.dispose();
        bevelSeam(out, bw);
        return out;
    }

    // Blend a diagonally-lit sliver into the inner edge of the frame: white toward the top-right
    // corner, black toward the bottom-left, fading to neutral along the way.
    private static void bevelSeam(final BufferedImage img, final int frame) {
        final int w = img.getWidth();
        final int h = img.getHeight();
        final int s = Math.max(1, (int) Math.round(Math.min(w, h) * SLEEVE_ART_BEVEL_FRACTION));
        for (int yy = frame; yy < h - frame; yy++) {
            for (int xx = frame; xx < w - frame; xx++) {
                if (yy >= frame + s && yy < h - frame - s && xx >= frame + s && xx < w - frame - s) {
                    continue;
                }
                final double dd = ((double) xx / (w - 1) + (1.0 - (double) yy / (h - 1))) / 2.0;
                final int rgb = img.getRGB(xx, yy);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                if (dd > 0.5) {
                    final double a = (dd - 0.5) * 2 * SLEEVE_ART_BEVEL_HI / 255.0;
                    r += (int) ((255 - r) * a);
                    g += (int) ((255 - g) * a);
                    b += (int) ((255 - b) * a);
                } else {
                    final double a = (0.5 - dd) * 2 * SLEEVE_ART_BEVEL_LO / 255.0;
                    r -= (int) (r * a);
                    g -= (int) (g * a);
                    b -= (int) (b * a);
                }
                img.setRGB(xx, yy, (r << 16) | (g << 8) | b);
            }
        }
    }

    /** The cropped card-art sleeve image for a key at the given offset if it is cached, else null (no fetch). */
    public static BufferedImage getSleeveArtCropped(final String key, final int offset) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        final File f = new File(ForgeConstants.CACHE_SLEEVE_PICS_DIR, SleeveArt.cacheFileName(key));
        if (!f.exists()) {
            return null;
        }
        try {
            final BufferedImage art = ImageIO.read(f);
            return art == null ? null : cropToCardAspect(art, offset);
        } catch (final IOException e) {
            return null;
        }
    }

    /** The full, uncropped art-crop image for a key if cached, else null. Used by the draggable preview. */
    public static BufferedImage getSleeveArtFull(final String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        final File f = new File(ForgeConstants.CACHE_SLEEVE_PICS_DIR, SleeveArt.cacheFileName(key));
        if (!f.exists()) {
            return null;
        }
        try {
            return ImageIO.read(f);
        } catch (final IOException e) {
            return null;
        }
    }

    /** Fetch the card-art sleeve for a key (if not cached) and run onReady on the EDT when it lands. */
    public static void fetchSleeveArt(final String key, final Runnable onReady) {
        if (key == null || key.isEmpty()) {
            return;
        }
        SwingUtilities.invokeLater(() -> GuiBase.getInterface().getImageFetcher().fetchSleeveArt(key, onReady::run));
    }

    // return the pair of image and a flag to indicate if it is a placeholder image.
    private static Pair<BufferedImage, Boolean> getOriginalImageInternal(String imageKey, boolean useDefaultIfNotFound, CardView cardView) {
        if (null == imageKey) {
            return Pair.of(null, false);
        }

        // Owner's sleeve as the back for any card the viewer can't see
        // With no sleeve set, fall through so the standard t:hidden back renders
        if (imageKey.equals(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD))) {
            final BufferedImage artBack = customSleeveBack(cardView);
            if (artBack != null) {
                return Pair.of(artBack, false);
            }
            final BufferedImage back = FSkin.getSleeveImage(sleeveIndexOf(cardView));
            if (back != null) {
                return Pair.of(back, false);
            }
        }

        IPaperCard ipc = null;
        boolean altState = imageKey.endsWith(ImageKeys.BACKFACE_POSTFIX);
        String specColor = "";
        if (imageKey.endsWith(ImageKeys.SPECFACE_W)) {
            specColor = "white";
        } else if (imageKey.endsWith(ImageKeys.SPECFACE_U)) {
            specColor = "blue";
        } else if (imageKey.endsWith(ImageKeys.SPECFACE_B)) {
            specColor = "black";
        } else if (imageKey.endsWith(ImageKeys.SPECFACE_R)) {
            specColor = "red";
        } else if (imageKey.endsWith(ImageKeys.SPECFACE_G)) {
            specColor = "green";
        }
        if (altState)
            imageKey = imageKey.substring(0, imageKey.length() - ImageKeys.BACKFACE_POSTFIX.length());
        if (!specColor.isEmpty())
            imageKey = imageKey.substring(0, imageKey.length() - ImageKeys.SPECFACE_W.length());
        if (imageKey.startsWith(ImageKeys.CARD_PREFIX)) {
            ipc = ImageUtil.getPaperCardFromImageKey(imageKey);
            if (ipc != null) {
                if (altState) {
                    imageKey = ipc.getCardAltImageKey();
                } else if (!specColor.isEmpty()) {
                    imageKey = ImageUtil.getImageKey(ipc, specColor, true);
                } else {
                    imageKey = ipc.getCardImageKey();
                }
                if (StringUtils.isBlank(imageKey))
                    return Pair.of(_defaultImage, true);
            }
        }

        // Replace .full to .artcrop if art crop is preferred
        // Only allow use art if the artist info is available
        boolean useArtCrop = "Crop".equals(FModel.getPreferences().getPref(ForgePreferences.FPref.UI_CARD_ART_FORMAT))
            && ipc != null && !ipc.getArtist().isEmpty();
        String originalKey = imageKey;
        if (useArtCrop) {
            if (ipc.getRules().getSplitType() == CardSplitType.Flip) {
                // Art crop will always use front face as image key for flip cards
                imageKey = ipc.getCardImageKey();
            }
            imageKey = TextUtil.fastReplace(imageKey, ".full", ".artcrop");
        }

        // Load from file and add to cache if not found in cache initially.
        BufferedImage original = getImage(imageKey);

        if (original == null && !useDefaultIfNotFound) {
            return Pair.of(null, false);
        }

        // if art crop is exist, check also if the full card image is also cached.
        if (useArtCrop && original != null) {
            BufferedImage cached = _CACHE.getIfPresent(originalKey);
            if (cached != null)
                return Pair.of(cached, false);
        }

        boolean noBorder = !useArtCrop && !isPreferenceEnabled(ForgePreferences.FPref.UI_RENDER_BLACK_BORDERS);
        boolean fetcherEnabled = isPreferenceEnabled(ForgePreferences.FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER);
        boolean isPlaceholder = (original == null) && fetcherEnabled;
        String setCode = imageKey.split("/")[0].trim().toUpperCase();

        // If the user has indicated that they prefer Forge NOT render a black border, round the image corners
        // to account for JPEG images that don't have a transparency.
        if (original != null && noBorder) {
            // use a quadratic equation to calculate the needed radius from an image dimension
            int radius;
            float width = original.getWidth();
            if (setCode.equals("A")) {  // Alpha
                // radius = 100; // 745 x 1040
                // radius = 68; // 488 x 680
                // radius = 25; // 146 x 204
                radius = (int)(-107.0 *(width * width) / 52648506.0 + 743043.0 * width / 5849834.0 + 171067480.0 / 26324253.0);
            } else if (setCode.equals("ME2") ||     // Masters Edition II
                    setCode.equals("ME3") ||        // Masters Edition III
                    setCode.equals("ME4") ||        // Masters Edition IV
                    setCode.equals("TD0") ||        // Commander Theme Decks
                    setCode.equals("TD1")           // Magic Online Deck Series
                    ) {
                // radius = 77; // 745 x 1040
                // radius = 52; // 488 x 680
                // radius = 19; // 146 x 204
                radius = (int)(23.0 * (width * width) / 17549502.0 + 559597.0 * width /5849834.0 + 43923392.0 / 8774751.0);
            } else {
                // radius = 65; // 745 x 1040
                // radius = 45; // 488 x 680
                // radius = 15; // 146 x 204
                radius = (int)(-145.0 * (width * width) / 8774751.0 + 287215.0 * width / 2924917.0 + 8911915.0 / 8774751.0);
            }
            //System.out.println(setCode + " - " + original.getWidth() + " - " + radius);
            original = makeRoundedCorner(original, radius);
        }

        // if image has white corners, get try to crop it out
        if (original != null && isWhite(FSkin.getColorFromPixel(original.getRGB(0, 0)))) {
            if (!isWhiteBorderSet(setCode)) {
                int xSpacing = original.getWidth() / 40;
                int ySpacing = original.getHeight() / 57;
                original = original.getSubimage(xSpacing, ySpacing, original.getWidth() - (2* xSpacing), original.getHeight() - (2* ySpacing));
            }
        }

        // No image file exists for the given key so optionally associate with
        // a default "not available" image, however do not add it to the cache,
        // as otherwise it's problematic to update if the real image gets fetched.
        if (original == null || useArtCrop) {
            if ((ipc != null || cardView != null) && !originalKey.equals(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD))) {
                float screenScale = GuiBase.getInterface().getScreenScale();
                int width = Math.round(488 * screenScale), height = Math.round(680 * screenScale);
                BufferedImage art = original;
                CardView card = ipc != null ? Card.getCardForUi(ipc).getView() : cardView;
                String legalString = null;
                original = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                if (art != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(StaticData.instance().getCardEdition(ipc.getEdition()).getDate());
                    int year = cal.get(Calendar.YEAR);
                    legalString = "Illus. " + ipc.getArtist() + "   ©" + year + " WOTC";
                }
                FCardImageRenderer.drawCardImage(original.createGraphics(), card, altState, width, height, art, legalString);
                // Skip store cache since the rendering speed seems to be fast enough
                // Also the scaleImage below will already cache re-sized image for CardPanel anyway
                // if (art != null || !fetcherEnabled)
                //     _CACHE.put(originalKey, original);
            } else {
                original = _defaultImage;
            }
        }

        return Pair.of(original, isPlaceholder);
    }

    private static boolean isWhite(Color color) {
        return color.getRed() > 200 && color.getBlue() > 200 && color.getGreen() > 200;
    }

    private static boolean isWhiteBorderSet(String setCode) {
        return setCode.equals("U") || setCode.equals("R") || setCode.equals("4E") || setCode.equals("5E") ||
            setCode.equals("6E") || setCode.equals("7E") || setCode.equals("8E") || setCode.equals("9E");
    }

    public static boolean isSupportedImageSize(final int width, final int height) {
        return !((3 > width && -1 != width) || (3 > height && -1 != height));
    }

    // cardView is for Emblem, since there is no paper card for them
    public static BufferedImage scaleImage(String key, final int width, final int height, boolean useDefaultImage, CardView cardView) {
        if (StringUtils.isEmpty(key) || !isSupportedImageSize(width, height)) {
            // picture too small or key not defined; return a blank
            return null;
        }

        String resizedKey = String.format("%s#%dx%d", key, width, height);
        if (key.equals(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD))) {
            resizedKey = hiddenSleeveCacheKey(cardView, width, height);
        }

        final BufferedImage cached = _CACHE.getIfPresent(resizedKey);
        if (null != cached) {
            return cached;
        }

        Pair<BufferedImage, Boolean> orgImgs = getOriginalImageInternal(key, useDefaultImage, cardView);
        BufferedImage original = orgImgs.getLeft();
        boolean isPlaceholder = orgImgs.getRight();
        if (original == null) { return null; }

        if (original == _defaultImage) {
            // Don't put the default image in the cache under the key for the card.
            // Instead, cache it under its own key, to avoid duplication of the
            // default image and to remove the need to invalidate the cache when
            // an image gets downloaded.
            resizedKey = String.format("__DEFAULT__#%dx%d", width, height);
            final BufferedImage cachedDefault = _CACHE.getIfPresent(resizedKey);
            if (null != cachedDefault) {
                return cachedDefault;
            }
        }

        // Calculate the scale required to best fit the image into the requested
        // (width x height) dimensions whilst retaining aspect ratio.
        double scaleX = (-1 == width ? 1 : (double)width / original.getWidth());
        double scaleY = (-1 == height? 1 : (double)height / original.getHeight());
        double bestFitScale = Math.min(scaleX, scaleY);
        if ((bestFitScale > 1) && !FModel.getPreferences().getPrefBoolean(FPref.UI_SCALE_LARGER)) {
            bestFitScale = 1;
        }

        BufferedImage result;
        if (1 == bestFitScale) {
            result = original;
        } else {
            int destWidth  = (int)(original.getWidth()  * bestFitScale);
            int destHeight = (int)(original.getHeight() * bestFitScale);

            ResampleOp resampler = new ResampleOp(destWidth, destHeight);
            result = resampler.filter(original, null);
        }

        if (!isPlaceholder) {
            _CACHE.put(resizedKey, result);
        }
        return result;
    }
    /**
     * Crops the Card Image to get the Card Art of "regular Card frame".
     * @param bufferedImage the image that will be crop
     */
    public static BufferedImage getCroppedArt(BufferedImage bufferedImage, float x, float y, float w, float h) {
        //todo add support for other card frames ie split card, etc.
        x = w * 0.1f;
        y = h * 0.11f;
        w -= 2 * x;
        h *= 0.43f;
        float ratioRatio = w / h / 1.302f;
        if (ratioRatio > 1) { //if too wide, shrink width
            float dw = w * (ratioRatio - 1);
            w -= dw;
            x += dw / 2;
        }
        else { //if too tall, shrink height
            float dh = h * (1 - ratioRatio);
            h -= dh;
            y += dh / 2;
        }
        return bufferedImage.getSubimage(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
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

    private static boolean isPreferenceEnabled(final ForgePreferences.FPref preferenceName) {
        return FModel.getPreferences().getPrefBoolean(preferenceName);
    }

    public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = output.createGraphics();

        // so instead fake soft-clipping by first drawing the desired clip shape
        // in fully opaque black with antialiasing enabled...
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.BLACK);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

        // ... then compositing the image on top,
        // using the black shape from above as alpha source
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, null);

        g2.dispose();

        return output;
    }

    public static boolean isDefaultImage(BufferedImage image) {
        return _defaultImage.equals(image);
    }

    public static BufferedImage getDefaultImage() { return _defaultImage; }

    public static BufferedImage getStarsImage() { return _stars; }

    public static BufferedImage getInvertedStarsImage() { return _inv_stars; }
}
