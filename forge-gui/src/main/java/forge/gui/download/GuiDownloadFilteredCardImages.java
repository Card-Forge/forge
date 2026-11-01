package forge.gui.download;

import com.google.common.collect.Iterables;
import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.item.PaperToken;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.ImageFetcher;
import forge.util.ImageUtil;
import forge.util.ScryfallRateLimiter;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Downloads card images for all cards matching the predicate.
 * Per face: CDN via {@link CdnUuidCache}, then the rate-limited Scryfall API, then cardforge.
 * Optionally also downloads the tokens of the sets those cards belong to, and can replace images
 * that already exist.
 */
public class GuiDownloadFilteredCardImages extends GuiDownloadService {
    // Scryfall's image CDN (*.scryfall.io) has no rate limit, and requests to the API are still
    // paced by ScryfallRateLimiter, so the downloads can run side by side.
    // Speed grows with the worker count until the connection is full, and the workers needed for that are about
    // 1.35 x (images per second the connection carries) x (round trip + time to first byte). Measured through an
    // emulated network with 90 KB images: 20 workers reach the full speed of every path up to 100 Mbit at 100 ms
    // or 300 Mbit at 30 ms (16 reach only 85-92% there) and more workers add nothing, only longer or faster paths gain.
    private static final int DOWNLOAD_THREADS = 20;

    private final Predicate<PaperCard> filter;
    private final boolean includeTokens;
    private final boolean overwrite;

    public GuiDownloadFilteredCardImages(Predicate<PaperCard> filter, boolean includeTokens, boolean overwrite) {
        this.filter = filter;
        this.includeTokens = includeTokens;
        this.overwrite = overwrite;
    }

    @Override
    public String getTitle() {
        return "Download Card Images";
    }

    @Override
    protected int getThreadCount() {
        return DOWNLOAD_THREADS;
    }

    @Override
    public void run() {
        super.run();
        if (overwrite) {
            // images were replaced on disk, so drop the copies that are already loaded
            FThreads.invokeInEdtLater(() -> GuiBase.getInterface().clearImageCache());
        }
    }

    @Override
    protected Map<String, String> getNeededFiles() throws UnsupportedEncodingException {
        final Map<String, String> downloads = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        final List<PaperCard> matches = new ArrayList<>();
        final Set<CardEdition> tokenEditions = new HashSet<>();
        final TreeSet<String> scryfallSetCodes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (final PaperCard c : Iterables.concat(
                FModel.getMagicDb().getCommonCards().getAllCards(),
                FModel.getMagicDb().getVariantCards().getAllCards())) {

            if (!filter.test(c)) { continue; }

            final String setCode3 = c.getEdition();
            if (StringUtils.isBlank(setCode3) || CardEdition.UNKNOWN_CODE.equals(setCode3)) { continue; }

            matches.add(c);
            CardEdition edition = StaticData.instance().getEditions().get(setCode3);
            if (edition != null && !StringUtils.isBlank(edition.getScryfallCode())) {
                scryfallSetCodes.add(edition.getScryfallCode());
            }
            if (hasTokenImages(edition)) {
                // token sets are synced with the card sets, so tokens find the CDN even when they are not downloaded now
                scryfallSetCodes.add(edition.getTokensCode());
                if (includeTokens) {
                    tokenEditions.add(edition);
                }
            }
        }

        // Warm the CDN UUID cache for every needed set before resolving URLs below, so this run
        // prefers the unthrottled CDN path over the rate-limited Scryfall API. Skip sets already
        // cached (e.g. by a prior bulk-data sync).
        final List<String> needSync = new ArrayList<>();
        for (String setCode : scryfallSetCodes) {
            if (!CdnUuidCache.isSetCached(setCode)) {
                needSync.add(setCode);
            }
        }
        if (needSync.isEmpty() && !scryfallSetCodes.isEmpty()) {
            reportStatus("All " + scryfallSetCodes.size() + " needed sets already cached.");
        } else if (!needSync.isEmpty()) {
            int alreadyCached = scryfallSetCodes.size() - needSync.size();
            if (alreadyCached > 0) {
                reportStatus(alreadyCached + "/" + scryfallSetCodes.size() + " sets already cached; syncing the remaining " + needSync.size() + "...");
            }
        }

        // Syncing sets one at a time against the rate-limited Scryfall API is fine for a handful
        // of sets, but with a cold cache (dozens/hundreds of missing sets) it can take minutes of
        // sequential requests while barely updating the UI. Past a threshold, do it in one shot
        // via the bulk-data export instead -- the same mechanism as the "Descarga masiva" button.
        final int BULK_SYNC_THRESHOLD = 15;
        if (needSync.size() > BULK_SYNC_THRESHOLD) {
            reportStatus(needSync.size() + " sets need syncing; using bulk data sync instead of "
                    + "one-by-one requests...");
            int setCount = ScryfallBulkDataSync.sync(ScryfallBulkDataSync.BULK_TYPE_DEFAULT_CARDS, null,
                    (message, fraction) -> reportStatus(message), () -> cancel);
            if (setCount < 0 && !cancel) {
                reportStatus("Bulk sync failed; falling back to per-set sync...");
                syncSetsOneByOne(needSync);
            }
        } else {
            syncSetsOneByOne(needSync);
        }

        for (final PaperCard c : matches) {
            addIfMissing(c, "", downloads);
            if (c.hasBackFace()) {
                addIfMissing(c, "back", downloads);
            }
        }

        for (final CardEdition edition : tokenEditions) {
            addMissingTokens(edition, downloads);
        }
        return downloads;
    }

