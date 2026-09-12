package forge.gamemodes.limited;

import forge.deck.CardPool;
import forge.deck.DeckSection;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.net.TestUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static forge.gamemodes.limited.LimitedPlayer.Effect.*;
import static org.testng.Assert.*;

public class ConspiracyDraftTest {

    @BeforeClass
    public void initCards() {
        TestUtils.ensureFModelInitialized();
    }

    static PaperCard card(String name) {
        PaperCard c = FModel.getMagicDb().getCommonCards().getCard(name);
        assertNotNull(c, "missing card " + name);
        return c;
    }

    private BoosterDraft draft;
    private List<LimitedPlayer> seats;
    private final ArrayDeque<List<Integer>> answers = new ArrayDeque<>();
    private final List<DraftPrompt> asked = new ArrayList<>();
    private final List<Consumer<List<Integer>>> deferred = new ArrayList<>();
    private boolean deferAnswers;
    private int nextPackId = 1000;

    @BeforeMethod
    public void newDraft() {
        draft = new BoosterDraft(LimitedPoolType.Full, 4);
        draft.setHumanSeats(Set.of(0, 1, 2, 3));
        seats = draft.getAllPlayers();
        answers.clear();
        asked.clear();
        deferred.clear();
        deferAnswers = false;
        for (LimitedPlayer p : seats) {
            p.setPromptSink((prompt, blocking, onAnswer) -> {
                asked.add(prompt);
                if (deferAnswers) {
                    deferred.add(onAnswer);
                } else {
                    onAnswer.accept(answers.isEmpty() ? prompt.defaultAnswer() : answers.poll());
                }
            });
        }
    }

    DraftPack packOf(String... names) {
        List<PaperCard> cards = new ArrayList<>();
        for (String n : names) cards.add(card(n));
        return new DraftPack(cards, nextPackId++);
    }

    DraftPack give(LimitedPlayer p, String... names) {
        DraftPack pack = packOf(names);
        p.receiveOpenedPack(pack);
        return pack;
    }

    Boolean pick(LimitedPlayer p, String name) {
        return p.draftCard(card(name), DeckSection.Sideboard, null);
    }

    Boolean pickWith(LimitedPlayer p, String name, String sourceName) {
        DraftAction variant = DraftAction.pick(card(sourceName), card(name));
        assertTrue(p.getActions(p.nextChoice()).contains(variant), "variant not offered: " + sourceName);
        return p.draftCard(card(name), DeckSection.Sideboard, variant);
    }

    void putFaceUp(LimitedPlayer p, String name) {
        give(p, name, "Forest");
        pick(p, name);
        p.passPack();
        assertTrue(p.getFaceUp().contains(card(name)));
    }

    List<String> poolNames(LimitedPlayer p) {
        return p.getPoolCards().stream().map(PaperCard::getName).collect(Collectors.toList());
    }

