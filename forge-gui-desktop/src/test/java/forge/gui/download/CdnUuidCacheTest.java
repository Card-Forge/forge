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

    private File tempDir;

    @BeforeClass
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("cdn_uuid_test").toFile();

        // SET: single-faced cards in multiple languages
        File setDir = new File(tempDir, SET);
        setDir.mkdir();
        // 1.json — "en" and "ja" UUIDs
        write(new File(setDir, "1.json"),
                "{\"en\":\"" + UUID_EN + "\",\"ja\":\"" + UUID_JA + "\"}");
        // 2.json — "en" only
        write(new File(setDir, "2.json"),
                "{\"en\":\"" + UUID_EN + "\"}");

        // SET_DFC: double-faced cards
        File dfcDir = new File(tempDir, SET_DFC);
        dfcDir.mkdir();
        // 1.json — distinct front and back UUIDs
        write(new File(dfcDir, "1.json"),
                "{\"en\":[\"" + UUID_FRONT + "\",\"" + UUID_BACK + "\"]}");
        // 2.json — both faces share the same UUID
        write(new File(dfcDir, "2.json"),
                "{\"en\":[\"" + UUID_FRONT + "\",\"" + UUID_FRONT + "\"]}");

        CdnUuidCache.cdnBaseDirOverride = tempDir.getAbsolutePath() + File.separator;
        CdnUuidCache.clearCacheForTesting();
    }

    @AfterClass
    public void tearDown() {
        CdnUuidCache.cdnBaseDirOverride = null;
        CdnUuidCache.clearCacheForTesting();
        deleteDir(tempDir);
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
        // When both faces share the same UUID, back=null internally; front UUID is used.
        String url = CdnUuidCache.getCdnUrl(SET_DFC, "2", "en", "back", "normal");
        Assert.assertEquals(url, ScryfallBulkData.cdnUrl(UUID_FRONT, "back", "normal"));
    }

    @Test
    public void dfcEmptyFaceString_treatedAsFront() {
        // ImageFetcher passes "" for the front face.
        String urlEmpty  = CdnUuidCache.getCdnUrl(SET_DFC, "1", "en", "", "normal");
        String urlFront  = CdnUuidCache.getCdnUrl(SET_DFC, "1", "en", "front", "normal");
        Assert.assertEquals(urlEmpty, urlFront);
    }

    // --- set code normalisation ---

    @Test
    public void uppercaseSetCode_lowercasedBeforeLookup() {
        String url = CdnUuidCache.getCdnUrl(SET.toUpperCase(), "1", "en", "front", "normal");
        Assert.assertEquals(url, ScryfallBulkData.cdnUrl(UUID_EN, "front", "normal"));
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
    public void absentSetDirectory_returnsNull() {
        Assert.assertNull(CdnUuidCache.getCdnUrl(SET_ABSENT, "1", "en", "front", "normal"));
    }

    @Test(dependsOnMethods = "absentSetDirectory_returnsNull")
    public void absentSetCachedAsMissing_secondCallAlsoNull() {
        // MISSING_SET sentinel should be in cache; same result on repeated lookup.
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
                else child.delete();
            }
        }
        dir.delete();
    }
}