    private void syncSetsOneByOne(List<String> needSync) throws UnsupportedEncodingException {
        int setIndex = 0;
        for (String setCode : needSync) {
            if (cancel) break;
            setIndex++;
            reportStatus("Syncing card data from an online source: set " + setIndex + "/" + needSync.size()
                    + " (" + setCode.toUpperCase() + ")...");
            ScryfallRateLimiter.awaitCooldownCleared(() -> cancel, this::reportStatus);
            if (cancel) break;
            ScryfallSetSync.sync(setCode);
        }
    }

    // -------------------------------------------------------------------------

    private void addIfMissing(PaperCard c, String face, Map<String, String> downloads) {
        final String imageKey = ImageUtil.getImageKey(c, face, true);
        if (imageKey == null) return;

        final File destFull = new File(ForgeConstants.CACHE_CARD_PICS_DIR, imageKey + ".jpg");
        final String fbKey = TextUtil.fastReplace(imageKey, ".full", ".fullborder") +
                             (!imageKey.contains(".full") ? ".fullborder" : "");
        final File destFb = new File(ForgeConstants.CACHE_CARD_PICS_DIR, fbKey + ".jpg");

        if (!overwrite && (destFull.exists() || destFb.exists())) return;
        if (downloads.containsKey(destFull.getAbsolutePath())) return;
        if (downloads.containsKey(destFb.getAbsolutePath())) return;

        final String url = buildUrl(c, face);
        if (url == null) return;

        // ImageKeys loads .full before .fullborder, so that is the file to replace if it is there
        downloads.put((overwrite && destFull.exists() ? destFull : destFb).getAbsolutePath(), url);
    }

    /** True if the edition has token images to download: tokens or named extras, in a set that Scryfall knows. */
    public static boolean hasTokenImages(final CardEdition edition) {
        return edition != null && edition.getType() != CardEdition.Type.CUSTOM_SET
                && !(edition.getTokens().isEmpty() && edition.getOther().isEmpty());
    }

    /** How many token images the editions have: {total, downloaded, missing}. */
    public static int[] countTokenImages(final Collection<CardEdition> editions) {
        int total = 0, downloaded = 0;
        for (final CardEdition edition : editions) {
            for (final TokenImage image : getTokenImages(edition).values()) {
                total++;
                if (image.file.exists()) {
                    downloaded++;
                }
            }
        }
        return new int[] {total, downloaded, total - downloaded};
    }

    /** A token image of an edition and the file it is saved as. */
    private static final class TokenImage {
        final String collectorNumber;
        final boolean back;
        final File file;

        TokenImage(final String collectorNumber, final String imageKey) {
            this.collectorNumber = collectorNumber;
            this.back = imageKey.endsWith(ImageKeys.BACKFACE_POSTFIX);
            this.file = new File(ForgeConstants.CACHE_TOKEN_PICS_DIR, ImageFetcher.getTokenFilename(imageKey));
        }
    }

