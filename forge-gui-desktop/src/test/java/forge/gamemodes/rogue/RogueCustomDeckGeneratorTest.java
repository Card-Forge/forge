package forge.gamemodes.rogue;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import forge.StaticData;
import forge.card.CardMockTestCase;
import forge.card.CardRarity;
import forge.deck.CardRelationMatrixGenerator;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.deck.DeckgenUtil;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.MyRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RogueCustomDeckGeneratorTest extends CardMockTestCase {
    private StaticData testData;
    private Predicate<PaperCard> previousCommanderPredicate;
    private Random previousRandom;
    private HashMap<String, List<Map.Entry<PaperCard, Integer>>> previousMatrix;

    @BeforeMethod
    public void prepareGeneration() {
        testData = FModel.getMagicDb();
        previousCommanderPredicate = testData.getCommanderPredicate();
        testData.setCommanderPredicate(card -> true);
        previousRandom = MyRandom.getRandom();
        MyRandom.setRandom(new Random(0));
        previousMatrix = CardRelationMatrixGenerator.cardPools.put(
            DeckFormat.Commander.toString(), new HashMap<>());
    }

    @AfterMethod(alwaysRun = true)
    public void restoreGeneration() {
        if (testData != null) {
            testData.setCommanderPredicate(previousCommanderPredicate);
        }
        MyRandom.setRandom(previousRandom);
        if (previousMatrix == null) {
            CardRelationMatrixGenerator.cardPools.remove(DeckFormat.Commander.toString());
        } else {
            CardRelationMatrixGenerator.cardPools.put(DeckFormat.Commander.toString(), previousMatrix);
        }
    }

    @Test
    public void retryReachesStarterThemeTarget() {
        Deck normal = createOffThemeDeck();
        List<PaperCard> themeCards = List.of(getCard("Youthful Valkyrie"),
            getCard("Inspiring Overseer"), getCard("Angel of Vitality"),
            getCard("Serra Angel"), getCard("Serra Avenger"));

        RogueDeck result = generateStartingDeck(normal, themeCards);

        assertNotNull(result);
        assertEquals(result.getStartDeck().getMain().countAll(), 33);
        assertEquals(result.getStartDeck().getCommanders(), normal.getCommanders());
        for (PaperCard card : themeCards) {
            assertEquals(result.getStartDeck().getMain().count(card), 1,
                "The retry must select each eligible theme card from the later source");
        }
    }

    @Test
    public void partialThemeRetryKeepsOriginalStarter() {
        Deck normal = createOffThemeDeck();
        PaperCard expensiveThemeCard = getCard("Angel of the Ruins");
        assertTrue(expensiveThemeCard.getRules().getManaCost().getCMC() > 6);
        List<PaperCard> themeCards = List.of(getCard("Youthful Valkyrie"),
            getCard("Inspiring Overseer"), getCard("Angel of Vitality"),
            getCard("Serra Angel"), expensiveThemeCard);

        RogueDeck result = generateStartingDeck(normal, themeCards);

        assertNotNull(result);
        assertEquals(result.getStartDeck().getMain().toFlatList().stream().sorted().toList(),
            normal.getMain().toFlatList().stream().sorted().toList(),
            "A retry with only four usable theme cards must keep the original card list");
    }

    private Deck createOffThemeDeck() {
        Deck deck = new Deck("Controlled starter");
        deck.getOrCreate(DeckSection.Commander).add(getCard("Giada, Font of Hope"));
        for (String name : List.of("Savannah Lions", "Elite Vanguard", "Leonin Vanguard",
            "Suntail Hawk", "Healer's Hawk", "Soul Warden", "Soul's Attendant",
            "Thraben Inspector", "Novice Inspector", "Doomed Traveler", "Selfless Savior",
            "Dauntless Bodyguard", "Benalish Hero", "Eager Cadet", "Loyal Sentry",
            "Soldier of the Pantheon", "Mother of Runes", "Giver of Runes", "Leonin Skyhunter")) {
            PaperCard card = getCard(name);
            assertTrue(card.getRules().getManaCost().getCMC() <= 3);
            deck.getMain().add(card);
        }
        assertFalse(deck.getCommanders().get(0).getRules().getAiHints().getDeckNeeds()
            .filter(deck.getMain().toFlatList()).iterator().hasNext());
        deck.getMain().add(getCard("Plains"), 14);
        return deck;
    }

    private RogueDeck generateStartingDeck(Deck normal, List<PaperCard> themeCards) {
        PaperCard commander = normal.getCommanders().get(0);
        PaperCard plains = getCard("Plains");
        List<PaperCard> candidates = new ArrayList<>(normal.getMain().toFlatList());
        candidates.addAll(themeCards);
        RogueDeck selected = new RogueDeck();
        selected.setName(normal.getName());
        selected.setStartDeck(normal);
        selected.setLandEdition(plains.getEdition());

        try (MockedStatic<RogueConfig> config = Mockito.mockStatic(RogueConfig.class);
                MockedStatic<DeckgenUtil> deckgen = Mockito.mockStatic(DeckgenUtil.class)) {
            config.when(() -> RogueConfig.getRandomCardsForEdition(Mockito.any(),
                Mockito.eq(commander.getEdition()))).thenAnswer(invocation -> {
                    Predicate<PaperCard> eligible = invocation.getArgument(0);
                    return candidates.stream().filter(eligible).distinct().toList();
                });
            config.when(() -> RogueConfig.getBasicLandEdition(plains.getEdition(), commander))
                .thenReturn(plains.getEdition());
            config.when(() -> RogueConfig.getBasicLandPrint("Plains", plains.getEdition()))
                .thenReturn(plains);
            deckgen.when(() -> DeckgenUtil.generateRandomCommanderDeck(Mockito.eq(commander),
                Mockito.eq(DeckFormat.Commander), Mockito.eq(false), Mockito.eq(false), Mockito.anyInt()))
                .thenReturn(normal);

            RogueDeck result = RogueCustomDeckGenerator.generateStartingDeck(selected);

            deckgen.verify(() -> DeckgenUtil.generateRandomCommanderDeck(Mockito.eq(commander),
                Mockito.eq(DeckFormat.Commander), Mockito.eq(false), Mockito.eq(false), Mockito.anyInt()),
                Mockito.times(1));
            return result;
        }
    }

    private PaperCard getCard(String name) {
        PaperCard card = testData.getCommonCards().getAllCards(name).stream()
            .filter(candidate -> candidate.getRarity() != CardRarity.MythicRare).findFirst().orElse(null);
        assertNotNull(card, "Missing fixture card " + name);
        return card;
    }
}