    // Abilities are found by exact sentence match, so a reworded card script would lose its ability without any error
    @Test
    public void everyDraftSentenceMapsToAnEffect() {
        assertEquals(LimitedPlayer.Effect.of(card("Noble Banneret")), EnumSet.of(FACE_UP, NOTE_CREATURE_NAME));
        assertEquals(LimitedPlayer.Effect.of(card("Paliano Vanguard")), EnumSet.of(FACE_UP, NOTE_CREATURE_TYPES));
        assertEquals(LimitedPlayer.Effect.of(card("Smuggler Captain")), EnumSet.of(FACE_UP, NOTE_CARD_NAME));
        assertEquals(LimitedPlayer.Effect.of(card("Animus of Predation")), EnumSet.of(FACE_UP, REMOVE_FACE_UP));
        assertEquals(LimitedPlayer.Effect.of(card("Cogwork Grinder")), EnumSet.of(FACE_UP, REMOVE_FACE_DOWN));
        assertEquals(LimitedPlayer.Effect.of(card("Cogwork Librarian")), EnumSet.of(FACE_UP, EXTRA_PICK_RETURN));
        assertEquals(LimitedPlayer.Effect.of(card("Leovold's Operative")), EnumSet.of(FACE_UP, EXTRA_PICK_SKIP));
        assertEquals(LimitedPlayer.Effect.of(card("Agent of Acquisitions")), EnumSet.of(FACE_UP, DRAFT_WHOLE_PACK));
        assertEquals(LimitedPlayer.Effect.of(card("Whispergear Sneak")), EnumSet.of(FACE_UP, PEEK_PACK));
        assertEquals(LimitedPlayer.Effect.of(card("Illusionary Informant")), EnumSet.of(FACE_UP, PEEK_NEXT_PICK));
        assertEquals(LimitedPlayer.Effect.of(card("Canal Dredger")), EnumSet.of(FACE_UP, LAST_CARD));
        assertEquals(LimitedPlayer.Effect.of(card("Archdemon of Paliano")), EnumSet.of(FACE_UP, RANDOM_PICKS));
        assertEquals(LimitedPlayer.Effect.of(card("Deal Broker")), EnumSet.of(FACE_UP, POST_DRAFT_TRADE));
        assertEquals(LimitedPlayer.Effect.of(card("Aether Searcher")), EnumSet.of(REVEAL, NOTE_NEXT));
        assertEquals(LimitedPlayer.Effect.of(card("Cogwork Spy")), EnumSet.of(REVEAL, SPY));
        assertEquals(LimitedPlayer.Effect.of(card("Cogwork Tracker")), EnumSet.of(REVEAL, NOTE_PASSER));
        assertEquals(LimitedPlayer.Effect.of(card("Spire Phantasm")), EnumSet.of(REVEAL, GUESS_NEXT));
        assertEquals(LimitedPlayer.Effect.of(card("Lore Seeker")), EnumSet.of(REVEAL, ADD_BOOSTER));
        assertEquals(LimitedPlayer.Effect.of(card("Regicide")), EnumSet.of(REVEAL, CHOOSE_COLORS));
        assertEquals(LimitedPlayer.Effect.of(card("Paliano, the High City")), EnumSet.of(REVEAL, CHOOSE_COLORS));
        for (String n : new String[] {"Lurking Automaton", "Garbage Fire", "Pyretic Hunter", "Custodi Peacekeeper"}) {
            assertEquals(LimitedPlayer.Effect.of(card(n)), EnumSet.of(REVEAL, NOTE_COUNT), n);
        }
        assertTrue(LimitedPlayer.Effect.of(card("Grizzly Bears")).isEmpty());
    }

    // The host's only guard against a client answering with an out-of-range, duplicate or missing choice
    @Test
    public void promptAnswersAreValidated() {
        DraftPrompt p = DraftPrompt.text(0, "pick", List.of("a", "b"), false);
        assertTrue(p.isValidAnswer(List.of(1)));
        assertFalse(p.isValidAnswer(List.of()));
        assertFalse(p.isValidAnswer(List.of(2)));
        assertEquals(p.defaultAnswer().size(), 1);
        DraftPrompt optional = DraftPrompt.text(0, "maybe", List.of("a"), true);
        assertTrue(optional.isValidAnswer(List.of()));
        assertEquals(optional.defaultAnswer(), List.of());
    }

    // The card fixes the order of choosers, and the in-game card reads the note as comma-separated colours
    @Test
    public void regicideAsksRightThenDrafterThenLeft() {
        LimitedPlayer drafter = seats.get(1);
        give(drafter, "Regicide", "Grizzly Bears");
        answers.addAll(List.of(List.of(0), List.of(0), List.of(0)));
        pick(drafter, "Regicide");
        assertEquals(asked.stream().map(DraftPrompt::seatIndex).collect(Collectors.toList()), List.of(0, 1, 2));
        assertEquals(drafter.getDraftNotes().get("Regicide"), List.of("white,blue,black"));
    }

    // A network answer arrives later, and the seat must get no new pack until it does
    @Test
    public void deferredGuessBlocksTheSeatAndHoldsThePack() {
        LimitedPlayer drafter = seats.get(0);
        DraftPack pack = give(drafter, "Spire Phantasm", "Grizzly Bears", "Shock");
        deferAnswers = true;
        pick(drafter, "Spire Phantasm");
        assertTrue(drafter.isBlocked());
        assertTrue(drafter.isPromptHeld(pack));
        deferred.get(0).accept(List.of(1));
        assertFalse(drafter.isBlocked());
        assertEquals(pack.getAwaitingGuess().getValue().getName(), "Shock");
    }

    // The card makes the added pack the next pick, whether or not the Lore Seeker pack has moved on when the answer arrives
    @Test
    public void loreSeekerPackIsTheNextPick() {
        LimitedPlayer drafter = seats.get(0);
        DraftPack source = give(drafter, "Lore Seeker", "Grizzly Bears");
        DraftPack queued = give(drafter, "Shock", "Forest");
        answers.add(List.of(0));
        pick(drafter, "Lore Seeker");
        assertSame(drafter.packQueue.get(0), source);
        assertSame(drafter.packQueue.get(2), queued);
        assertEquals(drafter.packQueue.size(), 3);

        LimitedPlayer later = seats.get(1);
        give(later, "Lore Seeker", "Grizzly Bears");
        DraftPack laterQueued = give(later, "Shock", "Forest");
        deferAnswers = true;
        pick(later, "Lore Seeker");
        later.passPack();
        deferred.get(0).accept(List.of(0));
        assertSame(later.packQueue.get(1), laterQueued);
        assertEquals(later.packQueue.size(), 2);
    }

