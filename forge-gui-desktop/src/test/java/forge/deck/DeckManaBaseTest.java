package forge.deck;

import java.util.List;
import java.util.stream.Collectors;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.ai.AITest;
import forge.card.MagicColor;
import forge.item.PaperCard;

/**
 * The mana base check of generated decks, on the problem seen in play: a blue-black control deck with Explore in it and
 * no way to make green mana, so that its hand was clogged with dead cards while its fetch land could only find Islands.
 */
public class DeckManaBaseTest extends AITest {
    private static PaperCard card(final String name) {
        final PaperCard card = StaticData.instance().getCommonCards().getUniqueByName(name);
        AssertJUnit.assertNotNull(name, card);
        return card;
    }

    private static void add(final Deck deck, final String name, final int n) {
        deck.getMain().add(card(name), n);
    }

    /** The control deck of the video: blue and black spells and lands, three Explore, a fetch land and no Forest. */
    private Deck blueBlackWithStrayGreen() {
        initAndCreateGame(); // loads the card database
        final Deck deck = new Deck("test");
        add(deck, "Creeping Tar Pit", 4);
        add(deck, "Drowned Catacomb", 4);
        add(deck, "Misty Rainforest", 4);
        add(deck, "Island", 8);
        add(deck, "Swamp", 4);
        add(deck, "Jace Beleren", 4);
        add(deck, "Sea Gate Oracle", 4);
        add(deck, "Inquisition of Kozilek", 4);
        add(deck, "Brainstorm", 4);
        add(deck, "Counterspell", 4);
        add(deck, "Explore", 3);
        add(deck, "Dark Ritual", 4);
        add(deck, "Fatal Push", 3);
        add(deck, "Opt", 2);
        return deck;
    }

    @Test
    public void landsOfBlueAndBlackMakeNoGreen() {
        final Deck deck = blueBlackWithStrayGreen();
        final byte colors = DeckManaBase.landColors(deck.getMain());
        AssertJUnit.assertEquals(MagicColor.BLUE | MagicColor.BLACK, colors);
    }

    @Test
    public void aFetchLandFindsOnlyWhatIsInTheDeck() {
        final Deck deck = blueBlackWithStrayGreen();
        // "Forest or Island": Island is there, Forest is not
        AssertJUnit.assertEquals(0, DeckManaBase.landColors(deck.getMain()) & MagicColor.GREEN);
        add(deck, "Forest", 1);
        AssertJUnit.assertEquals("with a Forest in the deck the fetch land finds it as well, and the Forest is a source itself",
                MagicColor.GREEN, DeckManaBase.landColors(deck.getMain()) & MagicColor.GREEN);
    }

    @Test
    public void detectsTheSpellsItCannotCast() {
        final Deck deck = blueBlackWithStrayGreen();
        final List<String> dead = DeckManaBase.uncastable(deck.getMain()).stream().map(PaperCard::getName).collect(Collectors.toList());
        AssertJUnit.assertEquals("only Explore is dead: " + dead, java.util.Collections.singletonList("Explore"), dead);
    }

    @Test
    public void swapsAFewStraySpellsForWhatTheDeckCanCast() {
        final Deck deck = blueBlackWithStrayGreen();
        final int size = deck.getMain().countAll();
        final int changed = DeckManaBase.fix(deck);
        AssertJUnit.assertTrue(changed > 0);
        AssertJUnit.assertEquals("the deck keeps its size", size, deck.getMain().countAll());
        AssertJUnit.assertEquals("no dead cards are left", 0, DeckManaBase.uncastable(deck.getMain()).size());
        AssertJUnit.assertEquals(0, deck.getMain().countByName("Explore"));
        AssertJUnit.assertEquals("they are not lost, they wait in the sideboard", 3, deck.get(DeckSection.Sideboard).countByName("Explore"));
    }

    @Test
    public void aDeckThatIsFineIsLeftAlone() {
        final Deck deck = blueBlackWithStrayGreen();
        deck.getMain().remove(card("Explore"), 3);
        deck.getMain().add(card("Island"), 3);
        final String before = deck.getMain().toCardList("\n");
        AssertJUnit.assertEquals(0, DeckManaBase.fix(deck));
        AssertJUnit.assertEquals(before, deck.getMain().toCardList("\n"));
    }

