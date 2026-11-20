package forge.deck;

import forge.ai.AITest;
import forge.item.PaperCard;
import forge.model.FModel;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class DeckFormatTest extends AITest {

    private static PaperCard card(final String name, final String set) {
        final PaperCard card = FModel.getMagicDb().getCommonCards().getCard(name, set);
        assertNotNull(card, name + "|" + set);
        return card;
    }

    private static Deck draftedFrom(final String set) {
        final Deck deck = new Deck("Commander Draft test");
        deck.getOrCreate(DeckSection.Commander).add(card("Rishkar, Peema Renegade", "CMM"));
        deck.getOrCreate(DeckSection.Commander).add(card("Pianna, Nomad Captain", "CMM"));
        deck.getMain().add(card("Sol Ring", set));
        deck.getOrCreate(DeckSection.Sideboard).add(card("Command Tower", set));
        return deck;
    }

    @Test
    public void monocoloredLegendsPartnerOnlyInCommanderMastersDraft() {
        // Neither legend has partner, so only the Commander Masters rule lets them lead together
        assertNull(DeckFormat.CommanderDraft.getCommanderConformanceProblem(draftedFrom("CMM")));
        assertEquals(DeckFormat.CommanderDraft.getCommanderConformanceProblem(draftedFrom("CMR")),
                "has an illegal commander partnership");
    }

    @Test
    public void onlyCommanderDraftSetsMakeACommanderDraft() {
        assertTrue(DeckFormat.isCommanderDraft(Arrays.asList(card("Sol Ring", "CMR"), card("Sol Ring", "CLB"))));
        // Commander 2021 is a preconstructed set, so it has no draft rules of its own
        assertFalse(DeckFormat.isCommanderDraft(Arrays.asList(card("Sol Ring", "C21"), card("Sol Ring", "CLB"))));
    }

    @Test
    public void freeCommandersFollowTheDraftedSets() {
        assertEquals(DeckFormat.getFreeCommanders(Collections.singletonList(card("Sol Ring", "CMR"))),
                Collections.singletonList("The Prismatic Piper"));
        // Basic lands are added after the draft, so a Baldur's Gate Forest is not a drafted Baldur's Gate card
        assertEquals(DeckFormat.getFreeCommanders(Arrays.asList(card("Sol Ring", "CMR"), card("Forest", "CLB"))),
                Collections.singletonList("The Prismatic Piper"));
        assertEquals(DeckFormat.getFreeCommanders(Arrays.asList(card("Sol Ring", "CMR"), card("Sol Ring", "CLB"))),
                Arrays.asList("The Prismatic Piper", "Faceless One"));
    }
}
