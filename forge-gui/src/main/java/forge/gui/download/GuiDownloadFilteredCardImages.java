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
 * Uses Scryfall as the primary source (matching the auto-downloader path) so
 * that images are actually available; falls back to the cardforge hosted server
 * for cards that lack a collector number.
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
        if (imageKey == null) { return; }

        // Destination path for this card face in the local cache
        final File destFull   = new File(ForgeConstants.CACHE_CARD_PICS_DIR, imageKey + ".jpg");
        // Also check for the fullborder variant that LibGDXImageFetcher produces from Scryfall
        final String fbKey    = TextUtil.fastReplace(imageKey, ".full", ".fullborder") +
                                (!imageKey.contains(".full") ? ".fullborder" : "") ;
        final File destFb     = new File(ForgeConstants.CACHE_CARD_PICS_DIR, fbKey + ".jpg");

        if (destFull.exists() || destFb.exists()) { return; }
        if (downloads.containsKey(destFull.getAbsolutePath())) { return; }

        final String url = buildUrl(c, face);
        if (url == null) { return; }

        downloads.put(destFull.getAbsolutePath(), url);
    }

    /**
     * Builds the download URL for one card face.
     * Prefers Scryfall (which works) for cards that have a collector number;
     * falls back to the cardforge hosted server otherwise.
     */
    private static String buildUrl(PaperCard c, String face) {
        final String collectorNum = c.getCollectorNumber();
        final boolean hasCollectorNum = !IPaperCard.NO_COLLECTOR_NUMBER.equals(collectorNum)
                && !"0".equals(collectorNum)
                && !StringUtils.isBlank(collectorNum);

        if (hasCollectorNum) {
            CardEdition edition = StaticData.instance().getEditions().get(c.getEdition());
            if (edition != null) {
                String scryfallCode = edition.getScryfallCode();
                if (!StringUtils.isBlank(scryfallCode)) {
                    String langCode = edition.getCardsLangCode();
                    String path = ImageUtil.getScryfallDownloadUrl(c, face, scryfallCode, langCode, false);
                    if (path != null) {
                        return ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD + path;
                    }
                }
            }
        }

        // Fallback: cardforge hosted server
        String cardforgeUrl = ImageUtil.getDownloadUrl(c, face);
        return cardforgeUrl != null ? ForgeConstants.URL_PIC_DOWNLOAD + cardforgeUrl : null;
    }
}