    /**
     * The token images of an edition, by file: its token list (with the back faces of double-faced tokens) and
     * its named extras, i.e. what StaticData.getOtherImageKey finds for a token that is a copy of a card
     * (CopyPermanent with DefinedName, like Tarmogoyf or Mutavault) or for the Monarch and the like.
     */
    private static Map<String, TokenImage> getTokenImages(final CardEdition edition) {
        final Map<String, TokenImage> images = new LinkedHashMap<>();
        final Map<String, CardRules> tokenRules = StaticData.instance().getAllTokens().getRules();
        for (final CardEdition.EditionEntry t : edition.getTokens().values()) {
            if (StringUtils.isBlank(t.collectorNumber())) continue;

            addTokenImage(images, t.collectorNumber(), tokenKey(edition, t));
            final CardRules rules = tokenRules.get(t.name());
            if (rules != null) {
                final PaperToken token = new PaperToken(rules, edition, t.name(), t.collectorNumber(), t.artistName());
                if (token.hasBackFace()) {
                    addTokenImage(images, t.collectorNumber(), token.getCardAltImageKey());
                }
            }
        }
        for (final CardEdition.EditionEntry o : edition.getOther().values()) {
            if (StringUtils.isBlank(o.collectorNumber())) continue;

            addTokenImage(images, o.collectorNumber(), tokenKey(edition, o));
        }
        return images;
    }

    /** Same key format as StaticData.getOtherImageKey. */
    private static String tokenKey(final CardEdition edition, final CardEdition.EditionEntry entry) {
        return ImageKeys.getTokenKey(String.format("%s|%s|%s", entry.name(), edition.getCode(), entry.collectorNumber()));
    }

    private static void addTokenImage(final Map<String, TokenImage> images, final String collectorNumber, final String imageKey) {
        final TokenImage image = new TokenImage(collectorNumber, imageKey);
        images.putIfAbsent(image.file.getAbsolutePath(), image);
    }

    void addMissingTokens(final CardEdition edition, final Map<String, String> downloads) {
        for (final Map.Entry<String, TokenImage> entry : getTokenImages(edition).entrySet()) {
            final TokenImage image = entry.getValue();
            if ((!overwrite && image.file.exists()) || downloads.containsKey(entry.getKey())) continue;

            final List<String> urls = new ArrayList<>();
            ImageFetcher.addScryfallTokenUrl(edition, image.collectorNumber, image.back ? "back" : "", urls);

            // the CDN url comes first when the token set is cached, otherwise the Scryfall API
            downloads.put(entry.getKey(), urls.get(0));
        }
    }

    /** Best available download URL for one face: CDN, then Scryfall API, then cardforge. */
    private static String buildUrl(PaperCard c, String face) {
        final String collectorNum = c.getCollectorNumber();
        final boolean hasCollectorNum = !IPaperCard.NO_COLLECTOR_NUMBER.equals(collectorNum)
                && !"0".equals(collectorNum)
                && !StringUtils.isBlank(collectorNum);

        CardEdition edition = hasCollectorNum
                ? StaticData.instance().getEditions().get(c.getEdition()) : null;
        String scryfallCode = (edition != null) ? edition.getScryfallCode() : null;
        boolean hasScryfallCode = !StringUtils.isBlank(scryfallCode);
        String preferredLang = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_CARD_DOWNLOAD_LANG);
        String langCode = (edition != null && hasScryfallCode)
                ? CdnUuidCache.resolvePreferredLangCode(preferredLang, scryfallCode, collectorNum, edition.getCardsLangCode())
                : null;

        // 1. CDN -- read-only (see CdnUuidCache.getCdnUrlIfCached()); falls through to the API
        // below if the warm-up loop above didn't resolve this set.
        if (edition != null && hasCollectorNum && hasScryfallCode) {
            String cdnUrl = CdnUuidCache.getCdnUrlIfCached(
                    scryfallCode, collectorNum, langCode, face, "normal");
            if (cdnUrl != null) return cdnUrl;
        }

        // 2. Scryfall API
        if (hasCollectorNum && edition != null && hasScryfallCode) {
            String apiPath = ImageUtil.getScryfallDownloadUrl(
                    c, face, scryfallCode, langCode, false);
            if (apiPath != null) return ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD + apiPath;
        }

        // 3. Cardforge
        String cardforgeUrl = ImageUtil.getDownloadUrl(c, face);
        return cardforgeUrl != null ? ForgeConstants.URL_PIC_DOWNLOAD + cardforgeUrl : null;
    }
}
