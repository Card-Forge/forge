package forge.gui.download;

import com.google.common.collect.Iterables;
import forge.StaticData;
import forge.card.CardEdition;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
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
        // uses the unthrottled CDN path instead of falling back to the rate-limited Scryfall API
        // just because the cache hadn't finished populating yet. Skip a set that's already
        // cached (e.g. from a bulk-data sync via ScryfallBulkDataSync) instead of unconditionally
        // re-fetching it through the rate-limited /cards/search endpoint -- otherwise the bulk
        // sync's whole point (avoiding that endpoint) is defeated on every subsequent download.
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

        int setIndex = 0;
        for (String setCode : needSync) {
            if (cancel) break;
            setIndex++;
            reportStatus("Syncing card data from Scryfall: set " + setIndex + "/" + needSync.size()
                    + " (" + setCode.toUpperCase() + ")...");
            ScryfallRateLimiter.awaitCooldownCleared(() -> cancel, this::reportStatus);
            if (cancel) break;
            ScryfallSetSync.sync(setCode);
        }

        for (final PaperCard c : matches) {
            addIfMissing(c, "", downloads);
            if (c.hasBackFace()) {
                addIfMissing(c, "back", downloads);
            }
        }
        return downloads;
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

        // 1. CDN -- read-only: the warm-up loop above is the one deliberate, sequential place
        // that syncs sets. If a set's warm-up sync failed to produce data, fall through to the
        // API below for this run rather than triggering another uncoordinated background sync
        // (see CdnUuidCache.getCdnUrlIfCached()).
        if (edition != null && hasCollectorNum && hasScryfallCode) {
            String cdnUrl = CdnUuidCache.getCdnUrlIfCached(
                    scryfallCode, collectorNum, edition.getCardsLangCode(), face, "normal");
            if (cdnUrl != null) return cdnUrl;
        }

        // 2. Scryfall API
        if (hasCollectorNum && edition != null && hasScryfallCode) {
            String apiPath = ImageUtil.getScryfallDownloadUrl(
                    c, face, scryfallCode, edition.getCardsLangCode(), false);
            if (apiPath != null) return ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD + apiPath;
        }

        // 3. Cardforge
        String cardforgeUrl = ImageUtil.getDownloadUrl(c, face);
        return cardforgeUrl != null ? ForgeConstants.URL_PIC_DOWNLOAD + cardforgeUrl : null;
    }
}
