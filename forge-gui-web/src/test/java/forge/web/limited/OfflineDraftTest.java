package forge.web.limited;

import forge.deck.DeckGroup;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPoolType;
import forge.model.FModel;
import forge.web.WebTestSupport;
import forge.web.protocol.ToBrowser.DraftCard;
import forge.web.protocol.ToBrowser.DraftState;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class OfflineDraftTest {
    private final BlockingQueue<DraftState> shown = new LinkedBlockingQueue<>();
    private volatile String failed;

    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    /** A draft of three full packs against one computer seat, so a pack comes back after every pick. It uses one set with no draft-matters cards, because those ask questions that no browser answers here. */
    private OfflineDraft twoSeats() {
        shown.clear();
        failed = null;
        return new OfflineDraft(() -> {
            final BoosterDraft draft = BoosterDraft.createBlockDraft(LimitedPoolType.Block, FModel.getBlocks().get("Core Set 2021"), null);
            draft.setPodSize(2);
            return draft;
        }, "Drafter", shown::add, problem -> failed = problem);
    }

    private DraftState next() throws InterruptedException {
        final DraftState state = shown.poll(30, TimeUnit.SECONDS);
        Assert.assertNotNull(state, "the draft never showed its next state: " + failed);
        return state;
    }

    private static List<String> sorted(final List<DraftCard> cards) {
        return cards.stream().map(DraftCard::name).sorted().toList();
    }

    // Fails if a pick is not added to the drafter's picks, or the drafter keeps the pack it picked from
    @Test(timeOut = 60_000)
    public void aPickJoinsThePicksAndThePackIsPassed() throws Exception {
        final OfflineDraft draft = twoSeats();
        try {
            final DraftState first = next();
            Assert.assertTrue(first.picks().isEmpty());
            final DraftCard chosen = first.cards().get(3);
            draft.pick(first.step(), 3, false);
            final DraftState after = next();
            Assert.assertEquals(after.picks().stream().map(DraftCard::name).toList(), List.of(chosen.name()));
            Assert.assertEquals(after.step(), first.step() + 1, "no new pack was shown");
            Assert.assertEquals(after.cards().size(), first.cards().size() - 1, "the drafter was not handed the other seat's pack");
            final List<String> left = new ArrayList<>(first.cards().stream().map(DraftCard::name).toList());
            left.remove(chosen.name());
            Assert.assertNotEquals(sorted(after.cards()), left.stream().sorted().toList(), "the drafter kept the pack it picked from");
        } finally {
            draft.close();
        }
    }

    // Fails if a pick from a pack already passed on, or from outside the pack, takes a card
    @Test(timeOut = 60_000)
    public void aPickOutsideThePackInHandTakesNothing() throws Exception {
        final OfflineDraft draft = twoSeats();
        try {
            final DraftState first = next();
            draft.pick(first.step() - 1, 1, false);
            draft.pick(first.step() + 1, 1, false);
            draft.pick(first.step(), -1, false);
            draft.pick(first.step(), first.cards().size(), false);
            // The draft handles picks in order, so this pick's state follows any that the bad picks made
            draft.pick(first.step(), 0, false);
            final DraftState after = next();
            Assert.assertEquals(after.picks().stream().map(DraftCard::name).toList(), List.of(first.cards().get(0).name()));
            Assert.assertTrue(shown.isEmpty(), "a bad pick changed the draft: " + shown);
        } finally {
            draft.close();
        }
    }

    // Fails if the draft never ends, ends with picks missing, or saves a deck without every pick
    @Test(timeOut = 60_000)
    public void theFinishedDraftSavesEveryPick() throws Exception {
        final OfflineDraft draft = twoSeats();
        try {
            DraftState state = next();
            while (!state.done()) {
                draft.pick(state.step(), 0, false);
                state = next();
            }
            Assert.assertEquals(state.picks().size(), 45, "three packs of fifteen");
            final DeckGroup saved = draft.save("Saved draft").get(30, TimeUnit.SECONDS);
            Assert.assertEquals(saved.getHumanDeck().getMain().countAll(), 45);
            Assert.assertEquals(saved.getAiDecks().size(), 1);
        } finally {
            draft.close();
        }
    }
}
