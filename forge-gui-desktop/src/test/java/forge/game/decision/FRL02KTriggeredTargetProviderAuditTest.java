package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/** Provider-side boundary checks for Blood's existing ChangeZone target shape. */
public class FRL02KTriggeredTargetProviderAuditTest extends AITest {
    private final TargetDecisionProvider provider = new TargetDecisionProvider();

    @Test
    public void bloodTargetShapePreservesForgeLegalityForZeroOneAndManyCandidates() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card source = addCardToZone("Blood Operative", chooser, ZoneType.Battlefield);

        final SpellAbility noTargetAbility = bloodTargetAbility(source, chooser);
        final TargetDecisionProvider.Generation noTarget = provider.generateTargetRequest(noTargetAbility, chooser, null);
        assertEquals(noTarget.getStatus(), TargetDecisionProvider.Status.INVALID_TARGETING,
                "Blood's mandatory one-card target must fail closed with zero candidates");

        final Card first = addCardToZone("Runeclaw Bear", opponent, ZoneType.Graveyard);
        final SpellAbility oneTargetAbility = bloodTargetAbility(source, chooser);
        final TargetDecisionProvider.Generation oneTargetGeneration =
                provider.generateTargetRequest(oneTargetAbility, chooser, null);
        assertEquals(oneTargetGeneration.getStatus(), TargetDecisionProvider.Status.DECISION);
        final DecisionRequest oneTarget = oneTargetGeneration.getRequest();
        assertTrue(oneTarget.isForced(), "one legal Blood target must be a forced provider request");
        assertTargetContextHasNoContinuation(oneTarget);
        assertEquals(cardCandidates(oneTarget), List.of(first.getId()));
        assertEquals(oneTarget.getCandidates().get(0).getTargetKind(), TargetCandidateKind.TARGET_CARD);
        assertEquals(provider.apply(oneTarget, oneTarget.getCandidates().get(0)).getStatus(),
                TargetDecisionProvider.Status.COMPLETE);
        assertTrue(oneTargetAbility.getTargets().contains(first));

        final Card second = addCardToZone("Llanowar Elves", opponent, ZoneType.Graveyard);
        final SpellAbility manyTargetAbility = bloodTargetAbility(source, chooser);
        final DecisionRequest manyTarget = provider.generateTargetRequest(manyTargetAbility, chooser, null).getRequest();
        assertFalse(manyTarget.isForced(), "multiple legal Blood targets must remain an external policy decision");
        assertTargetContextHasNoContinuation(manyTarget);
        assertEquals(cardCandidates(manyTarget), List.of(first.getId(), second.getId()).stream().sorted().toList());
        assertEquals(cardCandidates(manyTarget),
                CardUtil.getValidCardsToTarget(manyTargetAbility).stream().map(Card::getId).sorted().toList(),
                "provider candidates must equal Forge's legal card domain for Blood");
    }

    @Test
    public void bloodTargetShapeKeepsHiddenNamesNonPublicAndGenerationNeutral() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card source = addCardToZone("Blood Operative", chooser, ZoneType.Battlefield);
        final Card faceDown = addCardToZone("Runeclaw Bear", opponent, ZoneType.Graveyard);
        faceDown.turnFaceDown(true);
        final SpellAbility ability = bloodTargetAbility(source, chooser);

        final DecisionRequest request = NeutralityAssertions.assertGameAndRngNeutral(
                "Blood TARGET provider generation", game,
                () -> provider.generateTargetRequest(ability, chooser, null).getRequest());

        assertEquals(request.getDecisionType(), DecisionType.TARGET);
        assertTargetContextHasNoContinuation(request);
        final LegalCandidate candidate = request.getCandidates().get(0);
        assertEquals(candidate.getTargetKind(), TargetCandidateKind.TARGET_CARD);
        assertEquals(candidate.getTargetName(), "",
                "a face-down target may remain a legal candidate but must not expose its name");
        assertNull(request.getTargetContext().getDecisionSequenceId());
        assertNull(request.getTargetContext().getSubdecisionIndex());
    }

    @Test
    public void nativeBloodZeroTargetFailsPreparationBeforeStackInsertion() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card blood = addCardToZone("Blood Operative", ai, ZoneType.Battlefield);
        final Trigger trigger = blood.getTriggers().stream()
                .filter(candidate -> candidate.getMode() == TriggerType.ChangesZone)
                .filter(candidate -> "TrigChangeZone".equals(candidate.getParam("Execute")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Blood must expose its native ChangesZone trigger"));
        final SpellAbility ability = trigger.ensureAbility();
        ability.setActivatingPlayer(ai);
        final WrappedAbility wrapper = new WrappedAbility(trigger, ability, null);
        final int stackSizeBefore = game.getStack().size();

        assertFalse(ai.getController().playTrigger(blood, wrapper, true),
                "native AI target preparation must fail when Blood has no legal target");
        assertEquals(game.getStack().size(), stackSizeBefore,
                "a failed native mandatory target preparation must not push the trigger");
        assertTrue(ability.getTargets().isEmpty(),
                "zero legal targets must leave the underlying triggered ability untargeted");
    }

    private static SpellAbility bloodTargetAbility(final Card source, final Player chooser) {
        final SpellAbility ability = AbilityFactory.getAbility(
                "DB$ ChangeZone | Origin$ Graveyard | Destination$ Exile | ValidTgts$ Card | TgtZone$ Graveyard",
                source);
        ability.setActivatingPlayer(chooser);
        return ability;
    }

    private static void assertTargetContextHasNoContinuation(final DecisionRequest request) {
        assertFalse(request.getTargetContext().hasActionContinuation());
        assertNull(request.getTargetContext().getDecisionSequenceId());
        assertNull(request.getTargetContext().getSubdecisionIndex());
    }

    private static List<Integer> cardCandidates(final DecisionRequest request) {
        return request.getCandidates().stream()
                .filter(candidate -> candidate.getTargetKind() == TargetCandidateKind.TARGET_CARD)
                .map(LegalCandidate::getTargetEntityId)
                .sorted()
                .toList();
    }
}
