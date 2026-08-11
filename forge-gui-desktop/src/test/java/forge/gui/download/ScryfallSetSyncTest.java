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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exercises {@link ScryfallSetSync} — the on-demand, per-set generator that
 * {@link CdnUuidCache} falls back to when a set has no local data yet — against
 * a tiny embedded HTTP server standing in for {@code api.scryfall.com/cards/search},
 * so these run offline and don't depend on Scryfall's real catalog.
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

    // -------------------------------------------------------------------------

    @Test
    public void singleFacedCard_resolvesViaCdnUuidCache() {
        pages.put(1, page(false, 1,
                singleFaced("11111111-1111-1111-1111-111111111111", "1", "en")));

        String url = CdnUuidCache.getCdnUrl("neo", "1", "en", "front", "normal");

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

        String front = CdnUuidCache.getCdnUrl("sld", "5", "en", "front", "normal");
        String back  = CdnUuidCache.getCdnUrl("sld", "5", "en", "back", "normal");

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

        String back = CdnUuidCache.getCdnUrl("neo", "6", "en", "back", "normal");

        Assert.assertEquals(back, CdnUuidCache.cdnUrl("cccccccc-0000-0000-0000-000000000003", "back", "normal"));
    }

    @Test
    public void multiplePages_paginatesViaNextPageAndMergesAll() {
        pages.put(1, page(true, 1, singleFaced("dddddddd-0000-0000-0000-000000000001", "1", "en")));
        pages.put(2, page(false, 2, singleFaced("eeeeeeee-0000-0000-0000-000000000002", "2", "en")));

        String url1 = CdnUuidCache.getCdnUrl("neo", "1", "en", "front", "normal");
        String url2 = CdnUuidCache.getCdnUrl("neo", "2", "en", "front", "normal");

        Assert.assertNotNull(url1);
        Assert.assertNotNull(url2);
        Assert.assertEquals(requestCount.get(), 2, "should follow next_page across both pages");
    }

    @Test
    public void unknownSet_returns404_lookupIsNullWithoutThrowing() {
        // No page registered for "xyz" -> handle() serves a 404, same as Scryfall for no matches.
        String url = CdnUuidCache.getCdnUrl("xyz", "1", "en", "front", "normal");

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
        ScryfallSetSync.sync("neo");

        String url = CdnUuidCache.getCdnUrl("neo", "1", "en", "front", "normal");
        Assert.assertEquals(url, CdnUuidCache.cdnUrl("ffffffff-0000-0000-0000-000000000001", "front", "normal"),
                "pre-existing entries must win over a freshly-synced one");
    }

    @Test
    public void searchQuery_scopesToSetAndAllLanguages() {
        pages.put(1, page(false, 1, singleFaced("11111111-2222-3333-4444-555555555555", "1", "en")));

        CdnUuidCache.getCdnUrl("ltr", "1", "en", "front", "normal");

        Assert.assertNotNull(lastQuery);
        String decoded = URLDecoder.decode(lastQuery, StandardCharsets.UTF_8);
        Assert.assertTrue(decoded.contains("set:ltr"), "query should scope to the requested set: " + decoded);
        Assert.assertTrue(decoded.contains("lang:any"), "query should request all languages: " + decoded);
    }
}