    // Offers are revealed together, so every other seat must answer before the owner can choose
    @Test
    public void dealBrokerCollectsEveryOfferBeforeAccepting() {
        LimitedPlayer broker = seats.get(0);
        broker.getDeck().getOrCreate(DeckSection.Sideboard).add(card("Shock"));
        seats.get(1).getDeck().getOrCreate(DeckSection.Sideboard).add(card("Forest"));
        answers.addAll(List.of(List.of(0), List.of(0), List.of(), List.of(), List.of(0)));
        boolean[] done = {false};
        broker.activateBrokers(seats, 1, () -> done[0] = true);
        assertTrue(done[0]);
        assertEquals(asked.stream().map(DraftPrompt::seatIndex).collect(Collectors.toList()), List.of(0, 1, 2, 3, 0));
        assertTrue(broker.getDeck().get(DeckSection.Sideboard).contains(card("Forest")));
        assertTrue(seats.get(1).getDeck().get(DeckSection.Sideboard).contains(card("Shock")));
    }

    // Turning the source face down is the only thing that ends its ability
    @Test
    public void bannerNoteRevealsAndTurnsTheSourceFaceDown() {
        LimitedPlayer p = seats.get(0);
        putFaceUp(p, "Noble Banneret");
        give(p, "Grizzly Bears", "Shock");
        assertEquals(pickWith(p, "Grizzly Bears", "Noble Banneret"), Boolean.TRUE);
        assertEquals(p.getDraftNotes().get("Noble Banneret"), List.of("Grizzly Bears"));
        assertFalse(p.getFaceUp().contains(card("Noble Banneret")));
    }

    // The rulings keep a removed card's own reveal and note instructions, but not its face-up one
    @Test
    public void grinderRemovalKeepsRevealEffectsButNotThePoolOrFaceUp() {
        LimitedPlayer p = seats.get(0);
        putFaceUp(p, "Cogwork Grinder");
        give(p, "Garbage Fire", "Forest");
        pickWith(p, "Garbage Fire", "Cogwork Grinder");
        assertFalse(poolNames(p).contains("Garbage Fire"));
        assertEquals(p.getDraftNotes().get("Cogwork Grinder"), List.of("Garbage Fire"));
        assertNotNull(p.getDraftNotes().get("Garbage Fire"), "NOTE_COUNT still applies to a removed card");
        p.passPack();
        give(p, "Canal Dredger", "Forest");
        pickWith(p, "Canal Dredger", "Cogwork Grinder");
        assertFalse(p.getFaceUp().contains(card("Canal Dredger")), "a removed card is never face up");
    }

    // The rulings let several draft abilities apply to one card, such as a note and a removal
    @Test
    public void severalAbilitiesApplyToOnePick() {
        LimitedPlayer p = seats.get(0);
        putFaceUp(p, "Noble Banneret");
        putFaceUp(p, "Cogwork Grinder");
        give(p, "Grizzly Bears", "Shock");
        DraftAction choose = DraftAction.choose(card("Grizzly Bears"));
        assertTrue(p.getActions(p.nextChoice()).contains(choose));
        answers.add(List.of(0, 1));
        p.draftCard(card("Grizzly Bears"), DeckSection.Sideboard, choose);
        assertEquals(p.getDraftNotes().get("Noble Banneret"), List.of("Grizzly Bears"));
        assertEquals(p.getDraftNotes().get("Cogwork Grinder"), List.of("Grizzly Bears"));
        assertFalse(poolNames(p).contains("Grizzly Bears"));
    }

    // The returned Librarian must leave the pool and the face-up list, and reach clients as a pool removal
    @Test
    public void librarianTakesAnExtraPickThenReturnsToThePack() {
        LimitedPlayer p = seats.get(0);
        putFaceUp(p, "Cogwork Librarian");
        DraftPack pack = give(p, "Grizzly Bears", "Shock", "Forest");
        assertEquals(pickWith(p, "Grizzly Bears", "Cogwork Librarian"), Boolean.FALSE);
        p.drainPoolDelta();
        assertEquals(pick(p, "Shock"), Boolean.TRUE);
        assertTrue(pack.contains(card("Cogwork Librarian")));
        assertFalse(poolNames(p).contains("Cogwork Librarian"));
        assertEquals(p.drainPoolDelta().removed(), List.of(card("Cogwork Librarian")));
    }

