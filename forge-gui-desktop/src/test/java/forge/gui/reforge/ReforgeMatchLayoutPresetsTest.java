/*
 * REFORGE COMMANDER EXTENSION
 *
 * Tests for ReforgeMatchLayoutPresets: canonical N-player battlefield layout
 * generation (#88, doc:12a) and the apply()/restoreDefault() file operations
 * that persist/clear the user's match.xml.
 */
package forge.gui.reforge;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import forge.GuiDesktop;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class ReforgeMatchLayoutPresetsTest {

    private static final Pattern FIELD_DOC_PATTERN = Pattern.compile("<doc>FIELD_(\\d+)</doc>");

    private Path layoutPath;
    private boolean originalExisted;
    private byte[] originalContent;

    @BeforeClass
    public void setUpClass() {
        GuiBase.setInterface(new GuiDesktop());
    }

    /**
     * Back up whatever match layout the current environment already has (if any)
     * before every test, so tests that call apply()/restoreDefault() never leave
     * behind a mutated file on the machine running the tests.
     */
    @BeforeMethod
    public void backupLayoutFile() throws IOException {
        layoutPath = Path.of(ForgeConstants.MATCH_LAYOUT_FILE.userPrefLoc);
        originalExisted = Files.exists(layoutPath);
        originalContent = originalExisted ? Files.readAllBytes(layoutPath) : null;
    }

    @AfterMethod
    public void restoreLayoutFile() throws IOException {
        if (originalExisted) {
            Files.createDirectories(layoutPath.toAbsolutePath().getParent());
            Files.write(layoutPath, originalContent);
        } else {
            Files.deleteIfExists(layoutPath);
        }
    }

    // ---------------------------------------------------------------
    // layoutFor()
    // ---------------------------------------------------------------

    @Test
    public void testMaxPlayersConstant() {
        assertEquals(ReforgeMatchLayoutPresets.MAX_PLAYERS, 8);
    }

    @Test
    public void testLayoutForStartsAndEndsCorrectly() {
        final String xml = ReforgeMatchLayoutPresets.layoutFor(2);
        assertTrue(xml.startsWith("<?xml version=\"1.0\"?>\n"), "Should start with XML declaration");
        assertTrue(xml.contains("<layout serial=\"\">\n"), "Should contain the opening layout tag");
        assertTrue(xml.trim().endsWith("</layout>"), "Should end with the closing layout tag");
    }

    @Test
    public void testLayoutForTwoPlayersContainsBothFieldsOnly() {
        final String xml = ReforgeMatchLayoutPresets.layoutFor(2);
        assertTrue(xml.contains("<doc>FIELD_0</doc>"));
        assertTrue(xml.contains("<doc>FIELD_1</doc>"));
        assertFalse(xml.contains("<doc>FIELD_2</doc>"));
    }

    @Test
    public void testLayoutForFieldCountMatchesPlayerCountForAllSupportedCounts() {
        for (int players = 1; players <= ReforgeMatchLayoutPresets.MAX_PLAYERS; players++) {
            final String xml = ReforgeMatchLayoutPresets.layoutFor(players);
            final java.util.Set<Integer> fieldIndices = new java.util.TreeSet<>();
            final Matcher matcher = FIELD_DOC_PATTERN.matcher(xml);
            while (matcher.find()) {
                fieldIndices.add(Integer.parseInt(matcher.group(1)));
            }
            assertEquals(fieldIndices.size(), players,
                    "Expected " + players + " distinct FIELD_ docs for a " + players + "-player layout");
            for (int i = 0; i < players; i++) {
                assertTrue(fieldIndices.contains(i),
                        "Missing FIELD_" + i + " in " + players + "-player layout");
            }
        }
    }

    @Test
    public void testLayoutForClampsBelowMinimumToOnePlayer() {
        final String zero = ReforgeMatchLayoutPresets.layoutFor(0);
        final String negative = ReforgeMatchLayoutPresets.layoutFor(-5);
        final String minInt = ReforgeMatchLayoutPresets.layoutFor(Integer.MIN_VALUE);
        final String onePlayer = ReforgeMatchLayoutPresets.layoutFor(1);

        assertEquals(zero, onePlayer);
        assertEquals(negative, onePlayer);
        assertEquals(minInt, onePlayer);
        assertTrue(onePlayer.contains("<doc>FIELD_0</doc>"));
        assertFalse(onePlayer.contains("<doc>FIELD_1</doc>"));
    }

    @Test
    public void testLayoutForClampsAboveMaximumToEightPlayers() {
        final String nine = ReforgeMatchLayoutPresets.layoutFor(9);
        final String hundred = ReforgeMatchLayoutPresets.layoutFor(100);
        final String maxInt = ReforgeMatchLayoutPresets.layoutFor(Integer.MAX_VALUE);
        final String eightPlayers = ReforgeMatchLayoutPresets.layoutFor(ReforgeMatchLayoutPresets.MAX_PLAYERS);

        assertEquals(nine, eightPlayers);
        assertEquals(hundred, eightPlayers);
        assertEquals(maxInt, eightPlayers);
    }

    @Test
    public void testLayoutForContainsStaticZonesRegardlessOfPlayerCount() {
        for (final int players : new int[] {2, 5, 8}) {
            final String xml = ReforgeMatchLayoutPresets.layoutFor(players);
            for (final String zone : new String[] {
                    "REPORT_STACK", "REPORT_COMBAT", "REPORT_LOG", "REPORT_DEPENDENCIES",
                    "BUTTON_DOCK", "REPORT_MESSAGE", "DEV_MODE",
                    "HAND_0", "CARD_DETAIL", "CARD_PICTURE"}) {
                assertTrue(xml.contains("<doc>" + zone + "</doc>"),
                        players + "-player layout missing static zone " + zone);
            }
        }
    }

    @Test
    public void testLayoutForIsWellFormedXml() throws Exception {
        final String xml = ReforgeMatchLayoutPresets.layoutFor(4);
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document document = builder.parse(new InputSource(new StringReader(xml)));

        final Element root = document.getDocumentElement();
        assertEquals(root.getTagName(), "layout");

        // 3 left-rail cells + 4 field cells + 1 hand cell + 2 right-rail cells = 10
        final NodeList cells = document.getElementsByTagName("cell");
        assertEquals(cells.getLength(), 10);
    }

    @Test
    public void testLayoutForIsDeterministic() {
        assertEquals(ReforgeMatchLayoutPresets.layoutFor(3), ReforgeMatchLayoutPresets.layoutFor(3));
        assertEquals(ReforgeMatchLayoutPresets.layoutFor(6), ReforgeMatchLayoutPresets.layoutFor(6));
    }

    @Test
    public void testLayoutForDifferentPlayerCountsProduceDifferentLayouts() {
        assertFalse(ReforgeMatchLayoutPresets.layoutFor(2).equals(ReforgeMatchLayoutPresets.layoutFor(3)));
        assertFalse(ReforgeMatchLayoutPresets.layoutFor(4).equals(ReforgeMatchLayoutPresets.layoutFor(8)));
    }

    // ---------------------------------------------------------------
    // apply() / restoreDefault()
    // ---------------------------------------------------------------

    @Test
    public void testApplyWritesExpectedLayoutToDisk() throws IOException {
        ReforgeMatchLayoutPresets.apply(3);

        assertTrue(Files.exists(layoutPath), "apply() should create the match layout file");
        final String written = Files.readString(layoutPath);
        assertEquals(written, ReforgeMatchLayoutPresets.layoutFor(3));
    }

    @Test
    public void testApplyOverwritesPreviouslyAppliedLayout() throws IOException {
        ReforgeMatchLayoutPresets.apply(2);
        assertEquals(Files.readString(layoutPath), ReforgeMatchLayoutPresets.layoutFor(2));

        ReforgeMatchLayoutPresets.apply(5);
        final String written = Files.readString(layoutPath);
        assertEquals(written, ReforgeMatchLayoutPresets.layoutFor(5));
        assertFalse(written.equals(ReforgeMatchLayoutPresets.layoutFor(2)));
    }

    @Test
    public void testApplyDoesNotLeaveTemporaryFileBehind() throws IOException {
        ReforgeMatchLayoutPresets.apply(4);
        final Path temp = layoutPath.resolveSibling(layoutPath.getFileName() + ".tmp");
        assertFalse(Files.exists(temp), "apply() should not leave a .tmp file behind on success");
    }

    @Test
    public void testRestoreDefaultDeletesLayoutFile() throws IOException {
        ReforgeMatchLayoutPresets.apply(6);
        assertTrue(Files.exists(layoutPath));

        ReforgeMatchLayoutPresets.restoreDefault();
        assertFalse(Files.exists(layoutPath), "restoreDefault() should delete the user's match layout file");
    }

    @Test
    public void testRestoreDefaultWhenFileAbsentDoesNotThrow() throws IOException {
        Files.deleteIfExists(layoutPath);
        try {
            ReforgeMatchLayoutPresets.restoreDefault();
            ReforgeMatchLayoutPresets.restoreDefault();
        } catch (final IOException ex) {
            fail("restoreDefault() should be a no-op (not throw) when no layout file exists", ex);
        }
        assertFalse(Files.exists(layoutPath));
    }

    @Test
    public void testApplyThenRestoreDefaultRoundTrip() throws IOException {
        ReforgeMatchLayoutPresets.apply(7);
        assertTrue(Files.exists(layoutPath));

        ReforgeMatchLayoutPresets.restoreDefault();
        assertFalse(Files.exists(layoutPath));

        // Applying again afterward should still work normally.
        ReforgeMatchLayoutPresets.apply(2);
        assertEquals(Files.readString(layoutPath), ReforgeMatchLayoutPresets.layoutFor(2));
    }
}