    @Test
    public void aColourThatManySpellsNeedGetsLandsInsteadOfBeingDropped() {
        final Deck deck = blueBlackWithStrayGreen();
        deck.getMain().remove(card("Opt"), 2);
        deck.getMain().remove(card("Fatal Push"), 3);
        add(deck, "Explore", 4);
        add(deck, "Rampant Growth", 1);
        final int size = deck.getMain().countAll();
        DeckManaBase.fix(deck);
        AssertJUnit.assertEquals(size, deck.getMain().countAll());
        AssertJUnit.assertEquals("seven green spells make green a colour of the deck", 0, DeckManaBase.uncastable(deck.getMain()).size());
        AssertJUnit.assertEquals(7, deck.getMain().countByName("Explore"));
        AssertJUnit.assertTrue("with real Forests, which the fetch land finds as well", deck.getMain().countByName("Forest") >= 3);
    }

    @Test
    public void aManaCreatureThatCanBeCastMakesItsColourAvailable() {
        initAndCreateGame();
        final Deck deck = new Deck("test");
        add(deck, "Island", 12);
        add(deck, "Birds of Paradise", 4); // costs {G} itself: with no green source it is as dead as the rest
        add(deck, "Explore", 4);
        add(deck, "Brainstorm", 4);
        AssertJUnit.assertEquals("Birds and Explore both need green mana that nothing makes",
                2, DeckManaBase.uncastable(deck.getMain()).size());
        add(deck, "Forest", 4);
        AssertJUnit.assertEquals(0, DeckManaBase.uncastable(deck.getMain()).size());
    }

    @Test
    public void colourlessCostsNeedColourlessLands() {
        initAndCreateGame();
        final Deck deck = new Deck("test");
        add(deck, "Island", 12);
        add(deck, "Thought-Knot Seer", 4); // {3}{C}
        add(deck, "Brainstorm", 4);
        AssertJUnit.assertEquals("nothing here makes colourless mana", 1, DeckManaBase.uncastable(deck.getMain()).size());
        add(deck, "Eldrazi Temple", 4);
        AssertJUnit.assertEquals(0, DeckManaBase.uncastable(deck.getMain()).size());
    }

    @Test
    public void anyColorLandsMakeEveryColour() {
        initAndCreateGame();
        final Deck deck = new Deck("test");
        add(deck, "Mana Confluence", 4);
        add(deck, "Island", 8);
        add(deck, "Explore", 4);
        add(deck, "Lightning Bolt", 4);
        AssertJUnit.assertEquals(0, DeckManaBase.uncastable(deck.getMain()).size());
    }

    /** The ramp deck seen in play: Urza lands, Karn, Ancient Stirrings, and Chromatic Star/Sphere that the AI never casts. */
    private Deck rampWithCardsTheAiNeverPlays() {
        initAndCreateGame();
        final Deck deck = new Deck("ramp");
        add(deck, "Urza's Mine", 4);
        add(deck, "Urza's Tower", 4);
        add(deck, "Urza's Power Plant", 4);
        add(deck, "Forest", 6);
        add(deck, "Chromatic Star", 4);
        add(deck, "Chromatic Sphere", 3);
        add(deck, "Ancient Stirrings", 4);
        add(deck, "Karn Liberated", 2);
        add(deck, "Wurmcoil Engine", 4);
        add(deck, "Mind Stone", 4);
        add(deck, "Wayfarer's Bauble", 4);
        add(deck, "Oblivion Stone", 3);
        return deck;
    }

    @Test
    public void cardsTheAiNeverPlaysAreCountedAsDead() {
        final Deck deck = rampWithCardsTheAiNeverPlays();
        final List<String> dead = DeckManaBase.uncastable(deck.getMain()).stream().map(PaperCard::getName).collect(Collectors.toList());
        AssertJUnit.assertTrue(dead.toString(), dead.contains("Chromatic Star"));
        AssertJUnit.assertTrue(dead.toString(), dead.contains("Chromatic Sphere"));
        AssertJUnit.assertFalse("Mind Stone is a normal card for the AI: " + dead, dead.contains("Mind Stone"));
    }

    @Test
    public void fixSwapsThemForCardsTheAiPlaysAndKeepsTheSize() {
        final Deck deck = rampWithCardsTheAiNeverPlays();
        final int size = deck.getMain().countAll();
        DeckManaBase.fix(deck);
        AssertJUnit.assertEquals(size, deck.getMain().countAll());
        AssertJUnit.assertEquals(0, deck.getMain().count(card("Chromatic Star")));
        AssertJUnit.assertEquals(0, deck.getMain().count(card("Chromatic Sphere")));
        AssertJUnit.assertTrue("nothing the AI cannot play is left", DeckManaBase.uncastable(deck.getMain()).isEmpty());
    }
}
