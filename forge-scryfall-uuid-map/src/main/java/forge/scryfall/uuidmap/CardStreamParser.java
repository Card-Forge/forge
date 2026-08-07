package forge.scryfall.uuidmap;

import com.google.gson.stream.JsonReader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Streams a Scryfall bulk JSON file (all_cards or default_cards) and emits
 * a {@link CardRecord} for every card entry that carries image data.
 *
 * <p>Uses Gson's streaming {@link JsonReader} so the 2.5 GB file is never
 * fully loaded into memory — only one object at a time is in heap.
 */
public final class CardStreamParser {

    private CardStreamParser() {}

    /**
     * Parses {@code bulkFile} and calls {@code consumer} for every record with image data.
     *
     * @return number of records emitted to the consumer
     */
    public static long parse(Path bulkFile, Consumer<CardRecord> consumer) throws IOException {
        long written = 0L;
        long skipped = 0L;
        try (JsonReader reader = new JsonReader(new BufferedReader(
                new InputStreamReader(new FileInputStream(bulkFile.toFile()), StandardCharsets.UTF_8),
                1 << 20 /* 1 MB read buffer */))) {
            reader.beginArray();
            while (reader.hasNext()) {
                CardRecord record = readCard(reader);
                if (record != null) {
                    consumer.accept(record);
                    written++;
                } else {
                    skipped++;
                }
                long total = written + skipped;
                if (total % 50_000 == 0) {
                    System.err.printf("  %,d processed  (%,d written, %,d skipped)%n",
                            total, written, skipped);
                }
            }
            reader.endArray();
        }
        System.err.printf("  Done: %,d written, %,d skipped (no image)%n", written, skipped);
        return written;
    }

    private static CardRecord readCard(JsonReader reader) throws IOException {
        String id = null;
        String set = null;
        String cn = null;
        String lang = null;
        String frontUrl = null;
        String backUrl = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String field = reader.nextName();
            switch (field) {
                case "id":               id   = reader.nextString(); break;
                case "set":              set  = reader.nextString(); break;
                case "collector_number": cn   = reader.nextString(); break;
                case "lang":             lang = reader.nextString(); break;
                case "image_uris":
                    frontUrl = readNormalUrl(reader);
                    break;
                case "card_faces": {
                    String[] urls = readFaceUrls(reader);
                    frontUrl = urls[0];
                    backUrl  = urls[1];
                    break;
                }
                default: reader.skipValue(); break;
            }
        }
        reader.endObject();

        if (id == null || set == null || cn == null || lang == null || frontUrl == null) {
            return null;
        }

        return new CardRecord(
                set, cn, lang,
                uuidFromUrl(frontUrl, id),
                backUrl != null ? uuidFromUrl(backUrl, id) : null);
    }

    /** Reads an {@code image_uris} object and returns the value of the {@code normal} key. */
    private static String readNormalUrl(JsonReader reader) throws IOException {
        String normal = null;
        reader.beginObject();
        while (reader.hasNext()) {
            if ("normal".equals(reader.nextName())) {
                normal = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return normal;
    }

    /** Reads a {@code card_faces} array and returns {@code [front_normal_url, back_normal_url]}. */
    private static String[] readFaceUrls(JsonReader reader) throws IOException {
        String[] urls = new String[2];
        int idx = 0;
        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            while (reader.hasNext()) {
                String field = reader.nextName();
                if ("image_uris".equals(field) && idx < 2) {
                    urls[idx] = readNormalUrl(reader);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            idx++;
        }
        reader.endArray();
        return urls;
    }

    /**
     * Extracts the UUID segment from a Scryfall CDN image URL.
     *
     * <p>URL format: {@code https://cards.scryfall.io/normal/front/4/e/{uuid}.jpg?timestamp}
     *
     * <p>Parsing the UUID from the URL (rather than using the card's {@code id} field directly)
     * correctly handles the two Secret Lair DFC cards where both faces share an artwork UUID
     * that differs from the card's own {@code id}.
     *
     * <p>Falls back to {@code cardId} for non-CDN URLs such as
     * {@code errors.scryfall.com/soon.jpg} (placeholder for missing images).
     */
    static String uuidFromUrl(String url, String cardId) {
        if (url == null || !url.contains("cards.scryfall.io")) {
            return cardId;
        }
        int qmark = url.indexOf('?');
        String path = qmark >= 0 ? url.substring(0, qmark) : url;
        int slash = path.lastIndexOf('/');
        String filename = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }
}
