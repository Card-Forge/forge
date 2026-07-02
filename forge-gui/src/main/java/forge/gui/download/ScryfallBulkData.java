package forge.gui.download;

/**
 * Utility for constructing Scryfall CDN image URLs from card UUIDs.
 *
 * <p>The CDN ({@code cards.scryfall.io}) is not rate-limited. Given a UUID and image
 * size, the URL is fully deterministic:
 * <pre>
 *   https://cards.scryfall.io/{size}/{front|back}/{uuid[0]}/{uuid[1]}/{uuid}.jpg
 * </pre>
 * where {@code size} is {@code "normal"} or {@code "art_crop"}.
 *
 * <p>UUIDs are loaded from {@code res/cdn_uuid/{setCode}/{collectorNumber}.json} asset files
 * by {@link CdnUuidCache}.
 */
public final class ScryfallBulkData {

    private ScryfallBulkData() {}

    /**
     * Builds a Scryfall CDN image URL.
     *
     * @param uuid  the Scryfall card UUID (e.g. {@code "4e7a547f-..."})
     * @param side  {@code "front"} or {@code "back"}
     * @param size  {@code "normal"} or {@code "art_crop"}
     */
    public static String cdnUrl(String uuid, String side, String size) {
        return "https://cards.scryfall.io/" + size + "/" + side
                + "/" + uuid.charAt(0) + "/" + uuid.charAt(1) + "/" + uuid + ".jpg";
    }
}
