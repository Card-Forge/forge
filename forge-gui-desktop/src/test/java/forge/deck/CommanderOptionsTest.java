package forge.deck;

import forge.ai.AITest;
import forge.deck.io.DeckSerializer;
import forge.item.PaperCard;
import forge.model.FModel;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class CommanderOptionsTest extends AITest {

    private static PaperCard card(final String name) {
        final PaperCard card = FModel.getMagicDb().getCommonCards().getCard(name);
        assertNotNull(card, name);
        return card;
    }

    /** An Abzan (WBG) deck led by Ghave, with a few other legends among its cards. */
    private static Deck abzanDeck() {
        final Deck deck = new Deck("Abzan test");
        deck.getOrCreate(DeckSection.Commander).add(card("Ghave, Guru of Spores"));
        final CardPool main = deck.getMain();
        for (final String name : Arrays.asList("Anafenza, the Foremost", "Azusa, Lost but Seeking",
                "Tymna the Weaver", "Ikra Shidiqi, the Usurper", "Raised by Giants",
                "Llanowar Elves", "Swords to Plowshares", "Doom Blade")) {
            main.add(card(name));
        }
        main.add(card("Forest"), 3);
        return deck;
    }

    private static List<String> names(final List<CommanderOptions.Option> options) {
        final List<String> names = new ArrayList<>();
        for (final CommanderOptions.Option option : options) {
            names.add(option.getName() + ":" + option.getKind());
        }
        return names;
    }

    @Test
    public void optionsListDefaultThenSuggestedThenOthers() {
        final Deck deck = abzanDeck();
        // A card that isn't in the deck and one that can't lead it are ignored
        deck.addAltCommander("Sol Ring");
        deck.addAltCommander("Azusa, Lost but Seeking");
        deck.addAltCommander("Anafenza, the Foremost");

        final List<CommanderOptions.Option> options = CommanderOptions.getOptions(deck, DeckFormat.Commander);

        // Azusa is mono-green, so the deck's white and black cards rule her out.
        // Raised by Giants is a Background, which can't be a commander on its own.
        // Tymna and Ikra only cover the deck's colors together, as partners.
        assertEquals(names(options), Arrays.asList(
                "Ghave, Guru of Spores:DEFAULT",
                "Anafenza, the Foremost:SUGGESTED",
                "Ikra Shidiqi, the Usurper:OTHER",
                "Tymna the Weaver:OTHER"));
    }

    @Test
    public void partnerThatCannotLeadAloneOffersItsPartner() {
        final Deck deck = abzanDeck();
        final PaperCard tymna = card("Tymna the Weaver");

        assertFalse(CommanderOptions.canLeadAlone(deck, tymna, DeckFormat.Commander));
        assertTrue(CommanderOptions.canLeadAlone(deck, card("Anafenza, the Foremost"), DeckFormat.Commander));
        assertEquals(CommanderOptions.getPartnerOptions(deck, tymna, DeckFormat.Commander),
                Collections.singletonList(card("Ikra Shidiqi, the Usurper")));
        assertTrue(CommanderOptions.getPartnerOptions(deck, card("Anafenza, the Foremost"), DeckFormat.Commander).isEmpty());
    }

    @Test
    public void withCommandersSwapsCardsWithoutChangingTheBaseDeck() {
        final Deck deck = abzanDeck();
        final int mainSize = deck.getMain().countAll();
        final PaperCard ghave = card("Ghave, Guru of Spores");
        final PaperCard anafenza = card("Anafenza, the Foremost");

        final Deck swapped = CommanderOptions.withCommanders(deck, Collections.singletonList(anafenza));

        assertEquals(swapped.getName(), deck.getName());
        assertEquals(swapped.getCommanders(), Collections.singletonList(anafenza));
        assertEquals(swapped.getMain().countAll(), mainSize);
        assertTrue(swapped.getMain().contains(ghave));
        assertFalse(swapped.getMain().contains(anafenza));
        // The deck the player picked stays as it was
        assertEquals(deck.getCommanders(), Collections.singletonList(ghave));
        assertTrue(deck.getMain().contains(anafenza));

        // A single commander swapped for a partner pair takes both out of the main deck
        final PaperCard tymna = card("Tymna the Weaver");
        final PaperCard ikra = card("Ikra Shidiqi, the Usurper");
        final Deck pair = CommanderOptions.withCommanders(swapped, Arrays.asList(tymna, ikra));
        assertEquals(pair.getCommanders().size(), 2);
        assertEquals(pair.getMain().countAll(), mainSize - 1);
        assertTrue(pair.getMain().contains(ghave));
        assertTrue(pair.getMain().contains(anafenza));
        assertNull(DeckFormat.Commander.getCommanderConformanceProblem(pair));
    }

    @Test
    public void defaultPartnerPairIsOneOptionAndEachHalfCanTakeAnotherPartner() {
        final PaperCard tymna = card("Tymna the Weaver");
        final PaperCard ikra = card("Ikra Shidiqi, the Usurper");
        final PaperCard reyhan = card("Reyhan, Last of the Abzan");
        final Deck deck = CommanderOptions.withCommanders(abzanDeck(), Arrays.asList(tymna, ikra));
        deck.getMain().add(reyhan);

        final List<CommanderOptions.Option> options = CommanderOptions.getOptions(deck, DeckFormat.Commander);

        assertEquals(options.get(0).getKind(), CommanderOptions.Kind.DEFAULT);
        assertEquals(options.get(0).getCommanders().size(), 2);
        // Each half of the default pair comes right after it, so it can be given another partner
        assertEquals(names(options.subList(1, options.size())), Arrays.asList(
                "Ikra Shidiqi, the Usurper:OTHER",
                "Tymna the Weaver:OTHER",
                "Anafenza, the Foremost:OTHER",
                "Ghave, Guru of Spores:OTHER",
                "Reyhan, Last of the Abzan:OTHER"));
        assertEquals(CommanderOptions.getPartnerOptions(deck, tymna, DeckFormat.Commander), Arrays.asList(ikra, reyhan));
    }

    @Test
    public void commanderConformanceReportsOffColorCards() {
        final Deck deck = CommanderOptions.withCommanders(abzanDeck(),
                Collections.singletonList(card("Azusa, Lost but Seeking")));

        final String problem = DeckFormat.Commander.getCommanderConformanceProblem(deck);
        assertNotNull(problem);
        assertTrue(problem.startsWith("contains one or more cards that do not match the commanders color identity"), problem);
        // The full deck check reports the same problem before looking at the deck size
        assertEquals(DeckFormat.Commander.getDeckConformanceProblem(deck), problem);
    }

    @Test
    public void altCommandersSurviveSavingAndLoading() throws IOException {
        final Deck deck = abzanDeck();
        deck.addAltCommander("Anafenza, the Foremost");
        deck.addAltCommander("Tymna the Weaver");

        final File file = File.createTempFile("commander-options", ".dck");
        try {
            DeckSerializer.writeDeck(deck, file);
            final Deck loaded = DeckSerializer.fromFile(file);
            assertEquals(loaded.getAltCommanders(), Arrays.asList("Anafenza, the Foremost", "Tymna the Weaver"));
            assertEquals(new Deck(loaded).getAltCommanders(), loaded.getAltCommanders());
        } finally {
            assertTrue(file.delete());
        }
    }
}
