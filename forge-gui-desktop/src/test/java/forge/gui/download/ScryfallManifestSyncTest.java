package forge.gui.download;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exercises {@link ScryfallManifestSync} against a tiny embedded HTTP server standing
 * in for {@code api.scryfall.com/cards/manifest}, so these run offline and don't
 * depend on Scryfall's real rate limit or live catalog.
 */
@Test(groups = {"UnitTest"})
public class ScryfallManifestSyncTest {

    private HttpServer server;
    private File localCacheDir;
    private final AtomicInteger requestCount = new AtomicInteger();

    /** Pages served for the current test, keyed by 1-based page number. */
    private final Map<Integer, String> pages = new HashMap<>();

    @BeforeClass
    public void setUp() throws IOException {
        localCacheDir = Files.createTempDirectory("manifest_local").toFile();
        CdnUuidCache.localCacheDirOverride = localCacheDir.getAbsolutePath() + File.separator;

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        ScryfallManifestSync.manifestBaseUrlOverride = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterClass
    public void tearDown() {
        server.stop(0);
        CdnUuidCache.localCacheDirOverride = null;
        ScryfallManifestSync.manifestBaseUrlOverride = null;
    }

    @AfterMethod
    public void resetBetweenTests() {
        CdnUuidCache.clearCache();
        pages.clear();
        requestCount.set(0);
        for (File f : localCacheDir.listFiles()) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }

    private void handle(HttpExchange ex) throws IOException {
        requestCount.incrementAndGet();
        Map<String, String> params = queryParams(ex.getRequestURI().getRawQuery());
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        String body = pages.getOrDefault(page, "{\"object\":\"list\",\"has_more\":false,\"data\":[]}");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, bytes.length);
        try (var os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> queryParams(String rawQuery) {
        Map<String, String> out = new HashMap<>();
        if (rawQuery == null) return out;
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            out.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return out;
    }

    private static String entry(String id, String set, String cn, String lang, String imageUpdatedAt) {
        return String.format(
                "{\"id\":\"%s\",\"set_code\":\"%s\",\"collector_number\":\"%s\",\"lang\":\"%s\",\"image_updated_at\":%s}",
                id, set, cn, lang, imageUpdatedAt == null ? "null" : "\"" + imageUpdatedAt + "\"");
    }

    private static String page(boolean hasMore, String... entries) {
        return "{\"object\":\"list\",\"has_more\":" + hasMore + ",\"data\":[" + String.join(",", entries) + "]}";
    }

    // -------------------------------------------------------------------------

    @Test
    public void singlePage_mergesAllEntriesAndResolvesViaCdnUuidCache() throws Exception {
        pages.put(1, page(false,
                entry("11111111-1111-1111-1111-111111111111", "neo", "1", "en", "2026-01-01T00:00:00Z"),
                entry("22222222-2222-2222-2222-222222222222", "neo", "2", "en", "2026-01-01T00:00:00Z")));

        int merged = ScryfallManifestSync.sync("en", null);

        Assert.assertEquals(merged, 2);
        String url = CdnUuidCache.getCdnUrl("neo", "1", "en", "front", "normal");
        Assert.assertEquals(url, ScryfallBulkData.cdnUrl("11111111-1111-1111-1111-111111111111", "front", "normal"));
    }

    @Test
    public void dfcWithoutSeparateBackId_backFaceReusesFrontUuid() throws Exception {
        pages.put(1, page(false,
                entry("33333333-3333-3333-3333-333333333333", "neo", "5", "en", "2026-01-01T00:00:00Z")));

        ScryfallManifestSync.sync("en", null);

        String back = CdnUuidCache.getCdnUrl("neo", "5", "en", "back", "normal");
        Assert.assertEquals(back, ScryfallBulkData.cdnUrl("33333333-3333-3333-3333-333333333333", "back", "normal"),
                "manifest-derived entries should assume back shares the front's id");
    }

    @Test
    public void entriesWithoutImageUpdatedAt_areSkipped() throws Exception {
        pages.put(1, page(false, entry("44444444-4444-4444-4444-444444444444", "neo", "9", "en", null)));

        int merged = ScryfallManifestSync.sync("en", null);

        Assert.assertEquals(merged, 0);
        Assert.assertNull(CdnUuidCache.getCdnUrl("neo", "9", "en", "front", "normal"));
    }

    @Test
    public void multiplePages_allMergedAndFollowsPagination() throws Exception {
        pages.put(1, page(true, entry("aaaaaaaa-0000-0000-0000-000000000001", "one", "1", "en", "2026-03-03T00:00:00Z")));
        pages.put(2, page(false, entry("aaaaaaaa-0000-0000-0000-000000000002", "two", "1", "en", "2026-03-01T00:00:00Z")));

        int merged = ScryfallManifestSync.sync("en", null);

        Assert.assertEquals(merged, 2);
        Assert.assertNotNull(CdnUuidCache.getCdnUrl("one", "1", "en", "front", "normal"));
        Assert.assertNotNull(CdnUuidCache.getCdnUrl("two", "1", "en", "front", "normal"));
    }

    @Test
    public void secondSync_withNoNewData_stopsAfterFirstPage() throws Exception {
        pages.put(1, page(false, entry("bbbbbbbb-0000-0000-0000-000000000001", "neo", "1", "en", "2026-01-01T00:00:00Z")));
        ScryfallManifestSync.sync("en", null); // establishes the watermark
        requestCount.set(0);

        int merged = ScryfallManifestSync.sync("en", null);

        Assert.assertEquals(merged, 0, "everything is at or before the stored watermark");
        Assert.assertEquals(requestCount.get(), 1, "should not paginate past the page containing only known data");
    }

    @Test
    public void incrementalSync_onlyMergesEntriesNewerThanWatermark() throws Exception {
        pages.put(1, page(false, entry("cccccccc-0000-0000-0000-000000000001", "neo", "1", "en", "2026-01-01T00:00:00Z")));
        ScryfallManifestSync.sync("en", null);

        // A later sync: one new card (newer image update), then the previously-seen card.
        pages.put(1, page(false,
                entry("dddddddd-0000-0000-0000-000000000002", "neo", "2", "en", "2026-02-01T00:00:00Z"),
                entry("cccccccc-0000-0000-0000-000000000001", "neo", "1", "en", "2026-01-01T00:00:00Z")));

        int merged = ScryfallManifestSync.sync("en", null);

        Assert.assertEquals(merged, 1, "only the entry newer than the watermark should be merged");
        Assert.assertNotNull(CdnUuidCache.getCdnUrl("neo", "2", "en", "front", "normal"));
    }

    @Test
    public void merge_neverOverwritesExistingPrecomputedEntry() throws Exception {
        // Simulate forge-extras data already on disk with a genuine distinct back-face UUID.
        Map<String, String> cn1 = new HashMap<>();
        cn1.put("en", "eeeeeeee-0000-0000-0000-000000000001"); // pre-existing, precise front id
        Map<String, Map<String, String>> existing = new HashMap<>();
        existing.put("1", cn1);
        CdnUuidCache.mergeSetEntries("neo", existing);

        // Manifest sync sees a *different* id for the same card/lang -- must not clobber it.
        pages.put(1, page(false, entry("ffffffff-0000-0000-0000-000000000009", "neo", "1", "en", "2026-01-01T00:00:00Z")));
        ScryfallManifestSync.sync("en", null);

        String url = CdnUuidCache.getCdnUrl("neo", "1", "en", "front", "normal");
        Assert.assertEquals(url, ScryfallBulkData.cdnUrl("eeeeeeee-0000-0000-0000-000000000001", "front", "normal"),
                "pre-existing entries must win over a manifest-derived guess");
    }

    @Test
    public void mergeSetEntries_fillsGapsAlongsideExistingLanguages() {
        Map<String, String> cn1en = new HashMap<>();
        cn1en.put("en", "11111111-0000-0000-0000-000000000001");
        Map<String, Map<String, String>> first = new HashMap<>();
        first.put("1", cn1en);
        CdnUuidCache.mergeSetEntries("neo", first);

        Map<String, String> cn1ja = new HashMap<>();
        cn1ja.put("ja", "22222222-0000-0000-0000-000000000002");
        Map<String, Map<String, String>> second = new HashMap<>();
        second.put("1", cn1ja);
        CdnUuidCache.mergeSetEntries("neo", second);

        Assert.assertNotNull(CdnUuidCache.getCdnUrl("neo", "1", "en", "front", "normal"),
                "the earlier language entry must survive a later merge");
        Assert.assertEquals(CdnUuidCache.getCdnUrl("neo", "1", "ja", "front", "normal"),
                ScryfallBulkData.cdnUrl("22222222-0000-0000-0000-000000000002", "front", "normal"));
    }

    @Test
    public void clearCache_removesMergedDataAndDiskFiles() throws Exception {
        pages.put(1, page(false, entry("99999999-0000-0000-0000-000000000001", "neo", "1", "en", "2026-01-01T00:00:00Z")));
        ScryfallManifestSync.sync("en", null);
        Assert.assertNotNull(CdnUuidCache.getCdnUrl("neo", "1", "en", "front", "normal"));

        CdnUuidCache.clearCache();

        Assert.assertNull(CdnUuidCache.getCdnUrl("neo", "1", "en", "front", "normal"));
        Assert.assertEquals(new File(localCacheDir, "neo.json.gz").exists(), false);
    }

    @Test
    public void progressListener_calledWithDoneOnLastPageOnly() throws Exception {
        pages.put(1, page(true, entry("12121212-0000-0000-0000-000000000001", "one", "1", "en", "2026-03-03T00:00:00Z")));
        pages.put(2, page(false, entry("13131313-0000-0000-0000-000000000002", "two", "1", "en", "2026-03-01T00:00:00Z")));

        List<Boolean> doneFlags = new ArrayList<>();
        List<Integer> pageNumbers = new ArrayList<>();
        ScryfallManifestSync.sync("en", (page, merged, done) -> {
            pageNumbers.add(page);
            doneFlags.add(done);
        });

        Assert.assertEquals(pageNumbers, List.of(1, 2));
        Assert.assertEquals(doneFlags, List.of(false, true));
    }
}
