package forge.gui.download;

import com.google.common.collect.Iterables;
import forge.StaticData;
import forge.card.CardEdition;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.ImageUtil;
import forge.util.ScryfallRateLimiter;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Downloads card images for all cards matching the predicate.
 * Per face: CDN via {@link CdnUuidCache}, then the rate-limited Scryfall API, then cardforge.
 */
public class GuiDownloadFilteredCardImages extends GuiDownloadService {

    private final Predicate<PaperCard> filter;

    public GuiDownloadFilteredCardImages(Predicate<PaperCard> filter) {
        this.filter = filter;
    }

    @Override
    public String getTitle() {
        return "Download Card Images";
    }

    @Override
    protected Map<String, String> getNeededFiles() {
        final Map<String, String> downloads = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        final List<PaperCard> matches = new ArrayList<>();
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
        return downloads;
    }

    private void syncSetsOneByOne(List<String> needSync) {
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

    private static void addIfMissing(PaperCard c, String face, Map<String, String> downloads) {
        final String imageKey = ImageUtil.getImageKey(c, face, true);
        if (imageKey == null) return;

        final File destFull = new File(ForgeConstants.CACHE_CARD_PICS_DIR, imageKey + ".jpg");
        final String fbKey = TextUtil.fastReplace(imageKey, ".full", ".fullborder") +
                             (!imageKey.contains(".full") ? ".fullborder" : "");
        final File destFb = new File(ForgeConstants.CACHE_CARD_PICS_DIR, fbKey + ".jpg");

        if (destFull.exists() || destFb.exists()) return;
        if (downloads.containsKey(destFull.getAbsolutePath())) return;

        final String url = buildUrl(c, face);
        if (url == null) return;

        downloads.put(destFull.getAbsolutePath(), url);
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
