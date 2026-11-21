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
import java.util.HashMap;
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
    private final Predicate<PaperCard> filter;
    private final Predicate<CardEdition> editionFilter;
    private final boolean includeTokens;
    private final boolean overwrite;

    /**
     * @param filter        picks the cards
     * @param editionFilter says whether an edition is selected; only asked for editions that have no cards of their
     *                      own (Old Border Custom, Duelist Magazine), which no card filter can ever select
     */
    public GuiDownloadFilteredCardImages(Predicate<PaperCard> filter, Predicate<CardEdition> editionFilter,
                                         boolean includeTokens, boolean overwrite) {
        this.filter = filter;
        this.editionFilter = editionFilter;
        this.includeTokens = includeTokens;
        this.overwrite = overwrite;
    }

    @Override
    public String getTitle() {
        return "Download Card Images";
    }

    @Override
    protected int getThreadCount() {
        return PARALLEL_DOWNLOADS; // Scryfall's CDN has no rate limit; the API and paced hosts are still held back by ScryfallRateLimiter
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
        final Map<CardEdition, Boolean> tokensFromScryfall = new HashMap<>();
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
                // token sets are synced with the card sets, so tokens find the CDN even when they are not downloaded now;
                // an edition whose tokens a site other than Scryfall hosts (Old Border, Duelist) has no token set there
                if (tokensFromScryfall.computeIfAbsent(edition, GuiDownloadFilteredCardImages::usesScryfall)) {
                    scryfallSetCodes.add(edition.getTokensCode());
                }
                if (includeTokens) {
                    tokenEditions.add(edition);
                }
            }
        }

        final Set<CardEdition> tokenOnlyEditions = new HashSet<>();
        addTokenOnlyEditions(tokenOnlyEditions, editionFilter);
        for (final CardEdition edition : tokenOnlyEditions) {
            if (usesScryfall(edition)) {
                scryfallSetCodes.add(edition.getTokensCode());
            }
        }
        if (includeTokens) {
            tokenEditions.addAll(tokenOnlyEditions);
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

    /** True if the edition has token images to download: tokens or named extras, and it is not a custom set. */
    public static boolean hasTokenImages(final CardEdition edition) {
        return edition != null && edition.getType() != CardEdition.Type.CUSTOM_SET
                && !(edition.getTokens().isEmpty() && edition.getOther().isEmpty());
    }

    /**
     * Adds the editions that have token images but no cards of their own (Old Border Custom, Duelist Magazine,
     * Magic Player Rewards 2002) and that the filter accepts. No card of them can match a card filter, so the
     * tokens of such an edition are only reached this way.
     */
    public static void addTokenOnlyEditions(final Collection<CardEdition> editions, final Predicate<CardEdition> filter) {
        for (final CardEdition edition : StaticData.instance().getEditions()) {
            if (hasTokenImages(edition) && edition.getAllCardsInSet().isEmpty() && filter.test(edition)) {
                editions.add(edition);
            }
        }
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
        /** Where a site other than Scryfall hosts this token (token-images.txt), or null. */
        final String hostedUrl;

        TokenImage(final String collectorNumber, final String imageKey) {
            this.collectorNumber = collectorNumber;
            this.back = imageKey.endsWith(ImageKeys.BACKFACE_POSTFIX);
            final String filename = ImageFetcher.getTokenFilename(imageKey);
            this.file = new File(ForgeConstants.CACHE_TOKEN_PICS_DIR, filename);
            final String hosted = ImageFetcher.getHostedTokenUrl(filename);
            this.hostedUrl = hosted != null && !ImageFetcher.isHostedDownloadDisabled(hosted) ? hosted : null;
        }
    }

    /** False for an edition whose tokens are all hosted elsewhere: Scryfall has no token set for it. */
    private static boolean usesScryfall(final CardEdition edition) {
        for (final TokenImage image : getTokenImages(edition).values()) {
            if (image.hostedUrl == null) {
                return true;
            }
        }
        return false;
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

    private void addMissingTokens(final CardEdition edition, final Map<String, String> downloads) {
        for (final Map.Entry<String, TokenImage> entry : getTokenImages(edition).entrySet()) {
            final TokenImage image = entry.getValue();
            if ((!overwrite && image.file.exists()) || downloads.containsKey(entry.getKey())) continue;

            final List<String> urls = new ArrayList<>();
            if (image.hostedUrl != null) {
                urls.add(image.hostedUrl); // not a Scryfall token (Old Border, Duelist): asked first, paced by ScryfallRateLimiter
            }
            // the CDN url comes first when the token set is cached, otherwise the Scryfall API
            ImageFetcher.addScryfallTokenUrl(edition, image.collectorNumber, image.back ? "back" : "", urls);

            downloads.put(entry.getKey(), String.join(URL_SEPARATOR, urls));
        }
    }

    /**
     * The download URLs for one face, in the order to try them: CDN, then the Scryfall API in the wanted language,
     * then the API without a language (what Scryfall considers the default, for prints with no English version);
     * cardforge only if there is nothing else.
     */
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

        final List<String> urls = new ArrayList<>();

        // 1. CDN -- read-only (see CdnUuidCache.getCdnUrlIfCached()); it answers for the wanted language,
        // else English, else the language the print exists in. Only when it knows nothing about the print
        // (or the warm-up loop above didn't resolve this set) does the API below get asked.
        if (edition != null && hasCollectorNum && hasScryfallCode) {
            String cdnUrl = CdnUuidCache.getCdnUrlIfCached(
                    scryfallCode, collectorNum, langCode, face, "normal");
            if (cdnUrl != null) urls.add(cdnUrl);
        }

        // 2. Scryfall API
        if (hasCollectorNum && edition != null && hasScryfallCode) {
            String apiPath = ImageUtil.getScryfallDownloadUrl(
                    c, face, scryfallCode, langCode, false);
            if (apiPath != null) {
                urls.add(ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD + apiPath);
                String defaultLangPath = ImageUtil.getScryfallDownloadUrl(c, face, scryfallCode, "", false);
                if (defaultLangPath != null && !defaultLangPath.equals(apiPath)) {
                    urls.add(ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD + defaultLangPath);
                }
            }
        }

        // 3. Cardforge
        if (urls.isEmpty()) {
            String cardforgeUrl = ImageUtil.getDownloadUrl(c, face);
            if (cardforgeUrl != null) urls.add(ForgeConstants.URL_PIC_DOWNLOAD + cardforgeUrl);
        }
        return urls.isEmpty() ? null : String.join(URL_SEPARATOR, urls);
    }
}
