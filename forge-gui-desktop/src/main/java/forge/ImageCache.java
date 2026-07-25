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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.mortennobel.imagescaling.ResampleOp;

import forge.card.CardSplitType;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.item.PaperCard;
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
import forge.util.ThreadUtil;

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
    private static final Set<String> _placeholderKeys = ConcurrentHashMap.newKeySet();

    // A single large zone view (e.g. a 500-card library opened by a tutor effect) needs two
    // entries per card - the decoded original and the scaled copy the panel paints - so the
    // old 400 cap could not hold even one such view: measured on a 500-card library it
    // evicted ~700 entries per refresh, meaning every reopen re-decoded and re-rendered
    // everything. 400 was never a considered choice for that workload, it is just the value
    // this preference has always defaulted to, so treat it as "unset" and raise it; any other
    // value is one somebody actually chose and is left alone.
    private static final int LEGACY_DEFAULT_CACHE_SIZE = 400;
    private static final int DEFAULT_CACHE_SIZE = 1500;
    private static final LoadingCache<String, BufferedImage> _CACHE = CacheBuilder.newBuilder()
            .maximumSize(cacheSize())
            // soft values so memory pressure - not entry count - is what ultimately evicts
            // images, which is what this class has always documented itself as doing
            .softValues()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .build(new ImageLoader());

    private static int cacheSize() {
        final int configured = FModel.getPreferences().getPrefInt(FPref.UI_IMAGE_CACHE_MAXIMUM);
        return configured == LEGACY_DEFAULT_CACHE_SIZE ? DEFAULT_CACHE_SIZE : configured;
    }
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

    // Cache key suffix for a drawn card face, kept distinct from the "#WxH" scaled variants
    // and from any real image file, so all three can coexist and be invalidated together.
    private static final String RENDERED_SUFFIX = "#rendered";

    // Every scaled/rendered variant cached for a base image key, so a downloaded image can
    // drop exactly its own stale variants. Scanning the whole cache per download instead
    // would be O(cache) per card on the EDT, which is the cost this class is trying to shed.
    private static final Multimap<String, String> _variantKeys =
            Multimaps.synchronizedMultimap(HashMultimap.create());

    public static void clear() {
        preloadGeneration.incrementAndGet(); //cancel any in-flight preloading
        _CACHE.invalidateAll();
        _missingIconKeys.clear();
        _placeholderKeys.clear();
        _variantKeys.clear();
        ImageKeys.clearMissingCards();
    }

    private static final AtomicInteger preloadGeneration = new AtomicInteger();

    /**
     * Decodes the given cards' images into the cache on a single background thread, so
     * views that show many cards at once (e.g. searching a large library) find them
     * ready instead of decoding hundreds of images on first open. Best-effort: entries
     * are softly referenced and may be reclaimed under memory pressure, and preloading
     * stops when the cache is cleared.
     */
    public static void preloadOriginals(final Collection<PaperCard> cards) {
        final int generation = preloadGeneration.get();
        // Resolve each card through the same key-transformation the display paths use
        // (via resolveImageKey), so the warmed cache keys are exactly the ones a zone
        // view will look up. Resolution touches the card database, so it runs here on
        // the calling (EDT) thread; only file probing and decoding go to the worker.
        final List<String> cardKeys = new ArrayList<>(cards.size());
        final List<String> fileKeys = new ArrayList<>(cards.size());
        for (final PaperCard pc : cards) {
            final String cardKey = pc.getImageKey(false);
            if (StringUtils.isEmpty(cardKey)) {
                continue;
            }
            final ResolvedImageKey resolved = resolveImageKey(cardKey);
            if (resolved.fileKey != null && !resolved.useArtCrop) {
                cardKeys.add(cardKey);
                fileKeys.add(resolved.fileKey);
            }
        }

        ThreadUtil.getServicePool().execute(() -> {
            final List<String> missingCardKeys = new ArrayList<>();
            for (int i = 0; i < fileKeys.size(); i++) {
                if (generation != preloadGeneration.get()) {
                    return; //cache cleared - stop warming
                }
                try {
                    if (!warmCard(fileKeys.get(i), generation)) {
                        missingCardKeys.add(cardKeys.get(i));
                    }
                } catch (final Exception e) {
                    //best-effort warming; skip any card whose image fails to load
                }
            }

            // Cards without a local image can't be warmed - hand them to the online
            // fetcher (which no-ops if disabled) so they download during the early game
            // instead of when a zone view first shows them, and warm each image as it
            // arrives. fetchImage must run on the EDT; reuse CachedCardImage's fetcher
            // so in-flight downloads are shared with any card panels requesting them.
            if (!missingCardKeys.isEmpty() && generation == preloadGeneration.get()) {
                SwingUtilities.invokeLater(() -> {
                    for (final String cardKey : missingCardKeys) {
                        if (generation != preloadGeneration.get()) {
                            return;
                        }
                        CachedCardImage.fetcher.fetchImage(cardKey, () -> {
                            //downloaded: drop cached placeholder variants and warm the real image
                            clearGeneratedVariants(cardKey);
                            final ResolvedImageKey resolved = resolveImageKey(cardKey);
                            if (resolved.fileKey != null && generation == preloadGeneration.get()) {
                                ThreadUtil.getServicePool().execute(
                                        () -> warmCard(resolved.fileKey, generation));
                            }
                        });
                    }
                    // Until (unless) the downloads arrive, the zone view will render these
                    // as placeholders - pre-render those now, one card per EDT event
                    // (placeholder rendering shares non-thread-safe statics, so it must
                    // stay on the EDT), so a search doesn't pay for hundreds of renders.
                    prerenderPlaceholders(new ArrayDeque<>(missingCardKeys), generation);
                });
            }
        });
    }

    /**
     * Draws and caches the card face for each card with no local image, one per EDT event
     * (FCardImageRenderer shares non-thread-safe statics, so this cannot leave the EDT).
     * Needs no display size: the render is size-independent and whatever view opens first
     * resamples it, so this works on the very first match, before any zone view has been
     * opened to record a size.
     */
    private static void prerenderPlaceholders(final Deque<String> queue, final int generation) {
        if (queue.isEmpty() || generation != preloadGeneration.get()) {
            return;
        }
        final String cardKey = queue.poll();
        //must pass useDefaultIfNotFound: with no image file on disk the lookup returns early
        //otherwise, and never reaches the card-face render this is here to prime
        getOriginalImage(cardKey, true, null);
        SwingUtilities.invokeLater(() -> prerenderPlaceholders(queue, generation));
    }

    /**
     * Decodes one card's image into the cache off the EDT, which is the expensive half of
     * showing it; whatever size a view later asks for is a resample away. Returns false if
     * the card has no local image file. Best-effort.
     */
    private static boolean warmCard(final String fileKey, final int generation) {
        try {
            if (_CACHE.getIfPresent(fileKey) == null) {
                final File file = ImageKeys.getImageFile(fileKey);
                if (file == null || !file.isFile()) {
                    return false;
                }
                final BufferedImage image = ImageIO.read(file);
                if (image == null || generation != preloadGeneration.get()) {
                    return true;
                }
                _CACHE.put(fileKey, image);
            }
        } catch (final Exception e) {
            //best-effort warming
        }
        return true;
    }


    /**
     * Drops all scaled/rendered variants cached for the given base image key.
     * Called when the image fetcher downloads a real image so cached placeholder
     * renders don't mask it.
     */
    public static void clearGeneratedVariants(final String baseKey) {
        if (StringUtils.isEmpty(baseKey)) {
            return;
        }
        for (final String key : _variantKeys.removeAll(baseKey)) {
            _CACHE.invalidate(key);
            _placeholderKeys.remove(key);
        }
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

    private static String resizedKeyFor(final String key, final CardView cardView, final int width, final int height) {
        if (key.equals(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD))) {
            return hiddenSleeveCacheKey(cardView, width, height);
        }
        return String.format("%s#%dx%d", key, width, height);
    }

    // ========== Asynchronous loading ==========
    //
    // Decoding and resampling a card image takes tens of milliseconds; opening a zone view
    // of a large library does it hundreds of times, which used to freeze the EDT for
    // seconds. The pipeline below keeps everything that touches shared Forge state on the
    // EDT (key/file resolution, placeholder rendering, cache bookkeeping) and moves only
    // pure image work (ImageIO decode, corner rounding, resampling) to background threads.

    // accessed from the EDT only
    private static final Map<String, List<Runnable>> pendingLoads = new HashMap<>();

    /**
     * Cache-only lookup: returns the scaled image if already cached (possibly a cached
     * placeholder render), or null without doing any loading.
     */
    public static BufferedImage getCachedImage(final CardView card, final Iterable<PlayerView> viewers, final int width, final int height) {
        if (!isSupportedImageSize(width, height)) {
            return null;
        }
        final String key = card.getCurrentState().getImageKey(viewers);
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return _CACHE.getIfPresent(resizedKeyFor(key, card, width, height));
    }

    /** Returns true if the cached entry for this card/size is a placeholder render (real image still missing). */
    public static boolean isPlaceholderCached(final CardView card, final Iterable<PlayerView> viewers, final int width, final int height) {
        final String key = card.getCurrentState().getImageKey(viewers);
        return !StringUtils.isEmpty(key) && _placeholderKeys.contains(resizedKeyFor(key, card, width, height));
    }

    /**
     * Loads and caches the scaled image for this card off the EDT where possible, then runs
     * onDone on the EDT. Sleeves, art crops and missing images (placeholder renders) fall
     * back to the synchronous path, executed one card per EDT event so the UI stays
     * responsive. Must be called from the EDT.
     */
    public static void loadImageAsync(final CardView card, final Iterable<PlayerView> viewers, final int width, final int height, final Runnable onDone) {
        FThreads.assertExecutedByEdt(true);
        final String key = card.getCurrentState().getImageKey(viewers);
        if (StringUtils.isEmpty(key) || !isSupportedImageSize(width, height)) {
            return;
        }
        final String resizedKey = resizedKeyFor(key, card, width, height);
        if (_CACHE.getIfPresent(resizedKey) != null) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        List<Runnable> callbacks = pendingLoads.get(resizedKey);
        if (callbacks != null) { //load already in flight - just register the callback
            if (onDone != null) {
                callbacks.add(onDone);
            }
            return;
        }
        callbacks = new ArrayList<>(1);
        if (onDone != null) {
            callbacks.add(onDone);
        }
        pendingLoads.put(resizedKey, callbacks);

        // Resolve the image key on the EDT (card database lookups); the disk probing and
        // decode happen on the worker.
        ResolvedImageKey resolved = null;
        if (!key.equals(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD))) {
            resolved = resolveImageKey(key);
        }

        if (resolved == null || resolved.fileKey == null || resolved.useArtCrop) {
            // Sleeve backs, art crop mode, and cards with no image definition: run the
            // existing synchronous path, one card per EDT event.
            SwingUtilities.invokeLater(() -> {
                scaleImage(key, width, height, true, card);
                finishAsyncLoad(resizedKey);
            });
            return;
        }

        final String originalKey = resolved.fileKey;
        final String setCode = originalKey.split("/")[0].trim().toUpperCase();
        final boolean noBorder = !isPreferenceEnabled(ForgePreferences.FPref.UI_RENDER_BLACK_BORDERS);
        final boolean allowScaleLarger = FModel.getPreferences().getPrefBoolean(FPref.UI_SCALE_LARGER);
        ThreadUtil.getServicePool().execute(() -> {
            BufferedImage decoded = _CACHE.getIfPresent(originalKey);
            BufferedImage scaled = null;
            try {
                if (decoded == null) {
                    // ImageKeys.getImageFile is synchronized; keeping the (slow, many
                    // File.exists probes) resolution here spares the EDT from it.
                    final File imageFile = ImageKeys.getImageFile(originalKey);
                    if (imageFile != null && imageFile.isFile()) {
                        decoded = ImageIO.read(imageFile);
                    }
                }
                if (decoded != null) {
                    scaled = resample(postProcessCardImage(decoded, setCode, noBorder), width, height, allowScaleLarger);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
            final BufferedImage original = decoded;
            final BufferedImage result = scaled;
            SwingUtilities.invokeLater(() -> {
                if (result != null) {
                    _CACHE.put(originalKey, original);
                    _placeholderKeys.remove(resizedKey);
                    _CACHE.put(resizedKey, result);
                    _variantKeys.put(key, resizedKey); //so a later download can invalidate it
                } else {
                    // missing file or failed decode - let the synchronous path produce
                    // its fallback (placeholder render, cached by scaleImage)
                    scaleImage(key, width, height, true, card);
                }
                finishAsyncLoad(resizedKey);
            });
        });
    }

    private static void finishAsyncLoad(final String resizedKey) {
        final List<Runnable> callbacks = pendingLoads.remove(resizedKey);
        if (callbacks != null) {
            for (final Runnable callback : callbacks) {
                callback.run();
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

        final ResolvedImageKey resolved = resolveImageKey(imageKey);
        if (resolved.fileKey == null) {
            return Pair.of(_defaultImage, true);
        }
        final IPaperCard ipc = resolved.ipc;
        final boolean altState = resolved.altState;
        final boolean useArtCrop = resolved.useArtCrop;
        final String originalKey = resolved.originalKey;
        imageKey = resolved.fileKey;

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

        original = postProcessCardImage(original, setCode, noBorder);

        // No image file exists for the given key, so draw the card face instead.
        if (original == null || useArtCrop) {
            if ((ipc != null || cardView != null) && !originalKey.equals(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD))) {
                // Drawing a card face costs ~16ms and, unlike the scaled copy, does not depend
                // on the requested display size - it is always rendered at 488x680 (times the
                // screen scale) and resampled afterwards. So cache the render itself, keyed
                // separately from any real image: a zone view opening at any size then pays
                // only the resample, and warming can render these before a size is even known.
                // Registered as a variant of the base key so that a downloaded image drops it
                // along with the scaled copies, which is what the old comment here worried about.
                final String renderKey = originalKey + RENDERED_SUFFIX;
                final BufferedImage cachedRender = _CACHE.getIfPresent(renderKey);
                if (cachedRender != null) {
                    original = cachedRender;
                } else {
                    final BufferedImage art = original;
                    float screenScale = GuiBase.getInterface().getScreenScale();
                    int width = Math.round(488 * screenScale), height = Math.round(680 * screenScale);
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
                    _CACHE.put(renderKey, original);
                    _variantKeys.put(originalKey, renderKey);
                }
            } else {
                original = _defaultImage;
            }
        }

        return Pair.of(original, isPlaceholder);
    }

    private static final class ResolvedImageKey {
        final String fileKey;      // key used to load the image file; null if the card defines no image
        final String originalKey;  // pre-artcrop key, used for full-image cache lookups in crop mode
        final IPaperCard ipc;
        final boolean altState;
        final boolean useArtCrop;

        ResolvedImageKey(final String fileKey, final String originalKey, final IPaperCard ipc, final boolean altState, final boolean useArtCrop) {
            this.fileKey = fileKey;
            this.originalKey = originalKey;
            this.ipc = ipc;
            this.altState = altState;
            this.useArtCrop = useArtCrop;
        }
    }

    private static ResolvedImageKey resolveImageKey(String imageKey) {
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
                    return new ResolvedImageKey(null, null, ipc, altState, false);
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
        return new ResolvedImageKey(imageKey, originalKey, ipc, altState, useArtCrop);
    }

    /**
     * Best-fit scales the image into (width x height) retaining aspect ratio; -1 skips
     * that dimension. Pure image work - safe to run off the EDT.
     */
    private static BufferedImage resample(final BufferedImage original, final int width, final int height, final boolean allowScaleLarger) {
        double scaleX = (-1 == width ? 1 : (double)width / original.getWidth());
        double scaleY = (-1 == height? 1 : (double)height / original.getHeight());
        double bestFitScale = Math.min(scaleX, scaleY);
        if ((bestFitScale > 1) && !allowScaleLarger) {
            bestFitScale = 1;
        }
        if (1 == bestFitScale) {
            return original;
        }
        int destWidth  = (int)(original.getWidth()  * bestFitScale);
        int destHeight = (int)(original.getHeight() * bestFitScale);

        ResampleOp resampler = new ResampleOp(destWidth, destHeight);
        return resampler.filter(original, null);
    }

    /**
     * Rounds corners / crops white borders as the display preferences dictate. Pure image
     * work on the passed instance - safe to run off the EDT.
     */
    private static BufferedImage postProcessCardImage(BufferedImage original, final String setCode, final boolean noBorder) {
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
        return original;
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

        String resizedKey = resizedKeyFor(key, cardView, width, height);

        final BufferedImage cached = _CACHE.getIfPresent(resizedKey);
        if (null != cached) {
            // A cached placeholder render must not satisfy callers probing for a real
            // image (they use the miss to decide whether to queue an online fetch).
            if (!useDefaultImage && _placeholderKeys.contains(resizedKey)) {
                return null;
            }
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

        BufferedImage result = resample(original, width, height,
                FModel.getPreferences().getPrefBoolean(FPref.UI_SCALE_LARGER));

        // Cache even placeholder renders: re-rendering and re-scaling a full card face for
        // every card on every refresh makes large zone views (tutor searches through big
        // libraries) unusably slow. The placeholder keys are tracked so the entries can be
        // invalidated when the real image finishes downloading (see clearGeneratedVariants)
        // and so image-presence probes aren't fooled by them.
        if (isPlaceholder && original != _defaultImage) {
            _placeholderKeys.add(resizedKey);
        } else {
            _placeholderKeys.remove(resizedKey);
        }
        _CACHE.put(resizedKey, result);
        _variantKeys.put(key, resizedKey);
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
