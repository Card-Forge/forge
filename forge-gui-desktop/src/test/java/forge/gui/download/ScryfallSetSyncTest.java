package forge.gui.download;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

/**
 * Exercises {@link ScryfallSetSync} against an embedded HTTP server standing in for
 * {@code api.scryfall.com/cards/search}, offline. {@code autoSyncEnabled} is disabled so
 * tests drive {@link CdnUuidCache#syncPendingSets} synchronously instead of racing a real
 * background submission.
 */
@Test(groups = {"UnitTest"})
public class ScryfallSetSyncTest {

    private HttpServer server;
    private String baseUrl;
    private File localCacheDir;
    private final AtomicInteger requestCount = new AtomicInteger();
    private volatile String lastQuery;

    /** Pages served for the current test, keyed by 1-based page number. */
    private final Map<Integer, String> pages = new HashMap<>();

    @BeforeClass
    public void setUp() throws IOException {
        localCacheDir = Files.createTempDirectory("setsync_local").toFile();
        CdnUuidCache.localCacheDirOverride = localCacheDir.getAbsolutePath() + File.separator;
        CdnUuidCache.autoSyncEnabled = false;

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        ScryfallSetSync.searchBaseUrlOverride = baseUrl;
    }

    @AfterClass
    public void tearDown() {
        server.stop(0);
        CdnUuidCache.localCacheDirOverride = null;
        CdnUuidCache.autoSyncEnabled = true;
        ScryfallSetSync.searchBaseUrlOverride = null;
    }

    @AfterMethod
    public void resetBetweenTests() {
        CdnUuidCache.clearCacheForTesting();
        pages.clear();
        requestCount.set(0);
        lastQuery = null;
        for (File f : localCacheDir.listFiles()) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }

