package forge.screens.deckeditor.controllers;

import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.PaperCard;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class DraftPicksSectionsTest {
    @Test
    public void normalPicksGoToMainAndAlternatePicksGoToSideboard() {
        assertEquals(DraftPicksSections.getPickSection(false), DeckSection.Main);
        assertEquals(DraftPicksSections.getPickSection(true), DeckSection.Sideboard);
    }

    @Test
    public void movesCopiesBothWaysWithoutChangingDraftedPool() {
        PaperCard card = PaperCard.FAKE_CARD;
        Deck draftDeck = new Deck();
        draftDeck.getOrCreate(DeckSection.Sideboard).add(card, 3);

        DraftPicksSections.move(draftDeck, DeckSection.Sideboard, List.of(Map.entry(card, 2)));
        assertSections(draftDeck, card, 2, 1);

        DraftPicksSections.move(draftDeck, DeckSection.Main, List.of(Map.entry(card, 1)));
        assertSections(draftDeck, card, 1, 2);

        DraftPicksSections.move(draftDeck, DeckSection.Sideboard, List.of(Map.entry(card, 10)));
        assertSections(draftDeck, card, 3, 0);

        Deck savedDeck = (Deck) draftDeck.copyTo("Draft");
        assertSections(savedDeck, card, 3, 0);
    }

    @Test
    public void doesNotRestoreCardRemovedByDraftModel() {
        PaperCard card = PaperCard.FAKE_CARD;
        Deck draftDeck = new Deck();
        draftDeck.getOrCreate(DeckSection.Sideboard).add(card);
        draftDeck.getOrCreate(DeckSection.Sideboard).remove(card);

        DraftPicksSections.move(draftDeck, DeckSection.Sideboard, List.of(Map.entry(card, 1)));

        assertEquals(draftDeck.getOrCreate(DeckSection.Main).count(card), 0);
        assertEquals(draftDeck.getOrCreate(DeckSection.Sideboard).count(card), 0);
    }

    private static void assertSections(Deck deck, PaperCard card, int main, int sideboard) {
        assertEquals(deck.getOrCreate(DeckSection.Main).count(card), main);
        assertEquals(deck.getOrCreate(DeckSection.Sideboard).count(card), sideboard);
        assertEquals(deck.getOrCreate(DeckSection.Main).countAll()
                + deck.getOrCreate(DeckSection.Sideboard).countAll(), 3);
    }
}