    // The rulings allow several Librarians on one pack; each goes in after the last additional card, whatever its printing
    @Test
    public void librariansChainAndReturnAfterTheLastExtraPick() {
        LimitedPlayer p = seats.get(0);
        putFaceUp(p, "Cogwork Librarian");
        PaperCard foil = card("Cogwork Librarian").getFoiled();
        p.receiveOpenedPack(new DraftPack(new ArrayList<>(List.of(foil, card("Forest"))), nextPackId++));
        p.draftCard(foil, DeckSection.Sideboard, null);
        p.passPack();
        DraftPack pack = give(p, "Grizzly Bears", "Shock", "Forest", "Island");
        assertEquals(pickWith(p, "Grizzly Bears", "Cogwork Librarian"), Boolean.FALSE);
        assertEquals(pickWith(p, "Shock", "Cogwork Librarian"), Boolean.FALSE);
        assertFalse(pack.contains(card("Cogwork Librarian")));
        assertEquals(pick(p, "Forest"), Boolean.TRUE);
        assertEquals(pack.stream().filter(c -> c.getName().equals("Cogwork Librarian")).count(), 2L);
        assertTrue(p.getFaceUp().isEmpty());
    }

    // Leovold's Operative grants one extra pick, and its seat then passes one pack unpicked; several callers ask about the skip
    @Test
    public void operativeExtraPickThenSkipsOnePack() {
        LimitedPlayer p = seats.get(0);
        putFaceUp(p, "Leovold's Operative");
        give(p, "Grizzly Bears", "Shock", "Forest");
        pickWith(p, "Grizzly Bears", "Leovold's Operative");
        pick(p, "Shock");
        assertTrue(p.shouldSkipThisPick());
        assertTrue(p.shouldSkipThisPick(), "asking must not use up the skip");
        p.consumeSkip(p.passPack());
        assertFalse(p.shouldSkipThisPick());
        assertFalse(p.getFaceUp().contains(card("Leovold's Operative")));
    }

    // The rulings let other face-up abilities apply to each card Agent of Acquisitions drafts
    @Test
    public void agentHoldsThePackUntilEmptyAndOffersVariantsOnEachPick() {
        LimitedPlayer p = seats.get(0);
        putFaceUp(p, "Agent of Acquisitions");
        putFaceUp(p, "Cogwork Grinder");
        give(p, "Grizzly Bears", "Shock", "Forest");
        assertEquals(pickWith(p, "Shock", "Agent of Acquisitions"), Boolean.FALSE);
        assertEquals(pickWith(p, "Forest", "Cogwork Grinder"), Boolean.FALSE);
        assertEquals(pick(p, "Grizzly Bears"), Boolean.TRUE);
        assertTrue(p.shouldSkipThisPick());
    }

    // Spire Phantasm asks for a card name, so any printing of the named card is a correct guess
    @Test
    public void phantasmGuessComparesNamesNotPrintings() {
        LimitedPlayer guesser = seats.get(0);
        PaperCard forestA = FModel.getMagicDb().getCommonCards().getCard("Forest", "M11");
        PaperCard forestB = FModel.getMagicDb().getCommonCards().getCard("Forest", "M12");
        DraftPack pack = new DraftPack(new ArrayList<>(List.of(card("Spire Phantasm"), forestA, forestB)), nextPackId++);
        guesser.receiveOpenedPack(pack);
        answers.add(List.of(0));
        pick(guesser, "Spire Phantasm");
        assertEquals(pack.getAwaitingGuess().getValue(), forestA);
        LimitedPlayer next = seats.get(1);
        next.receiveOpenedPack(guesser.passPack());
        next.draftCard(forestB, DeckSection.Sideboard, null);
        assertEquals(guesser.getDraftNotes().get("Spire Phantasm"), List.of("Forest"));
    }

    // The card reveals the next card the chosen player drafts, not the last one
    @Test
    public void informantShowsTheTargetsNextPick() {
        List<String> privateLines = new ArrayList<>();
        draft.setLogEntry(new IDraftLog() {
            @Override public void addLogEntry(String message) { privateLines.add(message); }
        });
        LimitedPlayer me = seats.get(0);
        putFaceUp(me, "Illusionary Informant");
        answers.add(List.of(0));
        me.activate(DraftAction.pool(card("Illusionary Informant")));
        assertFalse(me.getFaceUp().contains(card("Illusionary Informant")));
        LimitedPlayer target = seats.get(1);
        give(target, "Shock", "Forest");
        privateLines.clear();
        pick(target, "Shock");
        assertTrue(privateLines.stream().anyMatch(l -> l.contains("Shock")));
    }

