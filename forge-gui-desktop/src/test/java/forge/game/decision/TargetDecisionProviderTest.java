package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class TargetDecisionProviderTest extends AITest {
    private final TargetDecisionProvider provider = new TargetDecisionProvider();

    @Test
    public void requiredCreatureTargetContainsEveryForgeLegalNonBlackCreature() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final SpellAbility ability = targetAbility("Dark Banishing", chooser);
        final Card bear = addCard("Runeclaw Bear", opponent);
        final Card elf = addCard("Llanowar Elves", chooser);
        addCard("Walking Corpse", opponent);

        final DecisionRequest request = decision(ability, chooser);

        assertEquals(request.getDecisionType(), DecisionType.TARGET);
        assertEquals(request.getTargetContext().getChoosingPlayerId(), chooser.getId());
        assertNull(request.getTargetContext().getDecisionSequenceId());
        assertNull(request.getTargetContext().getSubdecisionIndex());
        assertEquals(targetCardIds(request), List.of(elf.getId(), bear.getId()).stream().sorted().toList());
        assertFalse(hasTargetName(request, "Walking Corpse"));
        assertFalse(hasDone(request));
    }

    @Test
    public void requiredPlayerTargetUsesPlayerCandidates() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility ability = targetAbility("Drain Life", chooser);

        final DecisionRequest request = decision(ability, chooser);

        assertEquals(request.getCandidates().stream()
                .filter(candidate -> candidate.getTargetKind() == TargetCandidateKind.TARGET_PLAYER).count(), 2L);
        assertTrue(request.getCandidates().stream()
                .anyMatch(candidate -> candidate.getTargetEntityId() == chooser.getId()));
    }

    @Test
    public void stackSpellTargetUsesTheForgeStackInstanceIdentity() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card pendingCard = addCardToZone("Runeclaw Bear", opponent, ZoneType.Hand);
        final SpellAbility pendingSpell = pendingCard.getFirstSpellAbility();
        pendingSpell.setActivatingPlayer(opponent);
        game.getStackZone().add(pendingCard);
        game.getStack().add(pendingSpell);
        final SpellAbility ability = targetAbility("Counterspell", chooser);

        final DecisionRequest request = decision(ability, chooser);
        final LegalCandidate candidate = request.getCandidates().stream()
                .filter(value -> value.getTargetKind() == TargetCandidateKind.TARGET_STACK_OBJECT)
                .findFirst().orElseThrow();

        assertEquals(candidate.getTargetEntityId(), game.getStack().iterator().next().getId());
        assertEquals(candidate.getTargetZone(), ZoneType.Stack);
        assertEquals(provider.apply(request, candidate).getStatus(), TargetDecisionProvider.Status.COMPLETE);
        assertEquals(ability.getTargets().getFirstTargetedSpell(), pendingSpell);
    }

    @Test
    public void targetChooserCanDifferFromActivatingPlayerAndControlsLegality() {
        final Game game = initAndCreateGame();
        final Player activatingPlayer = game.getPlayers().get(1);
        final Player targetingPlayer = game.getPlayers().get(0);
        final SpellAbility ability = targetAbility("Evangelize", activatingPlayer);
        final Card activatorCreature = addCard("Runeclaw Bear", activatingPlayer);
        final Card chooserCreature = addCard("Llanowar Elves", targetingPlayer);
        ability.setTargetingPlayer(targetingPlayer);

        final DecisionRequest request = decision(ability, targetingPlayer);

        assertEquals(request.getTargetContext().getChoosingPlayerId(), targetingPlayer.getId());
        assertTrue(hasTargetEntity(request, chooserCreature.getId()));
        assertFalse(hasTargetEntity(request, activatorCreature.getId()));
    }

    @Test
    public void mustTargetFilteringUsesForgeStaticAbilitySemantics() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final SpellAbility ability = targetAbility("Dark Banishing", chooser);
        final Card flagbearer = addCard("Standard Bearer", opponent);
        final Card ordinaryCreature = addCard("Runeclaw Bear", opponent);

        final DecisionRequest request = decision(ability, chooser);

        assertEquals(targetCardIds(request), List.of(flagbearer.getId()));
        assertFalse(hasTargetEntity(request, ordinaryCreature.getId()));
    }

    @Test
    public void noLegalRequiredTargetIsExplicitlyNonContinuable() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final SpellAbility ability = targetAbility("Dark Banishing", chooser);
        addCard("Walking Corpse", opponent);

        final TargetDecisionProvider.Generation generation = provider.generateTargetRequest(ability, chooser, null);

        assertEquals(generation.getStatus(), TargetDecisionProvider.Status.INVALID_TARGETING);
        assertNull(generation.getRequest());
        assertTrue(generation.getRequestGenerationNanos() > 0);
    }

    @Test
    public void exactlyOneLegalTargetIsForced() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final SpellAbility ability = targetAbility("Dark Banishing", chooser);
        final Card bear = addCard("Runeclaw Bear", opponent);

        final DecisionRequest request = decision(ability, chooser);

        assertTrue(request.isForced());
        assertEquals(targetCardIds(request), List.of(bear.getId()));
    }

    @Test
    public void upToTargetsExposesDoneWithoutMakingTheRequestForced() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final SpellAbility ability = targetAbility("Quicksilver Geyser", chooser);
        final Card bear = addCard("Runeclaw Bear", opponent);

        final DecisionRequest request = decision(ability, chooser);

        assertTrue(hasTargetEntity(request, bear.getId()));
        assertTrue(hasDone(request));
        assertFalse(request.isForced());
    }

    @Test
    public void upToTargetWithNoVisibleTargetIsForcedDoneAndCompletesWithoutMutatingTargets() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility ability = targetAbility("Quicksilver Geyser", chooser);

        final DecisionRequest request = decision(ability, chooser);
        final TargetDecisionProvider.Generation completed = provider.apply(request,
                request.getCandidates().get(0));

        assertTrue(request.isForced());
        assertTrue(hasDone(request));
        assertEquals(completed.getStatus(), TargetDecisionProvider.Status.COMPLETE);
        assertEquals(ability.getTargets().size(), 0);
        assertNotNull(completed.getCostFeasibility());
    }

    @Test
    public void multiTargetChoicesAreSequentialAndRecomputeAfterEverySelection() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final SpellAbility ability = targetAbility("Incriminate", chooser);
        final Card firstTarget = addCard("Runeclaw Bear", opponent);
        final Card secondTarget = addCard("Llanowar Elves", opponent);
        final Card thirdTarget = addCard("Grizzly Bears", opponent);

        final DecisionRequest first = decision(ability, chooser);
        final TargetDecisionProvider.Generation afterFirst = provider.apply(first, candidateFor(first, firstTarget));
        final DecisionRequest second = afterFirst.getRequest();

        assertNotNull(second);
        assertFalse(hasTargetEntity(second, firstTarget.getId()));
        assertTrue(hasTargetEntity(second, secondTarget.getId()));
        assertTrue(hasTargetEntity(second, thirdTarget.getId()));
        assertFalse(hasDone(second));

        final TargetDecisionProvider.Generation completed = provider.apply(second, candidateFor(second, secondTarget));
        assertEquals(completed.getStatus(), TargetDecisionProvider.Status.COMPLETE);
        assertEquals(ability.getTargets().size(), 2);
        assertTrue(ability.getTargets().contains(firstTarget));
        assertTrue(ability.getTargets().contains(secondTarget));
    }

    @Test
    public void uniqueTargetSubgroupExcludesTargetChosenByTheParentGroup() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final SpellAbility firstGroup = targetAbility("Arc Trail", chooser);
        final Card firstTarget = addCard("Runeclaw Bear", opponent);
        final Card secondTarget = addCard("Llanowar Elves", opponent);

        final DecisionRequest firstRequest = decision(firstGroup, chooser);
        provider.apply(firstRequest, candidateFor(firstRequest, firstTarget));
        final SpellAbility secondGroup = firstGroup.getSubAbility();
        final DecisionRequest secondRequest = decision(secondGroup, chooser);

        assertEquals(secondRequest.getTargetContext().getTargetGroupIndex(), 1);
        assertFalse(hasTargetEntity(secondRequest, firstTarget.getId()));
        assertTrue(hasTargetEntity(secondRequest, secondTarget.getId()));
    }

    @Test
    public void repeatedGenerationUsesStableCandidateIdsAndSemanticOrder() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final SpellAbility ability = targetAbility("Dark Banishing", chooser);
        addCard("Runeclaw Bear", opponent);
        addCard("Llanowar Elves", opponent);

        final DecisionRequest first = decision(ability, chooser);
        final DecisionRequest second = decision(ability, chooser);

        assertEquals(candidateIds(second), candidateIds(first));
        assertEquals(semanticKeys(second), semanticKeys(first));
    }

    @Test
    public void unrelatedOpponentHandInformationDoesNotChangeVisibleTargetRequest() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final SpellAbility ability = targetAbility("Dark Banishing", chooser);
        addCard("Runeclaw Bear", opponent);

        final List<String> before = semanticKeys(decision(ability, chooser));
        addCardToZone("Counterspell", opponent, ZoneType.Hand);
        final List<String> after = semanticKeys(decision(ability, chooser));

        assertEquals(after, before);
    }

    @Test
    public void legallyTargetableFaceDownPermanentDoesNotExposeItsName() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final SpellAbility ability = targetAbility("Dark Banishing", chooser);
        final Card faceDownCreature = addCard("Runeclaw Bear", opponent);
        faceDownCreature.turnFaceDown(true);

        final LegalCandidate candidate = candidateFor(decision(ability, chooser), faceDownCreature);

        assertEquals(candidate.getTargetKind(), TargetCandidateKind.TARGET_CARD);
        assertEquals(candidate.getTargetName(), "");
    }

    @Test
    public void continuationIdentityIsRetainedAcrossSequentialTargetRequests() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final SpellAbility ability = targetAbility("Nimbleclaw Adept", chooser);
        final Card firstTarget = addCard("Runeclaw Bear", opponent);
        addCard("Llanowar Elves", opponent);
        final ActionContinuation continuation = new ActionContinuation(481L,
                PriorityActionKind.CAST_SPELL, "42:Nimbleclaw Adept");

        final DecisionRequest first = provider.generateTargetRequest(ability, chooser, continuation).getRequest();
        final DecisionRequest second = provider.apply(first, candidateFor(first, firstTarget)).getRequest();

        assertEquals(first.getTargetContext().getDecisionSequenceId(), 481L);
        assertEquals(first.getTargetContext().getSubdecisionIndex(), 1);
        assertEquals(second.getTargetContext().getDecisionSequenceId(), 481L);
        assertEquals(second.getTargetContext().getSubdecisionIndex(), 2);
    }

    @Test
    public void dividedTargetSemanticsFailLoudlyUntilTheirAllocationDecisionExists() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility ability = targetAbility("Volley of Boulders", chooser);

        assertThrows(UnsupportedTargetDecisionException.class,
                () -> provider.generateTargetRequest(ability, chooser, null));
    }

    @Test
    public void randomTargetSemanticsFailLoudlyRatherThanCreatingAPolicyDecision() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility ability = targetAbility("Dark Banishing", chooser);
        ability.getTargetRestrictions().setRandomTarget(true);

        assertThrows(UnsupportedTargetDecisionException.class,
                () -> provider.generateTargetRequest(ability, chooser, null));
    }

    private SpellAbility targetAbility(final String cardName, final Player player) {
        final Card source = addCardToZone(cardName, player, ZoneType.Hand);
        final SpellAbility ability = targetingAbility(source);
        ability.setActivatingPlayer(player);
        return ability;
    }

    private static SpellAbility targetingAbility(final Card source) {
        return source.getSpellAbilities().stream().filter(SpellAbility::usesTargeting).findFirst().orElseThrow();
    }

    private DecisionRequest decision(final SpellAbility ability, final Player chooser) {
        PriorityActionDiagnostics.recordTargetRequest(ability, chooser);
        final TargetDecisionProvider.Generation generation = provider.generateTargetRequest(ability, chooser, null);
        assertEquals(generation.getStatus(), TargetDecisionProvider.Status.DECISION);
        assertTrue(generation.getRequestGenerationNanos() > 0);
        return generation.getRequest();
    }

    private static LegalCandidate candidateFor(final DecisionRequest request, final Card target) {
        return request.getCandidates().stream()
                .filter(candidate -> candidate.getTargetEntityId() == target.getId())
                .findFirst()
                .orElseThrow();
    }

    private static boolean hasTargetEntity(final DecisionRequest request, final int entityId) {
        return request.getCandidates().stream().anyMatch(candidate -> candidate.getTargetEntityId() == entityId);
    }

    private static boolean hasTargetName(final DecisionRequest request, final String targetName) {
        return request.getCandidates().stream().anyMatch(candidate -> targetName.equals(candidate.getTargetName()));
    }

    private static boolean hasDone(final DecisionRequest request) {
        return request.getCandidates().stream().anyMatch(candidate -> candidate.getTargetKind() == TargetCandidateKind.DONE);
    }

    private static List<Integer> targetCardIds(final DecisionRequest request) {
        return request.getCandidates().stream()
                .filter(candidate -> candidate.getTargetKind() == TargetCandidateKind.TARGET_CARD)
                .map(LegalCandidate::getTargetEntityId)
                .toList();
    }

    private static List<Integer> candidateIds(final DecisionRequest request) {
        return request.getCandidates().stream().map(LegalCandidate::getCandidateId).toList();
    }

    private static List<String> semanticKeys(final DecisionRequest request) {
        return request.getCandidates().stream().map(LegalCandidate::getSemanticKey).toList();
    }
}
