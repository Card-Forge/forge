package forge.gamemodes.rogue;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import forge.StaticData;
import forge.card.CardMockTestCase;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.gamemodes.rogue.path.NodeBazaar;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gamemodes.rogue.path.RoguePath;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.ImageUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.GZIPOutputStream;
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
    public void runSaveRoundTrip() throws IOException {
        String name = "Gonti Curio - Tarnished Relic";
        PaperCard curio = RogueConfig.getRogueCard(name);
        assertNotNull(curio);
        assertSame(RogueConfig.getCard(name, null, null), curio);
        PaperCard foiledCurio = curio.getFoiled();

        PaperCard solRing = testData.getCommonCards().getCard("Sol Ring", "CMM");
        assertNotNull(solRing);
        Deck startDeck = new Deck("Test Deck");
        startDeck.getMain().add(solRing);
        RogueDeck rogueDeck = new RogueDeck();
        rogueDeck.setStartDeck(startDeck);

        RogueRun run = new RogueRun(rogueDeck);
        run.getCurrentDeck().getMain().add(foiledCurio, 2);
        run.setCurrentGold(7);

        NodePlanebound completed = new NodePlanebound();
        completed.setCompleted(true);
        NodeBazaar bazaar = new NodeBazaar();
        bazaar.setRowIndex(1);
        run.setPath(new RoguePath(new ArrayList<>(List.of(completed, bazaar))));
        run.setCurrentNodeIndex(1);

        Path save = Files.createTempFile("rogue-run-", RogueIO.SUFFIX_DATA);
        RogueRun loaded;
        try {
            try (GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(save))) {
                RogueIO.getSerializer(false).toXML(run, output);
            }
            loaded = RogueIO.loadRun(save.toFile());
        } finally {
            Files.deleteIfExists(save);
        }

        assertNotNull(loaded);
        assertEquals(loaded.getCurrentGold(), 7);
        assertEquals(loaded.getCurrentNodeIndex(), 1);
        assertTrue(loaded.getPath().getNode(0).isCompleted());
        assertTrue(loaded.getCurrentNode() instanceof NodeBazaar);
        assertEquals(loaded.getCurrentNode().getRowIndex(), 1);

        CardPool loadedDeck = loaded.getCurrentDeck().getMain();
        assertEquals(loadedDeck.countAll(), 3);
        assertEquals(loadedDeck.count(solRing), 1);
        List<PaperCard> loadedCurios = loadedDeck.toFlatList().stream()
            .filter(card -> card.getName().equals(name)).toList();
        assertEquals(loadedCurios, List.of(foiledCurio, foiledCurio));
        assertTrue(loadedCurios.stream().allMatch(PaperCard::isFoil));

        assertNull(testData.getCommonCards().getCard(name));
        assertSame(ImageUtil.getPaperCardFromImageKey(curio.getImageKey(false)), curio);
    }

    @Test
    public void basicLandPrintingFallsBack() {
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
    public void randomPrintsRespectEligibility() {
        Set<String> names = Set.of("Sol Ring", "Battle Angels of Tyr");
        List<PaperCard> cards = RogueConfig.getRandomCardsForEdition(
                card -> names.contains(card.getName()), "CMM");

        assertEquals(cards.size(), 1);
        assertEquals(cards.get(0).getName(), "Sol Ring");
        assertEquals(cards.get(0).getEdition(), "CMM");
    }

    @Test
    public void rejectsMythicOnlyCards() {
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
