package forge.card;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.item.PaperCard;
import forge.model.FModel;

public class PrintExclusionTest extends CardMockTestCase {

    private static final String UB = "UniversesBeyond";

    private static final String[] CARD_NAMES = {
            "Sol Ring", "Lightning Bolt", "Counterspell", "Llanowar Elves", "Wrath of God", "Swords to Plowshares"
    };

    private CardDb cardDb;

    @BeforeMethod
    public void setupDb() {
        cardDb = FModel.getMagicDb().getCommonCards();
    }

    @AfterMethod(alwaysRun = true)
    public void restoreDb() {
        if (cardDb != null)
            cardDb.setPrintExclusion(null);
    }

    private CardEdition edition(String code) {
        return FModel.getMagicDb().getEditions().get(code);
    }

    @Test
    public void pickerTicksRoundTripThroughEntries() {
        Set<String> universesBeyond = Set.of("FIN", "LTR", "LTC");
        Set<String> entries = SetExclusions.entriesFor(List.of("FIN", "ME1"), Set.of(UB), tag -> universesBeyond);
        // FIN is covered by the tag, so it is not stored, and the unticked UB sets are stored as kept
        assertEquals(entries, Set.of("#UniversesBeyond", "!LTR", "!LTC", "ME1"));
        SetExclusions exclusions = new SetExclusions(entries);
        assertTrue(exclusions.getTags().contains(UB));
        assertTrue(exclusions.excludes(edition("FIN"), null));
        assertTrue(exclusions.excludes(edition("ME1"), null));
        assertFalse(exclusions.excludes(edition("LTR"), null));
        // with the UB tag not excluded, a ticked UB set is stored as a plain code
        assertEquals(SetExclusions.entriesFor(List.of("FIN", "ME1"), Set.of(), tag -> universesBeyond), Set.of("FIN", "ME1"));
    }

    @Test
    public void blankEntriesAreIgnored() {
        assertTrue(new SetExclusions(List.of("#", "!", " ", "# ")).isEmpty());
        // a space after the prefix is trimmed, not taken as part of the tag
        assertTrue(new SetExclusions(List.of("# UniversesBeyond")).getTags().contains(UB));
    }

    @Test
    public void aSetCodeEntryExcludesOnlyThatSet() {
        SetExclusions exclusions = new SetExclusions(List.of("ME1"));
        assertTrue(exclusions.excludes(edition("ME1"), null));
        assertFalse(exclusions.excludes(edition("ME2"), null));
    }

    @Test
    public void aTagEntryExcludesTaggedSetsAndPrintsExceptKeptSets() {
        SetExclusions exclusions = new SetExclusions(List.of("#UniversesBeyond", "!LTR"));
        assertTrue(exclusions.excludes(edition("FIN"), null));
        assertFalse(exclusions.excludes(edition("LTR"), null));
        assertFalse(exclusions.excludes(edition("SLD"), null));  // only some of its prints are tagged
        assertTrue(exclusions.excludes(edition("SLD"), "143"));  // Rick, Steadfast Leader, a UB print
    }

    @Test
    public void anExclusionThatExcludesNothingChangesNoPrint() {
        PaperCard[] unique = new PaperCard[CARD_NAMES.length];
        PaperCard[] requested = new PaperCard[CARD_NAMES.length];
        for (int i = 0; i < CARD_NAMES.length; i++) {
            unique[i] = cardDb.getUniqueByName(CARD_NAMES[i]);
            requested[i] = cardDb.getCard(CARD_NAMES[i]);
        }
        cardDb.setPrintExclusion((edition, collectorNumber) -> false);
        for (int i = 0; i < CARD_NAMES.length; i++) {
            assertEquals(cardDb.getUniqueByName(CARD_NAMES[i]), unique[i]);
            assertEquals(cardDb.getCard(CARD_NAMES[i]), requested[i]);
        }
    }

    @Test
    public void anExcludedPrintIsReplacedByAnotherPrint() {
        for (String name : CARD_NAMES) {
            String defaultEdition = cardDb.getUniqueByName(name).getEdition();
            cardDb.setPrintExclusion((edition, collectorNumber) -> edition.getCode().equals(defaultEdition));
            assertNotEquals(cardDb.getUniqueByName(name).getEdition(), defaultEdition, name);
            assertFalse(cardDb.isPrintExcluded(cardDb.getCard(name)), name);
        }
    }

    @Test
    public void aCardWithOnlyExcludedPrintsKeepsOne() {
        cardDb.setPrintExclusion((edition, collectorNumber) -> true);
        for (String name : CARD_NAMES) {
            assertNotNull(cardDb.getUniqueByName(name), name);
            assertNotNull(cardDb.getCard(name), name);
        }
    }

    @Test
    public void universesBeyondTagsAreReadForSetsAndSinglePrints() {
        CardEdition sld = edition("SLD");
        assertTrue(sld.hasTag(UB, "143"));  // Rick, Steadfast Leader
        assertFalse(sld.hasTag(UB, "1"));   // Snow-Covered Plains
        // Death Baron's back face, listed as 1458☇, shares the tag of its front, 1458
        assertTrue(sld.hasTag(UB, "1458☇"));
        assertFalse(sld.hasTag(UB));  // SLD is tagged per print, not as a whole
        assertTrue(edition("LTR").hasTag(UB));
    }

    @Test
    public void aRequestByNameKeepsThatName() {
        BiPredicate<CardEdition, String> excludeUb = (edition, collectorNumber) ->
                edition.hasTag(UB, collectorNumber);
        cardDb.setPrintExclusion(excludeUb);
        PaperCard rick = cardDb.getCard("Rick, Steadfast Leader");
        assertNotNull(rick);
        // every print named Rick is UB, and the allowed Universes Within reprint shows the name Greymond instead
        assertEquals(rick.getDisplayName(), "Rick, Steadfast Leader");
    }
}
