package forge.gui.download;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Test(groups = {"UnitTest"})
public class CdnUuidCacheTest {

    private static final String SET        = "tst";
    private static final String SET_DFC    = "dfc";
    private static final String SET_ABSENT = "xyz";

    private static final String UUID_EN    = "aaaaaaaa-bbbb-cccc-dddd-000000000001";
    private static final String UUID_JA    = "aaaaaaaa-bbbb-cccc-dddd-000000000002";
    private static final String UUID_FRONT = "aaaaaaaa-bbbb-cccc-dddd-000000000003";
    private static final String UUID_BACK  = "aaaaaaaa-bbbb-cccc-dddd-000000000004";

    /** Local cache dir — starts empty; populated by the cache on first remote fetch. */
    private File localCacheDir;
    /** Remote "server" dir — pre-populated with set JSON files, served via file:// URL. */
    private File remoteDir;

    @BeforeClass
    public void setUp() throws IOException {
        localCacheDir = Files.createTempDirectory("cdn_local").toFile();
        remoteDir     = Files.createTempDirectory("cdn_remote").toFile();

        // tst.json — single-faced cards, multiple languages
        write(new File(remoteDir, SET + ".json"),
                "{"
                + "\"1\":{\"en\":\"" + UUID_EN + "\",\"ja\":\"" + UUID_JA + "\"},"
                + "\"2\":{\"en\":\"" + UUID_EN + "\"}"
                + "}");

        // dfc.json — double-faced cards
        write(new File(remoteDir, SET_DFC + ".json"),
                "{"
                + "\"1\":{\"en\":[\"" + UUID_FRONT + "\",\"" + UUID_BACK  + "\"]},"
                + "\"2\":{\"en\":[\"" + UUID_FRONT + "\",\"" + UUID_FRONT + "\"]}"
                + "}");

        // SET_ABSENT has no file in remoteDir — lookups must return null

        CdnUuidCache.localCacheDirOverride = localCacheDir.getAbsolutePath() + File.separator;
        CdnUuidCache.remoteBaseUrlOverride = remoteDir.toURI().toURL().toString();
        CdnUuidCache.clearCacheForTesting();
    }

    @AfterClass
    public void tearDown() {
        CdnUuidCache.localCacheDirOverride = null;
        CdnUuidCache.remoteBaseUrlOverride = null;
        CdnUuidCache.clearCacheForTesting();
        deleteDir(localCacheDir);
        deleteDir(remoteDir);
    }

    // --- happy path ---

    @Test
    public void englishFront_returnsCorrectCdnUrl() {
        String url = CdnUuidCache.getCdnUrl(SET, "1", "en", "front", "normal");
        Assert.assertEquals(url, ScryfallBulkData.cdnUrl(UUID_EN, "front", "normal"));
    }

    @Test
    public void artCropSize_reflectedInUrl() {
        String url = CdnUuidCache.getCdnUrl(SET, "1", "en", "front", "art_crop");
        Assert.assertEquals(url, ScryfallBulkData.cdnUrl(UUID_EN, "front", "art_crop"));
    }

    @Test
    public void japaneseLang_returnsJaUuid() {
        String url = CdnUuidCache.getCdnUrl(SET, "1", "ja", "front", "normal");
        Assert.assertEquals(url, ScryfallBulkData.cdnUrl(UUID_JA, "front", "normal"));
    }

    // --- language fallback ---

    @Test
    public void unknownLang_fallsBackToEnglish() {
        String url = CdnUuidCache.getCdnUrl(SET, "1", "zz", "front", "normal");
        Assert.assertEquals(url, ScryfallBulkData.cdnUrl(UUID_EN, "front", "normal"));
    }

    @Test
    public void cardWithOnlyEn_jaRequestFallsBack() {
        String url = CdnUuidCache.getCdnUrl(SET, "2", "ja", "front", "normal");
        Assert.assertEquals(url, ScryfallBulkData.cdnUrl(UUID_EN, "front", "normal"));
    }

    // --- DFC (double-faced cards) ---

    @Test
    public void dfcDistinctFaces_frontUuid() {
        String url = CdnUuidCache.getCdnUrl(SET_DFC, "1", "en", "front", "normal");
        Assert.assertEquals(url, ScryfallBulkData.cdnUrl(UUID_FRONT, "front", "normal"));
    }

    @Test
    public void dfcDistinctFaces_backUuid() {
        String url = CdnUuidCache.getCdnUrl(SET_DFC, "1", "en", "back", "normal");
        Assert.assertEquals(url, ScryfallBulkData.cdnUrl(UUID_BACK, "back", "normal"));
    }

    @Test
    public void dfcSameUuid_backRequestStillUsesSharedUuid() {
        // When both faces share the same UUID, back is stored as null internally.
        String url = CdnUuidCache.getCdnUrl(SET_DFC, "2", "en", "back", "normal");
        Assert.assertEquals(url, ScryfallBulkData.cdnUrl(UUID_FRONT, "back", "normal"));
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
        Assert.assertEquals(url, ScryfallBulkData.cdnUrl(UUID_EN, "front", "normal"));
    }

    // --- remote fetch writes to local cache ---

    @Test
    public void remoteFetch_writesLocalCacheFile() {
        // After the first successful lookup, the set JSON should be present in localCacheDir.
        CdnUuidCache.getCdnUrl(SET, "1", "en", "front", "normal");
        Assert.assertTrue(new File(localCacheDir, SET + ".json").exists(),
                "local cache file should be written after remote fetch");
    }

    @Test(dependsOnMethods = "remoteFetch_writesLocalCacheFile")
    public void localCache_usedOnSubsequentLookup() throws IOException {
        // Corrupt the in-memory cache but keep the local file; clear remote.
        CdnUuidCache.clearCacheForTesting();
        CdnUuidCache.remoteBaseUrlOverride = "file:///nonexistent-dir/";
        try {
            String url = CdnUuidCache.getCdnUrl(SET, "1", "en", "front", "normal");
            Assert.assertEquals(url, ScryfallBulkData.cdnUrl(UUID_EN, "front", "normal"),
                    "should resolve from local cache even when remote is unavailable");
        } finally {
            CdnUuidCache.remoteBaseUrlOverride = remoteDir.toURI().toURL().toString();
            CdnUuidCache.clearCacheForTesting();
        }
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
        // No local file and no remote file for SET_ABSENT.
        Assert.assertNull(CdnUuidCache.getCdnUrl(SET_ABSENT, "1", "en", "front", "normal"));
    }

    @Test(dependsOnMethods = "absentSet_returnsNull")
    public void absentSetCachedAsMissing_secondCallAlsoNull() {
        // MISSING_SET sentinel must be in cache; second lookup must not retry remote.
        Assert.assertNull(CdnUuidCache.getCdnUrl(SET_ABSENT, "99", "en", "front", "normal"));
    }

    @Test
    public void unknownCollectorNumber_returnsNull() {
        Assert.assertNull(CdnUuidCache.getCdnUrl(SET, "9999", "en", "front", "normal"));
    }

    // --- helpers ---

    private static void write(File f, String content) throws IOException {
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
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