    private void handle(HttpExchange ex) throws IOException {
        requestCount.incrementAndGet();
        lastQuery = ex.getRequestURI().getRawQuery();
        Map<String, String> params = queryParams(lastQuery);
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        String body = pages.get(page);
        byte[] bytes;
        int status;
        if (body == null) {
            bytes = "{\"object\":\"error\",\"status\":404,\"details\":\"no cards found\"}"
                    .getBytes(StandardCharsets.UTF_8);
            status = 404;
        } else {
            bytes = body.getBytes(StandardCharsets.UTF_8);
            status = 200;
        }
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
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

    private static String cdnUrl(String uuid) {
        return "https://cards.scryfall.io/normal/front/" + uuid.charAt(0) + "/" + uuid.charAt(1) + "/" + uuid + ".jpg";
    }

    private static String singleFaced(String id, String cn, String lang) {
        return String.format(
                "{\"id\":\"%s\",\"collector_number\":\"%s\",\"lang\":\"%s\",\"image_uris\":{\"normal\":\"%s\"}}",
                id, cn, lang, cdnUrl(id));
    }

    private static String doubleFaced(String id, String cn, String lang, String frontUuid, String backUuid) {
        return String.format(
                "{\"id\":\"%s\",\"collector_number\":\"%s\",\"lang\":\"%s\",\"card_faces\":["
                        + "{\"image_uris\":{\"normal\":\"%s\"}},"
                        + "{\"image_uris\":{\"normal\":\"%s\"}}]}",
                id, cn, lang, cdnUrl(frontUuid), cdnUrl(backUuid));
    }

    private String page(boolean hasMore, int pageNumber, String... entries) {
        String nextPage = hasMore ? ",\"next_page\":\"" + baseUrl + "/?page=" + (pageNumber + 1) + "\"" : "";
        return "{\"object\":\"list\",\"has_more\":" + hasMore + nextPage
                + ",\"data\":[" + String.join(",", entries) + "]}";
    }

    /** Triggers a cold lookup, (cdn) syncs (operation) synchronously, then re-queries for the result. */
    private static String resolveAfterSync(String set, String cn, String lang, String face, String size) {
        CdnUuidCache.getCdnUrl(set, cn, lang, face, size);
        CdnUuidCache.syncPendingSets();
        return CdnUuidCache.getCdnUrl(set, cn, lang, face, size);
    }

    // -------------------------------------------------------------------------

    @Test
    public void singleFacedCard_resolvesViaCdnUuidCache() {
        pages.put(1, page(false, 1,
                singleFaced("11111111-1111-1111-1111-111111111111", "1", "en")));

        String url = resolveAfterSync("neo", "1", "en", "front", "normal");

        Assert.assertEquals(url,
                CdnUuidCache.cdnUrl("11111111-1111-1111-1111-111111111111", "front", "normal"));
        Assert.assertTrue(new File(localCacheDir, "neo.json.gz").exists());
    }

    @Test
    public void dfcWithDistinctArtworkUuids_bothFacesResolveIndependently() {
        pages.put(1, page(false, 1,
                doubleFaced("22222222-2222-2222-2222-222222222222", "5", "en",
                        "aaaaaaaa-0000-0000-0000-000000000001",
                        "bbbbbbbb-0000-0000-0000-000000000002")));

        String front = resolveAfterSync("sld", "5", "en", "front", "normal");
        String back  = CdnUuidCache.getCdnUrl("sld", "5", "en", "back", "normal"); // set already synced by now

        Assert.assertEquals(front, CdnUuidCache.cdnUrl("aaaaaaaa-0000-0000-0000-000000000001", "front", "normal"),
                "front face UUID should come from card_faces[0].image_uris, not the card id");
        Assert.assertEquals(back, CdnUuidCache.cdnUrl("bbbbbbbb-0000-0000-0000-000000000002", "back", "normal"),
                "back face UUID should come from card_faces[1].image_uris, distinct from front");
    }

    @Test
    public void dfcWithSharedArtworkUuid_backResolvesToSameUuid() {
        pages.put(1, page(false, 1,
                doubleFaced("33333333-3333-3333-3333-333333333333", "6", "en",
                        "cccccccc-0000-0000-0000-000000000003",
                        "cccccccc-0000-0000-0000-000000000003")));

        String back = resolveAfterSync("neo", "6", "en", "back", "normal");

        Assert.assertEquals(back, CdnUuidCache.cdnUrl("cccccccc-0000-0000-0000-000000000003", "back", "normal"));
    }

    @Test
    public void multiplePages_paginatesViaNextPageAndMergesAll() {
        pages.put(1, page(true, 1, singleFaced("dddddddd-0000-0000-0000-000000000001", "1", "en")));
        pages.put(2, page(false, 2, singleFaced("eeeeeeee-0000-0000-0000-000000000002", "2", "en")));

        String url1 = resolveAfterSync("neo", "1", "en", "front", "normal");
        String url2 = CdnUuidCache.getCdnUrl("neo", "2", "en", "front", "normal"); // set already synced by now

        Assert.assertNotNull(url1);
        Assert.assertNotNull(url2);
        Assert.assertEquals(requestCount.get(), 2, "should follow next_page across both pages");
    }

    @Test
    public void unknownSet_returns404_lookupIsNullWithoutThrowing() {
        // No page registered for "xyz" -> handle() serves a 404, same as Scryfall for no matches.
        String url = resolveAfterSync("xyz", "1", "en", "front", "normal");

        Assert.assertNull(url);
        Assert.assertFalse(new File(localCacheDir, "xyz.json.gz").exists());
    }

    @Test
    public void sync_neverOverwritesExistingPrecomputedEntry() {
        // Pre-existing, precise data (e.g. from a prior manifest sync).
        Map<String, Map<String, String[]>> existing = new HashMap<>();
        Map<String, String[]> cn1 = new HashMap<>();
        cn1.put("en", new String[]{"ffffffff-0000-0000-0000-000000000001", null});
        existing.put("1", cn1);
        CdnUuidCache.mergeSetEntriesWithFaces("neo", existing);

        // Scryfall search sees a *different* id for the same card/lang -- must not clobber it.
        pages.put(1, page(false, 1, singleFaced("99999999-0000-0000-0000-000000000009", "1", "en")));
        ScryfallSetSync.sync("neo"); // direct call, bypassing CdnUuidCache's backgrounding entirely

        String url = CdnUuidCache.getCdnUrl("neo", "1", "en", "front", "normal");
        Assert.assertEquals(url, CdnUuidCache.cdnUrl("ffffffff-0000-0000-0000-000000000001", "front", "normal"),
                "pre-existing entries must win over a freshly-synced one");
    }

    @Test
    public void searchQuery_scopesToSetAndEnglish() {
        pages.put(1, page(false, 1, singleFaced("11111111-2222-3333-4444-555555555555", "1", "en")));

        resolveAfterSync("ltr", "1", "en", "front", "normal");

        Assert.assertNotNull(lastQuery);
        String decoded = URLDecoder.decode(lastQuery, StandardCharsets.UTF_8);
        Assert.assertTrue(decoded.contains("set:ltr"), "query should scope to the requested set: " + decoded);
        // Scoped to English only -- CdnUuidCache.getCdnUrl() always falls back to "en" and
        // virtually every edition is English, so fetching every printed language (lang:any)
        // multiplied API calls ~10x and tripped Scryfall's rate limit before a set finished syncing.
        Assert.assertTrue(decoded.contains("lang:en"), "query should scope to English: " + decoded);
    }

    // -------------------------------------------------------------------------
    // Miss-record retry

    private static void writeGzip(File f, String content) throws IOException {
        try (GZIPOutputStream gz = new GZIPOutputStream(new FileOutputStream(f))) {
            gz.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void freshMiss_doesNotRetryScryfall() throws IOException {
        // "neo" already has local data, but cn 9 was checked moments ago and came back empty.
        writeGzip(new File(localCacheDir, "neo.json.gz"),
                "{\"1\":{\"en\":\"11111111-0000-0000-0000-000000000001\"},"
                        + "\"9\":{\"en\":{\"miss\":\"" + java.time.Instant.now() + "\"}}}");

        String url = CdnUuidCache.getCdnUrl("neo", "9", "en", "front", "normal");

        Assert.assertNull(url);
        Assert.assertEquals(requestCount.get(), 0, "a fresh miss must not hit Scryfall again");
    }

    @Test
    public void staleMiss_retriesAndResolvesIfNowFound() throws IOException {
        writeGzip(new File(localCacheDir, "neo.json.gz"),
                "{\"9\":{\"en\":{\"miss\":\"2020-01-01T00:00:00Z\"}}}");
        pages.put(1, page(false, 1, singleFaced("99999999-0000-0000-0000-000000000009", "9", "en")));

        String immediate = CdnUuidCache.getCdnUrl("neo", "9", "en", "front", "normal");
        Assert.assertNull(immediate, "the retry is only queued, not run synchronously");
        CdnUuidCache.syncPendingSets();

        String url = CdnUuidCache.getCdnUrl("neo", "9", "en", "front", "normal");
        Assert.assertEquals(url, CdnUuidCache.cdnUrl("99999999-0000-0000-0000-000000000009", "front", "normal"));
        Assert.assertEquals(requestCount.get(), 1, "a stale miss should trigger exactly one retry");
    }

    @Test
    public void staleMiss_stillMissing_refreshesTimestampAndStopsRetrying() throws IOException {
        writeGzip(new File(localCacheDir, "neo.json.gz"),
                "{\"9\":{\"en\":{\"miss\":\"2020-01-01T00:00:00Z\"}}}");
        // No page registered -> 404, same as Scryfall finding nothing for this set.

        CdnUuidCache.getCdnUrl("neo", "9", "en", "front", "normal"); // queues the retry
        CdnUuidCache.syncPendingSets();
        Assert.assertEquals(requestCount.get(), 1, "the stale miss should have triggered one retry");

        String second = CdnUuidCache.getCdnUrl("neo", "9", "en", "front", "normal");
        Assert.assertNull(second);
        Assert.assertEquals(requestCount.get(), 1, "the refreshed miss timestamp should prevent an immediate second retry");
    }
}
