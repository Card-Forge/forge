package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class CardSelectionDecisionProviderTest extends AITest {
    private final CardSelectionDecisionProvider provider = new CardSelectionDecisionProvider();

    @Test
    public void exactTwoCompletesSequentiallyWithoutMutatingTheLiveHand() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility source = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        source.setActivatingPlayer(chooser);
        final Card first = addCardToZone("Island", chooser, ZoneType.Hand);
        final Card second = addCardToZone("Mountain", chooser, ZoneType.Hand);
        final Card third = addCardToZone("Forest", chooser, ZoneType.Hand);
        final CardCollection valid = new CardCollection(List.of(first, second, third));

        final CardSelectionDecisionProvider.SessionStart start = provider.beginSession(chooser, chooser, source,
                valid, 2, 2, valid);
        final CardSelectionDecisionProvider.Generation stepZero = provider.generateNext(start.getSession(), null);

        assertEquals(start.getStatus(), CardSelectionDecisionProvider.Status.READY);
        assertEquals(stepZero.getStatus(), CardSelectionDecisionProvider.Status.DECISION);
        assertEquals(stepZero.getRequest().getDecisionType(), DecisionType.CARD_SELECTION);
        assertEquals(stepZero.getRequest().getCardSelectionContext().getSelectionAdapter(),
                CardSelectionAdapter.DISCARD);
        assertEquals(stepZero.getRequest().getCandidates().size(), 3);
        assertEquals(stepZero.getRequest().getCardSelectionContext().getSelectionStepIndex(), 0);
        assertNull(stepZero.getRequest().getCardSelectionContext().getDecisionSequenceId());
        assertNull(stepZero.getRequest().getCardSelectionContext().getActionSubdecisionIndex());

        final int handSizeBeforeSelection = chooser.getCardsIn(ZoneType.Hand).size();
        final LegalCandidate firstCandidate = candidateFor(stepZero.getRequest(), first);
        final CardSelectionDecisionProvider.Generation stepOne = provider.apply(stepZero.getRequest(), firstCandidate);

        assertEquals(chooser.getCardsIn(ZoneType.Hand).size(), handSizeBeforeSelection);
        assertEquals(stepOne.getStatus(), CardSelectionDecisionProvider.Status.DECISION);
        assertEquals(stepOne.getRequest().getCandidates().size(), 2);
        assertEquals(stepOne.getRequest().getCardSelectionContext().getSelectionStepIndex(), 1);

        final LegalCandidate secondCandidate = candidateFor(stepOne.getRequest(), second);
        final CardSelectionDecisionProvider.Generation complete = provider.apply(stepOne.getRequest(), secondCandidate);

        assertEquals(complete.getStatus(), CardSelectionDecisionProvider.Status.COMPLETE);
        assertEquals(chooser.getCardsIn(ZoneType.Hand).size(), handSizeBeforeSelection);
        assertEquals(complete.getSelectedCards().size(), 2);
        assertSame(complete.getSelectedCards().get(0), first);
        assertSame(complete.getSelectedCards().get(1), second);
        assertTrue(start.getSession().isCompleted());
        assertEquals(provider.generateNext(start.getSession(), null).getStatus(),
                CardSelectionDecisionProvider.Status.COMPLETE);
    }

    @Test
    public void duplicateNamesHaveDistinctDeterministicallyOrderedCandidates() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility source = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        source.setActivatingPlayer(chooser);
        final Card later = addCardToZone("Island", chooser, ZoneType.Hand);
        final Card earlier = addCardToZone("Island", chooser, ZoneType.Hand);
        final CardCollection reversed = new CardCollection(List.of(later, earlier));

        final DecisionRequest request = request(chooser, chooser, source, reversed, 1, 1, reversed, null);
        final List<CardSelectionCard> cards = request.getCandidates().stream()
                .map(LegalCandidate::getCardSelectionCard).toList();

        assertEquals(cards.size(), 2);
        assertFalse(cards.get(0).getCardId() == cards.get(1).getCardId());
        assertEquals(request.getCandidates().stream().map(LegalCandidate::getSemanticKey).toList(),
                request.getCandidates().stream().map(LegalCandidate::getSemanticKey).sorted().toList());
    }

    @Test
    public void doneIsLocalOptionalityAndForcedWhenNoCardsRemain() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility source = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        source.setActivatingPlayer(chooser);
        final Card selectable = addCardToZone("Island", chooser, ZoneType.Hand);
        final CardCollection one = new CardCollection(selectable);

        final DecisionRequest optional = request(chooser, chooser, source, one, 0, 1, one, null);
        assertEquals(optional.getCandidates().stream().map(LegalCandidate::getCardSelectionKind).toList(),
                List.of(CardSelectionCandidateKind.SELECT_CARD, CardSelectionCandidateKind.DONE));
        assertFalse(optional.isForced());

        final CardCollection empty = new CardCollection();
        final DecisionRequest forcedDone = request(chooser, chooser, source, empty, 0, 1, empty, null);
        assertEquals(forcedDone.getCandidates().size(), 1);
        assertEquals(forcedDone.getCandidates().get(0).getCardSelectionKind(), CardSelectionCandidateKind.DONE);
        assertTrue(forcedDone.isForced());

        final CardSelectionDecisionProvider.SessionStart zeroStart = provider.beginSession(chooser, chooser, source,
                empty, 0, 0, empty);
        final CardSelectionDecisionProvider.Generation zero = provider.generateNext(zeroStart.getSession(), null);
        assertEquals(zero.getStatus(), CardSelectionDecisionProvider.Status.COMPLETE);
        assertTrue(zero.getSelectedCards().isEmpty());
        assertTrue(zeroStart.getSession().isCompleted());
        assertEquals(provider.generateNext(zeroStart.getSession(), null).getStatus(),
                CardSelectionDecisionProvider.Status.COMPLETE);
    }

    @Test
    public void doneTerminatesTheSessionAndCannotBeAppliedTwice() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility source = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        source.setActivatingPlayer(chooser);
        final Card first = addCardToZone("Island", chooser, ZoneType.Hand);
        final Card second = addCardToZone("Mountain", chooser, ZoneType.Hand);
        final CardCollection valid = new CardCollection(List.of(first, second));
        final ActionContinuation continuation = new ActionContinuation(93L,
                PriorityActionKind.CAST_SPELL, "selection");
        final CardSelectionDecisionProvider.SessionStart start = provider.beginSession(chooser, chooser, source,
                valid, 0, 2, valid);
        final CardSelectionDecisionProvider.Generation initial = provider.generateNext(start.getSession(), continuation);
        final LegalCandidate done = initial.getRequest().getCandidates().stream()
                .filter(candidate -> candidate.getCardSelectionKind() == CardSelectionCandidateKind.DONE)
                .findFirst().orElseThrow();

        final CardSelectionDecisionProvider.Generation complete = provider.apply(initial.getRequest(), done);
        final CardSelectionDecisionProvider.Generation afterDone = provider.generateNext(start.getSession(), continuation);
        final CardSelectionDecisionProvider.Generation reapplied = provider.apply(initial.getRequest(), done);

        assertEquals(complete.getStatus(), CardSelectionDecisionProvider.Status.COMPLETE);
        assertTrue(start.getSession().isCompleted());
        assertEquals(afterDone.getStatus(), CardSelectionDecisionProvider.Status.COMPLETE);
        assertNull(afterDone.getRequest());
        assertEquals(continuation.nextSubdecisionIndex(), 2);
        assertEquals(reapplied.getStatus(), CardSelectionDecisionProvider.Status.STALE_SELECTION);
        assertEquals(reapplied.getReason(), CardSelectionDecisionProvider.Reason.REQUEST_OWNERSHIP);
    }

    @Test
    public void secondGenerationIsRejectedWhileTheFirstRequestIsOutstanding() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility source = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        source.setActivatingPlayer(chooser);
        final Card first = addCardToZone("Island", chooser, ZoneType.Hand);
        final Card second = addCardToZone("Mountain", chooser, ZoneType.Hand);
        final CardCollection valid = new CardCollection(List.of(first, second));
        final ActionContinuation continuation = new ActionContinuation(92L,
                PriorityActionKind.CAST_SPELL, "selection");
        final CardSelectionDecisionProvider.SessionStart start = provider.beginSession(chooser, chooser, source,
                valid, 2, 2, valid);
        final CardSelectionDecisionProvider.Generation initial = provider.generateNext(start.getSession(), continuation);
        final CardSelectionDecisionProvider.Generation duplicate = provider.generateNext(start.getSession(), continuation);

        assertEquals(initial.getRequest().getCardSelectionContext().getSelectionStepIndex(), 0);
        assertEquals(initial.getRequest().getCardSelectionContext().getActionSubdecisionIndex(), Integer.valueOf(1));
        assertEquals(duplicate.getStatus(), CardSelectionDecisionProvider.Status.STALE_SELECTION);
        assertEquals(duplicate.getReason(), CardSelectionDecisionProvider.Reason.REQUEST_OUTSTANDING);
        assertNull(duplicate.getRequest());
        final DecisionRequest afterApply = provider.apply(initial.getRequest(), candidateFor(initial.getRequest(), first))
                .getRequest();
        assertEquals(afterApply.getCardSelectionContext().getSelectionStepIndex(), 1);
        assertEquals(afterApply.getCardSelectionContext().getActionSubdecisionIndex(), Integer.valueOf(2));
    }

    @Test
    public void impossibleAndMalformedBoundsFailExplicitly() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility source = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        source.setActivatingPlayer(chooser);
        final CardCollection empty = new CardCollection();

        assertEquals(provider.beginSession(chooser, chooser, source, empty, 1, 1, empty).getReason(),
                CardSelectionDecisionProvider.Reason.IMPOSSIBLE_MINIMUM);
        assertEquals(provider.beginSession(chooser, chooser, source, empty, 2, 1, empty).getReason(),
                CardSelectionDecisionProvider.Reason.MIN_EXCEEDS_MAX);
        assertEquals(provider.beginSession(chooser, chooser, source, empty, -1, 1, empty).getReason(),
                CardSelectionDecisionProvider.Reason.NEGATIVE_BOUNDS);
    }

    @Test
    public void visibleSupersetIsContextOnlyAndHiddenSelectableCardIsRejected() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player affected = game.getPlayers().get(0);
        final SpellAbility source = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        source.setActivatingPlayer(chooser);
        final Card validCard = addCardToZone("Island", affected, ZoneType.Hand);
        final Card visibleInvalid = addCardToZone("Mountain", affected, ZoneType.Hand);
        final CardCollection valid = new CardCollection(validCard);
        final CardCollection visible = new CardCollection(List.of(validCard, visibleInvalid));

        final DecisionRequest request = request(chooser, affected, source, valid, 1, 1, visible, null);
        assertEquals(request.getCandidates().size(), 1);
        assertEquals(request.getCardSelectionContext().getVisibleCards().size(), 2);
        assertEquals(request.getCardSelectionContext().getChooserId(), chooser.getId());
        assertEquals(request.getCardSelectionContext().getAffectedPlayerId(), affected.getId());
        assertFalse(request.getCandidates().stream().anyMatch(candidate -> candidate.getCardSelectionCard()
                .getCardId() == visibleInvalid.getId()));

        final CardCollection hiddenView = new CardCollection(visibleInvalid);
        final CardSelectionDecisionProvider.SessionStart hidden = provider.beginSession(chooser, affected, source,
                valid, 1, 1, hiddenView);
        assertEquals(hidden.getStatus(), CardSelectionDecisionProvider.Status.UNSUPPORTED_HIDDEN_CARD_SELECTION);
        assertEquals(hidden.getReason(), CardSelectionDecisionProvider.Reason.HIDDEN_SELECTABLE_CARD);
        assertNull(hidden.getSession());
    }

    @Test
    public void leavingAndReturningWithSameIdButNewTimestampIsStale() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility source = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        source.setActivatingPlayer(chooser);
        final Card selected = addCardToZone("Island", chooser, ZoneType.Hand);
        final CardCollection valid = new CardCollection(selected);
        final CardSelectionDecisionProvider.SessionStart start = provider.beginSession(chooser, chooser, source,
                valid, 1, 1, valid);
        final DecisionRequest original = provider.generateNext(start.getSession(), null).getRequest();
        final int originalId = selected.getId();
        final long originalTimestamp = selected.getGameTimestamp();

        final Card graveyard = game.getAction().moveToGraveyard(selected, source);
        final Card returned = game.getAction().moveTo(ZoneType.Hand, graveyard, source, AbilityKey.newMap());

        assertEquals(returned.getId(), originalId);
        assertFalse(returned.getGameTimestamp() == originalTimestamp);
        final CardSelectionDecisionProvider.Generation stale = provider.apply(original,
                original.getCandidates().get(0));
        assertEquals(stale.getStatus(), CardSelectionDecisionProvider.Status.STALE_SELECTION);
        assertEquals(stale.getReason(), CardSelectionDecisionProvider.Reason.LIVE_STATE_CHANGED);
    }

    @Test
    public void sameNamedReplacementIsNeverSubstituted() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility source = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        source.setActivatingPlayer(chooser);
        final Card original = addCardToZone("Island", chooser, ZoneType.Hand);
        final CardCollection valid = new CardCollection(original);
        final CardSelectionDecisionProvider.SessionStart start = provider.beginSession(chooser, chooser, source,
                valid, 1, 1, valid);
        final DecisionRequest request = provider.generateNext(start.getSession(), null).getRequest();

        game.getAction().moveToGraveyard(original, source);
        final Card replacement = addCardToZone("Island", chooser, ZoneType.Hand);
        final CardSelectionDecisionProvider.Generation stale = provider.apply(request, request.getCandidates().get(0));

        assertFalse(replacement.getId() == original.getId());
        assertEquals(stale.getStatus(), CardSelectionDecisionProvider.Status.STALE_SELECTION);
        assertTrue(chooser.getCardsIn(ZoneType.Hand).contains(replacement));
    }

    @Test
    public void sessionCreationConsumesNoContinuationAndRequestsConsumeExactlyOneIndex() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility source = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        source.setActivatingPlayer(chooser);
        final Card first = addCardToZone("Island", chooser, ZoneType.Hand);
        final Card second = addCardToZone("Mountain", chooser, ZoneType.Hand);
        final CardCollection valid = new CardCollection(List.of(first, second));
        final ActionContinuation continuation = new ActionContinuation(91L,
                PriorityActionKind.CAST_SPELL, "selection");

        final CardSelectionDecisionProvider.SessionStart start = provider.beginSession(chooser, chooser, source,
                valid, 2, 2, valid);
        final DecisionRequest firstRequest = provider.generateNext(start.getSession(), continuation).getRequest();
        final DecisionRequest secondRequest = provider.apply(firstRequest, candidateFor(firstRequest, first))
                .getRequest();

        assertEquals(firstRequest.getCardSelectionContext().getActionSubdecisionIndex(), Integer.valueOf(1));
        assertEquals(secondRequest.getCardSelectionContext().getActionSubdecisionIndex(), Integer.valueOf(2));
        assertEquals(firstRequest.getCardSelectionContext().getDecisionSequenceId(), Long.valueOf(91L));
        assertEquals(firstRequest.getCardSelectionContext().getSelectionSessionId(),
                secondRequest.getCardSelectionContext().getSelectionSessionId());
        assertEquals(firstRequest.getCardSelectionContext().getSelectionStepIndex(), 0);
        assertEquals(secondRequest.getCardSelectionContext().getSelectionStepIndex(), 1);
    }

    @Test
    public void oneRemainingRequiredCardIsForcedAndCannotBeSelectedTwice() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility source = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        source.setActivatingPlayer(chooser);
        final Card first = addCardToZone("Island", chooser, ZoneType.Hand);
        final Card second = addCardToZone("Mountain", chooser, ZoneType.Hand);
        final CardCollection valid = new CardCollection(List.of(first, second));
        final CardSelectionDecisionProvider.SessionStart start = provider.beginSession(chooser, chooser, source,
                valid, 2, 2, valid);
        final DecisionRequest initial = provider.generateNext(start.getSession(), null).getRequest();
        final LegalCandidate chosen = candidateFor(initial, first);
        final DecisionRequest remaining = provider.apply(initial, chosen).getRequest();

        assertTrue(remaining.isForced());
        assertEquals(remaining.getCandidates().get(0).getCardSelectionCard().getCardId(), second.getId());
        final CardSelectionDecisionProvider.Generation repeated = provider.apply(initial, chosen);
        assertEquals(repeated.getStatus(), CardSelectionDecisionProvider.Status.STALE_SELECTION);
        assertEquals(repeated.getReason(), CardSelectionDecisionProvider.Reason.REQUEST_OWNERSHIP);
    }

    @Test
    public void focusedCardSelectionMetricsMeasureGenerationAndSequentialShrinkage() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility source = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        source.setActivatingPlayer(chooser);
        final CardCollection valid = new CardCollection();
        for (final String name : List.of("Island", "Mountain", "Forest", "Swamp", "Plains", "Opt", "Shock")) {
            valid.add(addCardToZone(name, chooser, ZoneType.Hand));
        }

        final CardSelectionDecisionProvider.SessionStart shrinkageStart = provider.beginSession(chooser, chooser,
                source, valid, 2, 2, valid);
        final CardSelectionDecisionProvider.Generation first = provider.generateNext(shrinkageStart.getSession(), null);
        final CardSelectionDecisionProvider.Generation second = provider.apply(first.getRequest(),
                first.getRequest().getCandidates().get(0));
        assertEquals(first.getRequest().getCandidates().size(), 7);
        assertEquals(second.getRequest().getCandidates().size(), 6);

        final List<Long> timings = new ArrayList<>();
        for (int index = 0; index < 200; index++) {
            final CardSelectionDecisionProvider.SessionStart start = provider.beginSession(chooser, chooser,
                    source, valid, 2, 2, valid);
            final CardSelectionDecisionProvider.Generation generation = provider.generateNext(start.getSession(), null);
            if (index >= 20) {
                timings.add(generation.getGenerationNanos());
            }
        }
        timings.sort(Long::compareTo);
        System.out.println("FRL02F_FOCUSED_METRICS candidate_counts=[7, 6] candidate_mean=6.5"
                + " candidate_p50=6 candidate_p95=7 candidate_max=7 atomic_steps=2"
                + " shrinkage=[0, 1] generation_p50_ns=" + percentile(timings, 0.50)
                + " generation_p95_ns=" + percentile(timings, 0.95)
                + " generation_p99_ns=" + percentile(timings, 0.99));
    }

    @Test
    public void mulliganBottomUsesAnExplicitSourceAbsentAdapter() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Card first = addCardToZone("Island", chooser, ZoneType.Hand);
        final Card second = addCardToZone("Mountain", chooser, ZoneType.Hand);
        final CardCollection valid = new CardCollection(List.of(first, second));

        final CardSelectionDecisionProvider.SessionStart start = provider.beginSession(chooser, chooser,
                CardSelectionAdapter.MULLIGAN_BOTTOM, valid, 2, 2, valid);
        final DecisionRequest request = provider.generateNext(start.getSession(), null).getRequest();
        final CardSelectionContext context = request.getCardSelectionContext();

        assertEquals(start.getStatus(), CardSelectionDecisionProvider.Status.READY);
        assertEquals(context.getSelectionAdapter(), CardSelectionAdapter.MULLIGAN_BOTTOM);
        assertNull(context.getSourceCardId());
        assertNull(context.getSourceCardTimestamp());
        assertNull(context.getDecisionSequenceId());
        assertNull(context.getActionSubdecisionIndex());
        assertEquals(request.getCandidates().size(), 2);
    }

    private DecisionRequest request(final Player chooser, final Player affected, final SpellAbility source,
            final CardCollection valid, final int min, final int max, final CardCollection visible,
            final ActionContinuation continuation) {
        final CardSelectionDecisionProvider.SessionStart start = provider.beginSession(chooser, affected, source,
                valid, min, max, visible);
        assertEquals(start.getStatus(), CardSelectionDecisionProvider.Status.READY);
        return provider.generateNext(start.getSession(), continuation).getRequest();
    }

    private static LegalCandidate candidateFor(final DecisionRequest request, final Card card) {
        return request.getCandidates().stream()
                .filter(candidate -> candidate.getCardSelectionCard().getCardId() == card.getId())
                .findFirst().orElseThrow();
    }

    private static SpellAbility spell(final Card card) {
        return card.getSpellAbilities().stream().filter(SpellAbility::isSpell).findFirst().orElseThrow();
    }

    private static long percentile(final List<Long> sortedValues, final double percentile) {
        return sortedValues.get((int) Math.ceil(percentile * sortedValues.size()) - 1);
    }
}
