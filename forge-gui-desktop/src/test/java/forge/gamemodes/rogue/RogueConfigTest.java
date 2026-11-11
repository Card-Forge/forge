package forge.gamemodes.rogue;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import forge.StaticData;
import forge.card.CardMockTestCase;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.ItemPool;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RogueConfigTest extends CardMockTestCase {
    private StaticData testData;
    private Predicate<PaperCard> originalCommanderPredicate;

    @BeforeMethod
    public void allowCommanderLegalCards() {
        testData = FModel.getMagicDb();
        originalCommanderPredicate = testData.getCommanderPredicate();
        // RogueConfig's own filters are under test; the card fixture does not initialize formats.
        testData.setCommanderPredicate(card -> true);
    }

    @AfterMethod(alwaysRun = true)
    public void restoreCommanderLegality() {
        if (testData != null) {
            testData.setCommanderPredicate(originalCommanderPredicate);
        }
    }

    @Test
    public void basicLandEditionPrefersDeckSettingThenCommanderThenFallback() {
        PaperCard commander = FModel.getMagicDb().getCommonCards().getCard("Sol Ring", "CMM");
        assertNotNull(commander);

        assertEquals(RogueConfig.getBasicLandEdition("M3C", commander), "M3C");
        assertEquals(RogueConfig.getBasicLandEdition(" ", commander), "CMM");
        assertEquals(RogueConfig.getBasicLandEdition(null, commander), "CMM");
        assertEquals(RogueConfig.getBasicLandEdition(null, null), "MH3");
    }

    @Test
    public void basicLandPrintPrefersRequestedEditionAndFallsBackByLandType() {
        PaperCard plains = RogueConfig.getBasicLandPrint("Plains", "MH3");
        assertNotNull(plains);
        assertEquals(plains.getEdition(), "MH3");
        assertEquals(plains.getArtIndex(), 1);

        PaperCard fallbackPlains = RogueConfig.getBasicLandPrint("Plains", "NO_SUCH_EDITION");
        assertNotNull(fallbackPlains);
        assertEquals(fallbackPlains.getEdition(), "MH3");

        PaperCard fallbackWastes = RogueConfig.getBasicLandPrint("Wastes", "NO_SUCH_EDITION");
        assertNotNull(fallbackWastes);
        assertEquals(fallbackWastes.getEdition(), "M3C");
        assertNull(RogueConfig.getBasicLandPrint("NO_SUCH_BASIC_LAND", "MH3"));
    }

    @Test
    public void basicLandsFromEditionIncludeOnlyBasicsFromThatEdition() {
        PaperCard plains = FModel.getMagicDb().getCommonCards().getCard("Plains", "MH3");
        PaperCard wastes = FModel.getMagicDb().getCommonCards().getCard("Wastes", "M3C");
        PaperCard solRing = FModel.getMagicDb().getCommonCards().getCard("Sol Ring", "CMM");
        assertNotNull(plains);
        assertNotNull(wastes);
        assertNotNull(solRing);

        ItemPool<PaperCard> pool = ItemPool.createFrom(List.of(plains, wastes, solRing), PaperCard.class);
        fModelMock.when(FModel::getAllCards).thenReturn(pool);

        assertEquals(RogueConfig.getBasicLandsFromEdition("MH3"), List.of(plains));
        assertEquals(RogueConfig.getBasicLandsFromEdition("M3C"), List.of(wastes));
        assertTrue(RogueConfig.getBasicLandsFromEdition("CMM").isEmpty());
        assertTrue(RogueConfig.getBasicLandsFromEdition(null).isEmpty());
    }

    @Test
    public void randomCardsExcludeMultiplayerTextAndPreferRequestedPrinting() {
        Set<String> names = Set.of("Sol Ring", "Battle Angels of Tyr");
        List<PaperCard> cards = RogueConfig.getRandomCardsForEdition(
                card -> names.contains(card.getName()), "CMM");

        assertEquals(cards.size(), 1);
        assertEquals(cards.get(0).getName(), "Sol Ring");
        assertEquals(cards.get(0).getEdition(), "CMM");
    }

    @Test
    public void nonMythicRandomPrintRejectsMythicOnlyCards() {
        PaperCard nonMythic = FModel.getMagicDb().getCommonCards().getCard("Sol Ring");
        assertNotNull(nonMythic);
        PaperCard chosen = RogueConfig.getNonMythicRandomPrint(nonMythic, card -> true, "CMM");
        assertNotNull(chosen);
        assertEquals(chosen.getEdition(), "CMM");

        PaperCard mythic = FModel.getMagicDb().getCommonCards().getCard("Atraxa, Grand Unifier");
        assertNotNull(mythic);
        assertNull(RogueConfig.getNonMythicRandomPrint(mythic, card -> true, "ONE"));
    }
}
