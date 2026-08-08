package forge.gui.download;

import com.google.common.collect.Iterables;
import forge.StaticData;
import forge.card.CardEdition;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.ImageUtil;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Downloads card images for all cards that match the supplied predicate.
 *
 * URL priority per card face:
 *  1. cards.scryfall.io CDN (not rate-limited) — when a UUID JSON file exists at
 *     {@code res/cdn_uuid/{scryfallCode}/{collectorNumber}.json} for this card
 *  2. api.scryfall.com per-card API (rate-limited, 100 ms/request) — fallback when
 *     no UUID is available but the card has a collector number
 *  3. cardforge hosted server — final fallback
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

        for (final PaperCard c : Iterables.concat(
                FModel.getMagicDb().getCommonCards().getAllCards(),
                FModel.getMagicDb().getVariantCards().getAllCards())) {

            if (!filter.test(c)) { continue; }

            final String setCode3 = c.getEdition();
            if (StringUtils.isBlank(setCode3) || CardEdition.UNKNOWN_CODE.equals(setCode3)) { continue; }

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

    /**
     * Returns the best available download URL for one card face.
     *
     * Priority:
     *  1. cards.scryfall.io CDN URL from cdn_uuid JSON file (no rate limit; optional assets)
     *  2. api.scryfall.com per-card API URL (rate-limited; GuiDownloadService
     *     enforces 100 ms between requests to api.scryfall.com URLs automatically)
     *  3. cardforge hosted server
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

        // 1. CDN — fast, no rate limit; requires cdn_uuid JSON files in assets
        if (edition != null && hasCollectorNum && hasScryfallCode) {
            String cdnUrl = CdnUuidCache.getCdnUrl(
                    scryfallCode, collectorNum, edition.getCardsLangCode(), face, "normal");
            if (cdnUrl != null) return cdnUrl;
        }

        // 2. Scryfall per-card API — rate-limited (100 ms/request via GuiDownloadService)
        if (hasCollectorNum && edition != null && hasScryfallCode) {
            String apiPath = ImageUtil.getScryfallDownloadUrl(
                    c, face, scryfallCode, edition.getCardsLangCode(), false);
            if (apiPath != null) return ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD + apiPath;
        }

        // 3. Cardforge hosted server
        String cardforgeUrl = ImageUtil.getDownloadUrl(c, face);
        return cardforgeUrl != null ? ForgeConstants.URL_PIC_DOWNLOAD + cardforgeUrl : null;
    }
}
