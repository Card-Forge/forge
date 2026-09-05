package forge.gui.download;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * UUID data is pre-seeded into the local cache dir (gzip, as {@link CdnUuidCache} writes it).
 * {@link ScryfallSetSync#searchBaseUrlOverride} points at an unreachable address so an
 * unseeded lookup fails fast; on-demand generation is covered by {@link ScryfallSetSyncTest}.
 */
@Test(groups = {"UnitTest"})
public class CdnUuidCacheTest {

    private static final String SET        = "tst";
    private static final String SET_DFC    = "dfc";
    private static final String SET_ABSENT = "xyz";

    private static final String UUID_EN    = "aaaaaaaa-bbbb-cccc-dddd-000000000001";
    private static final String UUID_JA    = "aaaaaaaa-bbbb-cccc-dddd-000000000002";
    private static final String UUID_FRONT = "aaaaaaaa-bbbb-cccc-dddd-000000000003";
    private static final String UUID_BACK  = "aaaaaaaa-bbbb-cccc-dddd-000000000004";

    private File localCacheDir;

    @BeforeClass
    public void setUp() throws IOException {
        localCacheDir = Files.createTempDirectory("cdn_local").toFile();

        // tst.json.gz — single-faced cards, multiple languages
        writeGzip(new File(localCacheDir, SET + ".json.gz"),
                "{"
                + "\"1\":{\"en\":\"" + UUID_EN + "\",\"ja\":\"" + UUID_JA + "\"},"
                + "\"2\":{\"en\":\"" + UUID_EN + "\"}"
                + "}");

        // dfc.json.gz — double-faced cards
        writeGzip(new File(localCacheDir, SET_DFC + ".json.gz"),
                "{"
                + "\"1\":{\"en\":[\"" + UUID_FRONT + "\",\"" + UUID_BACK  + "\"]},"
                + "\"2\":{\"en\":[\"" + UUID_FRONT + "\",\"" + UUID_FRONT + "\"]}"
                + "}");

        // SET_ABSENT has no local file; the Scryfall override is unreachable.

        CdnUuidCache.localCacheDirOverride = localCacheDir.getAbsolutePath() + File.separator;
        CdnUuidCache.autoSyncEnabled = false; // drive misses synchronously, not via the pool
        ScryfallSetSync.searchBaseUrlOverride = "http://127.0.0.1:1/unreachable";
        CdnUuidCache.clearCacheForTesting();
    }

    @AfterClass
    public void tearDown() {
        CdnUuidCache.localCacheDirOverride = null;
        CdnUuidCache.autoSyncEnabled = true;
        ScryfallSetSync.searchBaseUrlOverride = null;
        CdnUuidCache.clearCacheForTesting();
        deleteDir(localCacheDir);
    }

    // --- CDN URL formula ---

    @Test
    public void cdnUrl_matchesScryfallFormula() {
        String uuid = "4e7a547f-d1b0-4f4e-9a99-3c44fc89c048";
        Assert.assertEquals(
                CdnUuidCache.cdnUrl(uuid, "front", "normal"),
                "https://cards.scryfall.io/normal/front/4/e/" + uuid + ".jpg");
        Assert.assertEquals(
                CdnUuidCache.cdnUrl(uuid, "back", "art_crop"),
                "https://cards.scryfall.io/art_crop/back/4/e/" + uuid + ".jpg");
    }

    // --- happy path ---

    @Test
    public void englishFront_returnsCorrectCdnUrl() {
        String url = CdnUuidCache.getCdnUrl(SET, "1", "en", "front", "normal");
        Assert.assertEquals(url, CdnUuidCache.cdnUrl(UUID_EN, "front", "normal"));
    }

    @Test
    public void artCropSize_reflectedInUrl() {
        String url = CdnUuidCache.getCdnUrl(SET, "1", "en", "front", "art_crop");
        Assert.assertEquals(url, CdnUuidCache.cdnUrl(UUID_EN, "front", "art_crop"));
    }

    @Test
    public void japaneseLang_returnsJaUuid() {
        String url = CdnUuidCache.getCdnUrl(SET, "1", "ja", "front", "normal");
        Assert.assertEquals(url, CdnUuidCache.cdnUrl(UUID_JA, "front", "normal"));
    }

    // --- language fallback ---

    @Test
    public void unknownLang_fallsBackToEnglish() {
        String url = CdnUuidCache.getCdnUrl(SET, "1", "zz", "front", "normal");
        Assert.assertEquals(url, CdnUuidCache.cdnUrl(UUID_EN, "front", "normal"));
    }

    @Test
    public void cardWithOnlyEn_jaRequestFallsBack() {
        String url = CdnUuidCache.getCdnUrl(SET, "2", "ja", "front", "normal");
        Assert.assertEquals(url, CdnUuidCache.cdnUrl(UUID_EN, "front", "normal"));
    }

    // --- DFC (double-faced cards) ---

    @Test
    public void dfcDistinctFaces_frontUuid() {
        String url = CdnUuidCache.getCdnUrl(SET_DFC, "1", "en", "front", "normal");
        Assert.assertEquals(url, CdnUuidCache.cdnUrl(UUID_FRONT, "front", "normal"));
    }

    @Test
    public void dfcDistinctFaces_backUuid() {
        String url = CdnUuidCache.getCdnUrl(SET_DFC, "1", "en", "back", "normal");
        Assert.assertEquals(url, CdnUuidCache.cdnUrl(UUID_BACK, "back", "normal"));
    }

    @Test
    public void dfcSameUuid_backRequestStillUsesSharedUuid() {
        // When both faces share the same UUID, back is stored as null internally.
        String url = CdnUuidCache.getCdnUrl(SET_DFC, "2", "en", "back", "normal");
        Assert.assertEquals(url, CdnUuidCache.cdnUrl(UUID_FRONT, "back", "normal"));
    }

    @Test
    public void dfcEmptyFaceString_treatedAsFront() {
        // ImageFetcher passes "" for the front face.
        String urlEmpty = CdnUuidCache.getCdnUrl(SET_DFC, "1", "en", "",      "normal");
        String urlFront = CdnUuidCache.getCdnUrl(SET_DFC, "1", "en", "front", "normal");
        Assert.assertEquals(urlEmpty, urlFront);
    }

    // --- set code normalisation ---

    @Test
    public void uppercaseSetCode_lowercasedBeforeLookup() {
        String url = CdnUuidCache.getCdnUrl(SET.toUpperCase(), "1", "en", "front", "normal");
        Assert.assertEquals(url, CdnUuidCache.cdnUrl(UUID_EN, "front", "normal"));
    }

    // --- null / missing inputs ---

    @Test
    public void nullScryfallCode_returnsNull() {
        Assert.assertNull(CdnUuidCache.getCdnUrl(null, "1", "en", "front", "normal"));
    }

    @Test
    public void nullCollectorNumber_returnsNull() {
        Assert.assertNull(CdnUuidCache.getCdnUrl(SET, null, "en", "front", "normal"));
    }

    @Test
    public void absentSet_returnsNull() {
        // No local file for SET_ABSENT, and the Scryfall search override is unreachable.
        Assert.assertNull(CdnUuidCache.getCdnUrl(SET_ABSENT, "1", "en", "front", "normal"));
    }

    @Test(dependsOnMethods = "absentSet_returnsNull")
    public void absentSetCachedAsMissing_secondCallAlsoNull() {
        // MISSING_SET sentinel must be in cache; second lookup must not retry Scryfall.
        Assert.assertNull(CdnUuidCache.getCdnUrl(SET_ABSENT, "99", "en", "front", "normal"));
    }

    @Test
    public void unknownCollectorNumber_returnsNull() {
        Assert.assertNull(CdnUuidCache.getCdnUrl(SET, "9999", "en", "front", "normal"));
    }

    // --- miss tracking ---

    @Test(dependsOnMethods = "unknownCollectorNumber_returnsNull")
    public void missingEntry_isPersistedAsTimestampedMiss() throws IOException {
        // The prior lookup for cn 9999 should have recorded a miss marker on disk.
        String raw = readGunzipped(new File(localCacheDir, SET + ".json.gz"));
        Assert.assertTrue(raw.contains("\"9999\""), "miss should be recorded under its collector number: " + raw);
        Assert.assertTrue(raw.contains("\"miss\""), "miss should be recorded with the miss marker: " + raw);

        // Real entries already in the file must be untouched.
        Assert.assertEquals(CdnUuidCache.getCdnUrl(SET, "1", "en", "front", "normal"),
                CdnUuidCache.cdnUrl(UUID_EN, "front", "normal"));
    }

    @Test(dependsOnMethods = "missingEntry_isPersistedAsTimestampedMiss")
    public void freshMiss_stillReturnsNullOnRepeatLookup() {
        // The miss recorded above is fresh, well under the retry window.
        Assert.assertNull(CdnUuidCache.getCdnUrl(SET, "9999", "en", "front", "normal"));
    }

    @Test
    public void recordedMiss_upgradesToRealEntryOnceMerged() {
        // A real entry found later (e.g. via a resync) must overwrite the miss marker.
        Map<String, String[]> cn7 = new HashMap<>();
        cn7.put("en", new String[]{UUID_EN, null});
        Map<String, Map<String, String[]>> found = new HashMap<>();
        found.put("7", cn7);

        Assert.assertNull(CdnUuidCache.getCdnUrl(SET, "7", "en", "front", "normal"));
        CdnUuidCache.mergeSetEntriesWithFaces(SET, found);

        Assert.assertEquals(CdnUuidCache.getCdnUrl(SET, "7", "en", "front", "normal"),
                CdnUuidCache.cdnUrl(UUID_EN, "front", "normal"));
    }

    // --- helpers ---

    private static void writeGzip(File f, String content) throws IOException {
        try (GZIPOutputStream gz = new GZIPOutputStream(new FileOutputStream(f))) {
            gz.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String readGunzipped(File f) throws IOException {
        try (GZIPInputStream gz = new GZIPInputStream(new FileInputStream(f))) {
            return new String(gz.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void deleteDir(File dir) {
        if (dir == null) return;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) deleteDir(child);
                else //noinspection ResultOfMethodCallIgnored
                    child.delete();
            }
        }
        //noinspection ResultOfMethodCallIgnored
        dir.delete();
    }
}