    // Each copy counts its own three random picks, so the pack stays hidden until the last copy turns face down
    @Test
    public void twoArchdemonsAreTrackedSeparately() {
        LimitedPlayer p = seats.get(0);
        putFaceUp(p, "Archdemon of Paliano");
        give(p, "Archdemon of Paliano", "Archdemon of Paliano");
        p.draftCard(PaperCard.FAKE_CARD, DeckSection.Sideboard, null);
        p.passPack();
        assertEquals(p.getFaceUp().stream().filter(c -> c.getName().equals("Archdemon of Paliano")).count(), 2L);
        for (int i = 0; i < 2; i++) {
            give(p, "Forest", "Island");
            p.draftCard(PaperCard.FAKE_CARD, DeckSection.Sideboard, null);
            p.passPack();
        }
        assertEquals(p.getFaceUp().stream().filter(c -> c.getName().equals("Archdemon of Paliano")).count(), 1L);
        assertTrue(p.isPackHidden());
        give(p, "Forest", "Island");
        p.draftCard(PaperCard.FAKE_CARD, DeckSection.Sideboard, null);
        assertFalse(p.isPackHidden());
    }

    // Archdemon lets the player look at a random card as they draft it, so abilities still apply to that card
    @Test
    public void abilitiesApplyToAnArchdemonRandomPick() {
        LimitedPlayer p = seats.get(0);
        putFaceUp(p, "Cogwork Grinder");
        putFaceUp(p, "Archdemon of Paliano");
        give(p, "Shock", "Shock");
        answers.add(List.of(0));
        p.draftCard(PaperCard.FAKE_CARD, DeckSection.Sideboard, null);
        assertFalse(poolNames(p).contains("Shock"));
        assertEquals(p.getDraftNotes().get("Cogwork Grinder"), List.of("Shock"));
    }

    // One Canal Dredger holder takes the last card without a prompt; with several, the passer chooses
    @Test
    public void lastCardGoesToTheOnlyDredgerOrTheChosenOne() {
        LimitedPlayer passer = seats.get(0);
        putFaceUp(seats.get(2), "Canal Dredger");
        DraftPack last = packOf("Forest");
        draft.routeLastCard(passer, last);
        assertSame(last.getDestination(), seats.get(2));
        putFaceUp(seats.get(3), "Canal Dredger");
        DraftPack another = packOf("Island");
        answers.add(List.of(1));
        draft.routeLastCard(passer, another);
        assertSame(another.getDestination(), seats.get(3));
        assertEquals(asked.get(asked.size() - 1).seatIndex(), 0);
    }

    // The only end-to-end run: it catches a draft that never ends and a pool that drifts from the picks
    @Test
    public void aFullLocalDraftWithAiSeatsCompletes() {
        BoosterDraft full = BoosterDraft.createDraft(LimitedPoolType.Full);
        assertNotNull(full);
        LimitedPlayer me = full.getHumanPlayer();
        me.setPromptSink((prompt, blocking, onAnswer) -> onAnswer.accept(prompt.defaultAnswer()));
        int picks = 0;
        while (full.hasNextChoice()) {
            CardPool pack = full.nextChoice();
            if (pack == null || pack.isEmpty()) {
                break;
            }
            if (me.shouldSkipThisPick()) {
                full.skipChoice();
                continue;
            }
            full.setChoice(pack.toFlatList().get(0));
            picks++;
        }
        assertTrue(picks > 0);
        assertEquals(me.getPoolCards().size(), picks);
    }

    // A skipped pack must pass on without a pick, and the skip is used up only once it has
    @Test
    public void skipChoiceUsesTheSkipOnlyAfterPassing() {
        LimitedPlayer me = seats.get(0);
        putFaceUp(me, "Leovold's Operative");
        give(me, "Grizzly Bears", "Shock", "Island");
        pickWith(me, "Grizzly Bears", "Leovold's Operative");
        pick(me, "Shock");
        me.passPack();
        DraftPack skipped = give(me, "Forest", "Plains");
        draft.skipChoice();
        assertFalse(me.shouldSkipThisPick());
        assertSame(seats.get(3).nextChoice(), skipped);
    }
}
