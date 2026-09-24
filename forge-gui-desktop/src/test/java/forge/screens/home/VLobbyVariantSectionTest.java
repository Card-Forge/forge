package forge.screens.home;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.model.FModel;

/**
 * Issue #11800: picking a new main deck, or a new sleeve, in the lobby used to drop the planar
 * deck already picked for that player, and they started Planechase with an empty planar deck.
 */
public class VLobbyVariantSectionTest extends AITest {

    private static CardPool planes(String... names) {
        CardPool pool = new CardPool();
        for (String name : names) {
            pool.add(FModel.getMagicDb().getVariantCards().getCard(name));
        }
        return pool;
    }

    private static Deck lobbyDeckWithPlanes(CardPool planes) {
        Deck deck = new Deck("Lobby");
        deck.putSection(DeckSection.Planes, planes);
        return deck;
    }

    @Test
    public void aRandomPlanarDeckIsKeptRatherThanRerolled() {
        CardPool picked = planes("Minamo", "The Lux Foundation Library");
        assertSame(VLobby.variantSectionToKeep("Random", lobbyDeckWithPlanes(picked), DeckSection.Planes), picked);
    }

    @Test
    public void aChosenPlanarDeckIsKept() {
        CardPool picked = planes("Minamo");
        assertSame(VLobby.variantSectionToKeep(new Deck("Planar deck"), lobbyDeckWithPlanes(picked), DeckSection.Planes), picked);
    }

    /** "Use deck's planes section" means the new deck's own planes, so the selector has to run again. */
    @Test
    public void usingTheDecksOwnSectionPicksAgain() {
        assertNull(VLobby.variantSectionToKeep("Use deck's planes section", lobbyDeckWithPlanes(planes("Minamo")), DeckSection.Planes));
        assertNull(VLobby.variantSectionToKeep("Use deck's default avatar (random if unavailable)", new Deck(), DeckSection.Avatar));
    }

    @Test
    public void nothingPickedBeforePicksAgain() {
        assertNull(VLobby.variantSectionToKeep("Random", null, DeckSection.Planes));
        assertNull(VLobby.variantSectionToKeep("Random", new Deck(), DeckSection.Planes));
    }

    @Test
    public void aNewSleeveKeepsTheLobbyPlanarDeck() {
        CardPool picked = planes("Minamo", "The Lux Foundation Library");
        Deck lobbyDeck = lobbyDeckWithPlanes(picked);
        Deck saved = new Deck("Saved");
        saved.setSleeveArtKey("sleeve-key");
        saved.setSleeveArtOffset(3);

        Deck sent = VLobby.withSleeveOf(lobbyDeck, saved);

        assertEquals(sent.get(DeckSection.Planes).countAll(), 2);
        assertEquals(sent.getSleeveArtKey(), "sleeve-key");
        assertEquals(sent.getSleeveArtOffset(), 3);
        assertEquals(lobbyDeck.getSleeveArtKey(), new Deck().getSleeveArtKey(), "the lobby's own deck isn't changed in place");
    }

    /** With no lobby deck yet, send a copy so the saved deck object never becomes the lobby's own. */
    @Test
    public void aNewSleeveWithoutALobbyDeckCopiesTheSavedDeck() {
        Deck saved = new Deck("Saved");
        saved.setSleeveArtKey("sleeve-key");

        Deck sent = VLobby.withSleeveOf(null, saved);

        assertEquals(sent.getSleeveArtKey(), "sleeve-key");
        assertEquals(sent.getName(), "Saved");
        assertNotSame(sent, saved);
    }
}
