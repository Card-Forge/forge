package forge.game.decision;

import forge.ai.AITest;
import forge.card.mana.ManaAtom;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.AbilitySub;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class PaymentDecisionProviderTest extends AITest {
    private final PaymentDecisionProvider provider = new PaymentDecisionProvider();

    @Test
    public void fixedOneManaSourceCreatesPaymentContractForActualPayer() {
        final Game game = initAndCreateGame();
        final Player payer = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", payer, ZoneType.Hand);
        addCard("Mountain", payer);
        final SpellAbility spell = bolt.getFirstSpellAbility();
        spell.setActivatingPlayer(payer);
        final ManaCostBeingPaid remaining = new ManaCostBeingPaid(
                spell.getPayCosts().getCostMana().getManaCostFor(spell));

        final PaymentDecisionProvider.Generation generation = provider.generatePaymentRequest(
                remaining, spell, payer, identityMatrix(), null);
        final DecisionRequest request = generation.getRequest();

        assertEquals(request.getDecisionType(), DecisionType.PAYMENT);
        assertEquals(request.getPaymentContext().getPaymentStage(), PaymentStage.SOURCE);
        assertEquals(request.getPaymentContext().getPayerId(), payer.getId());
        assertEquals(request.getPaymentContext().getRemainingCostSummary(), "{R}");
        assertEquals(request.getCandidates().get(0).getPaymentKind(),
                PaymentCandidateKind.ACTIVATE_MANA_SOURCE);
    }

    @Test
    public void exactFloatingManaResourceCanBeSelectedEvenWhenForgeEqualsCollapsesIt() {
        final Game game = initAndCreateGame();
        final Player payer = game.getPlayers().get(1);
        final Card spellCard = addCardToZone("Unsummon", payer, ZoneType.Hand);
        final Card firstIsland = addCard("Island", payer);
        final Card secondIsland = addCard("Island", payer);
        final SpellAbility spell = spellCard.getFirstSpellAbility();
        spell.setActivatingPlayer(payer);
        final Mana first = new Mana((byte) ManaAtom.BLUE, firstIsland, null, payer);
        final Mana second = new Mana((byte) ManaAtom.BLUE, secondIsland, null, payer);
        payer.getManaPool().addManaNoEvent(first);
        payer.getManaPool().addManaNoEvent(second);
        final ManaCostBeingPaid remaining = new ManaCostBeingPaid(
                spell.getPayCosts().getCostMana().getManaCostFor(spell));
        final List<Mana> spent = new ArrayList<>();

        assertTrue(first.equals(second), "fixture must cover Forge's semantic equality collapse");
        assertTrue(payer.getManaPool().containsManaInstance(first));
        assertTrue(payer.getManaPool().containsManaInstance(second));
        assertTrue(payer.getManaPool().tryPayCostWithManaInstance(spell, remaining, second, spent));
        assertTrue(payer.getManaPool().containsManaInstance(first));
        assertFalse(payer.getManaPool().containsManaInstance(second));
        assertEquals(spent, List.of(second));
        assertTrue(remaining.isPaid());
    }

    @Test
    public void exactlyOneAvailableSourceIsForced() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Mountain");

        final DecisionRequest request = decision(fixture);

        assertTrue(request.isForced());
        assertEquals(request.getCandidates().size(), 1);
    }

    @Test
    public void twoDistinctSourcesCreateStrategicDeterministicCandidates() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Mountain", "Mountain");

        final DecisionRequest first = decision(fixture);
        final DecisionRequest second = decision(fixture);

        assertFalse(first.isForced());
        assertEquals(first.getCandidates().size(), 2);
        assertNotEquals(first.getCandidates().get(0).getSourceCardId(),
                first.getCandidates().get(1).getSourceCardId());
        assertEquals(keys(first), keys(second));
        assertTrue(first.getCandidates().get(0).getSemanticKey()
                .compareTo(first.getCandidates().get(1).getSemanticKey()) < 0);
    }

    @Test
    public void tappedSourceIsNotOffered() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Mountain", "Mountain");
        fixture.sources().get(0).tap(true, null, fixture.payer());

        final DecisionRequest request = decision(fixture);

        assertTrue(request.isForced());
        assertEquals(request.getCandidates().get(0).getSourceCardId(), fixture.sources().get(1).getId());
    }

    @Test
    public void floatingManaIsOneExactCandidatePerResource() {
        final PaymentFixture fixture = fixture("Unsummon", "Island", "Island");
        final Mana first = new Mana((byte) ManaAtom.BLUE, fixture.sources().get(0), null, fixture.payer());
        final Mana second = new Mana((byte) ManaAtom.BLUE, fixture.sources().get(1), null, fixture.payer());
        fixture.payer().getManaPool().addManaNoEvent(first);
        fixture.payer().getManaPool().addManaNoEvent(second);
        fixture.sources().forEach(card -> card.tap(true, null, fixture.payer()));

        final DecisionRequest request = decision(fixture);

        assertEquals(request.getCandidates().size(), 2);
        assertTrue(request.getCandidates().stream()
                .allMatch(candidate -> candidate.getPaymentKind() == PaymentCandidateKind.USE_FLOATING_MANA));
        assertNotEquals(request.getCandidates().get(0).getSemanticKey(),
                request.getCandidates().get(1).getSemanticKey());
    }

    @Test
    public void fixedMultiOutputBundleIsOneSourceCandidate() {
        final PaymentFixture fixture = fixture("Counterspell", "Dimir Aqueduct");

        final DecisionRequest request = decision(fixture);

        assertEquals(request.getCandidates().size(), 1);
        assertEquals(request.getCandidates().get(0).getPaymentKind(),
                PaymentCandidateKind.ACTIVATE_MANA_SOURCE);
    }

    @Test
    public void variableOutputSourceFailsExplicitlyInsteadOfCallingControllerChoice() {
        final PaymentFixture fixture = fixture("Unsummon", "Birds of Paradise");
        fixture.sources().get(0).setSickness(false);

        final PaymentDecisionProvider.Generation generation = generate(fixture);

        assertEquals(generation.getStatus(), PaymentDecisionProvider.Status.UNSUPPORTED);
        assertEquals(generation.getUnsupportedReason(),
                PaymentDecisionProvider.UnsupportedReason.VARIABLE_MANA_OUTPUT);
        assertNull(generation.getRequest());
    }

    @Test
    public void impossibleSupportedPaymentIsInvalid() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Island");

        assertEquals(generate(fixture).getStatus(), PaymentDecisionProvider.Status.INVALID_PAYMENT);
    }

    @Test
    public void paidStateIsCompleteAndDoesNotCreateRequest() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Mountain");
        fixture.remaining().payMana(new Mana((byte) ManaAtom.RED, fixture.sources().get(0), null,
                fixture.payer()), fixture.payer().getManaPool());

        final PaymentDecisionProvider.Generation generation = generate(fixture);

        assertEquals(generation.getStatus(), PaymentDecisionProvider.Status.COMPLETE);
        assertNull(generation.getRequest());
    }

    @Test
    public void continuationAndActualPayerRemainSeparateMetadata() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Mountain");
        final ActionContinuation continuation = new ActionContinuation(481L,
                PriorityActionKind.CAST_SPELL, "Lightning Bolt");

        final DecisionRequest request = provider.generatePaymentRequest(fixture.remaining(), fixture.ability(),
                fixture.payer(), identityMatrix(), continuation).getRequest();

        assertEquals(request.getPaymentContext().getPayerId(), fixture.payer().getId());
        assertEquals(request.getPaymentContext().getDecisionSequenceId(), Long.valueOf(481L));
        assertEquals(request.getPaymentContext().getSubdecisionIndex(), Integer.valueOf(1));
    }

    @Test
    public void irrelevantOpponentHiddenCardsDoNotChangeRequest() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Mountain", "Mountain");
        final List<String> before = keys(decision(fixture));
        addCardToZone("Counterspell", fixture.payer().getGame().getPlayers().get(0), ZoneType.Hand);

        assertEquals(keys(decision(fixture)), before);
    }

    @Test
    public void applyingExactFloatingManaCompletesThroughForgePool() {
        final PaymentFixture fixture = fixture("Unsummon", "Island", "Island");
        final Mana first = new Mana((byte) ManaAtom.BLUE, fixture.sources().get(0), null, fixture.payer());
        final Mana selected = new Mana((byte) ManaAtom.BLUE, fixture.sources().get(1), null, fixture.payer());
        fixture.payer().getManaPool().addManaNoEvent(first);
        fixture.payer().getManaPool().addManaNoEvent(selected);
        fixture.sources().forEach(card -> card.tap(true, null, fixture.payer()));
        final DecisionRequest request = decision(fixture);
        final LegalCandidate candidate = request.getCandidates().stream()
                .filter(value -> value.getSourceCardId() == fixture.sources().get(1).getId())
                .findFirst().orElseThrow();

        final PaymentDecisionProvider.Generation result = provider.apply(request, candidate);

        assertEquals(result.getStatus(), PaymentDecisionProvider.Status.COMPLETE);
        assertTrue(fixture.payer().getManaPool().containsManaInstance(first));
        assertFalse(fixture.payer().getManaPool().containsManaInstance(selected));
    }

    @Test
    public void applyingSourceUsesForgeActivationAndCompletes() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Mountain");
        final DecisionRequest request = decision(fixture);

        final PaymentDecisionProvider.Generation result = provider.apply(request,
                request.getCandidates().get(0));

        assertEquals(result.getStatus(), PaymentDecisionProvider.Status.COMPLETE);
        assertTrue(fixture.sources().get(0).isTapped());
        assertTrue(fixture.remaining().isPaid());
        assertEquals(fixture.ability().getPayingManaAbilities().size(), 1);
    }

    @Test
    public void partialPaymentRegeneratesWithoutReofferingConsumedSource() {
        final PaymentFixture fixture = fixture("Dark Banishing", "Swamp", "Swamp", "Swamp");
        final ActionContinuation continuation = new ActionContinuation(481L,
                PriorityActionKind.CAST_SPELL, "Dark Banishing");
        final DecisionRequest first = provider.generatePaymentRequest(fixture.remaining(), fixture.ability(),
                fixture.payer(), identityMatrix(), continuation).getRequest();
        final LegalCandidate selected = first.getCandidates().get(0);

        final PaymentDecisionProvider.Generation result = provider.apply(first, selected);
        final DecisionRequest next = result.getRequest();

        assertEquals(result.getStatus(), PaymentDecisionProvider.Status.DECISION);
        assertEquals(next.getCandidates().size(), 2);
        assertFalse(next.getCandidates().stream()
                .anyMatch(candidate -> candidate.getSourceCardId() == selected.getSourceCardId()));
        assertEquals(next.getPaymentContext().getDecisionSequenceId(), Long.valueOf(481L));
        assertEquals(next.getPaymentContext().getSubdecisionIndex(), Integer.valueOf(2));
    }

    @Test
    public void staleSourceCandidateIsRejectedWithoutMutation() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Mountain");
        final DecisionRequest request = decision(fixture);
        fixture.sources().get(0).tap(true, null, fixture.payer());

        assertThrows(IllegalStateException.class,
                () -> provider.apply(request, request.getCandidates().get(0)));
        assertFalse(fixture.remaining().isPaid());
    }

    @Test
    public void candidateFromAnotherRequestIsRejected() {
        final PaymentFixture firstFixture = fixture("Lightning Bolt", "Mountain");
        final DecisionRequest first = decision(firstFixture);
        final PaymentFixture secondFixture = fixture("Lightning Bolt", "Mountain");
        final DecisionRequest second = decision(secondFixture);

        assertThrows(IllegalArgumentException.class,
                () -> provider.apply(first, second.getCandidates().get(0)));
    }

    @Test
    public void phyrexianManaFailsWithStructuredUnsupportedReason() {
        final PaymentFixture fixture = fixture("Gitaxian Probe", "Island");

        final PaymentDecisionProvider.Generation generation = generate(fixture);

        assertEquals(generation.getStatus(), PaymentDecisionProvider.Status.UNSUPPORTED);
        assertEquals(generation.getUnsupportedReason(),
                PaymentDecisionProvider.UnsupportedReason.PHYREXIAN_MANA);
    }

    @Test
    public void zeroCostSubAbilityCannotReplaceRootPaymentAction() {
        final PaymentFixture fixture = fixture("Dark Banishing", "Swamp", "Swamp", "Swamp");
        final AbilitySub sub = new AbilitySub(ApiType.Draw, fixture.ability().getHostCard(), null, Map.of());
        sub.setParent(fixture.ability());
        sub.setActivatingPlayer(fixture.payer());

        final DecisionRequest request = provider.generatePaymentRequest(fixture.remaining(), sub,
                fixture.payer(), identityMatrix(), null).getRequest();

        assertEquals(request.getPaymentContext().getRemainingCostSummary(), "{2}{B}");
        assertSame(request.getPaymentContext().getAbility(), fixture.ability());
    }

    @Test
    public void requestUsesCurrentLiveRemainingCostAfterEarlierAnnouncementState() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Mountain", "Mountain");
        fixture.remaining().increaseGenericMana(1);

        final DecisionRequest request = decision(fixture);

        assertEquals(request.getPaymentContext().getRemainingCostSummary(), "{1}{R}");
    }

    @Test
    public void paymentOutsidePrioritySequenceRemainsUncorrelated() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Mountain");

        final PaymentDecisionContext context = decision(fixture).getPaymentContext();

        assertNull(context.getDecisionSequenceId());
        assertNull(context.getSubdecisionIndex());
        assertFalse(context.hasActionContinuation());
    }

    @Test
    public void sameSourceSameColorFloatingObjectsAreNotSilentlyMerged() {
        final PaymentFixture fixture = fixture("Unsummon", "Island");
        final Mana first = new Mana((byte) ManaAtom.BLUE, fixture.sources().get(0), null, fixture.payer());
        final Mana second = new Mana((byte) ManaAtom.BLUE, fixture.sources().get(0), null, fixture.payer());
        fixture.payer().getManaPool().addManaNoEvent(first);
        fixture.payer().getManaPool().addManaNoEvent(second);
        fixture.sources().get(0).tap(true, null, fixture.payer());

        final DecisionRequest request = decision(fixture);

        assertEquals(request.getCandidates().size(), 2);
        assertNotEquals(request.getCandidates().get(0).getSemanticKey(),
                request.getCandidates().get(1).getSemanticKey());
    }

    @Test
    public void unsupportedStateDoesNotConsumeContinuationSubdecision() {
        final ActionContinuation continuation = new ActionContinuation(481L,
                PriorityActionKind.CAST_SPELL, "test");
        final PaymentFixture unsupported = fixture("Unsummon", "Birds of Paradise");
        unsupported.sources().get(0).setSickness(false);
        final PaymentFixture supported = fixture("Lightning Bolt", "Mountain");

        assertEquals(provider.generatePaymentRequest(unsupported.remaining(), unsupported.ability(),
                unsupported.payer(), identityMatrix(), continuation).getStatus(),
                PaymentDecisionProvider.Status.UNSUPPORTED);
        final DecisionRequest request = provider.generatePaymentRequest(supported.remaining(), supported.ability(),
                supported.payer(), identityMatrix(), continuation).getRequest();

        assertEquals(request.getPaymentContext().getSubdecisionIndex(), Integer.valueOf(1));
    }

    @Test
    public void nonIdentityExtraManaMatrixIsExplicitlyUnsupportedInsteadOfFalseInvalid() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Island");
        final ManaConversionMatrix matrix = identityMatrix();
        matrix.adjustColorReplacement((byte) ManaAtom.BLUE, (byte) ManaAtom.RED, true);

        final PaymentDecisionProvider.Generation generation = provider.generatePaymentRequest(
                fixture.remaining(), fixture.ability(), fixture.payer(), matrix, null);

        assertEquals(generation.getStatus(), PaymentDecisionProvider.Status.UNSUPPORTED);
        assertEquals(generation.getUnsupportedReason(),
                PaymentDecisionProvider.UnsupportedReason.MANA_CONVERSION_MATRIX);
    }

    @Test
    public void nullMatrixUsesOrdinaryNoExtraMatrixCandidateSemantics() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Mountain", "Mountain");
        final DecisionRequest identityRequest = provider.generatePaymentRequest(
                fixture.remaining(), fixture.ability(), fixture.payer(), identityMatrix(), null).getRequest();

        final PaymentDecisionProvider.Generation nullGeneration = provider.generatePaymentRequest(
                fixture.remaining(), fixture.ability(), fixture.payer(), null, null);

        assertEquals(nullGeneration.getStatus(), PaymentDecisionProvider.Status.DECISION);
        assertEquals(keys(nullGeneration.getRequest()), keys(identityRequest));
        assertEquals(nullGeneration.getRequest().isForced(), identityRequest.isForced());
        assertNull(nullGeneration.getRequest().getPaymentContext().getMatrix());
    }

    @Test
    public void applyingRequestWithNullMatrixCompletesThroughForge() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Mountain");
        final DecisionRequest request = provider.generatePaymentRequest(
                fixture.remaining(), fixture.ability(), fixture.payer(), null, null).getRequest();

        final PaymentDecisionProvider.Generation result = provider.apply(
                request, request.getCandidates().get(0));

        assertEquals(result.getStatus(), PaymentDecisionProvider.Status.COMPLETE);
        assertTrue(fixture.sources().get(0).isTapped());
        assertTrue(fixture.remaining().isPaid());
    }

    @Test
    public void effectiveMatrixAlreadyAppliedToLivePoolIsUsedForLegality() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Island");
        fixture.payer().getManaPool().adjustColorReplacement(
                (byte) ManaAtom.BLUE, (byte) ManaAtom.RED, true);

        final PaymentDecisionProvider.Generation generation = provider.generatePaymentRequest(
                fixture.remaining(), fixture.ability(), fixture.payer(), fixture.payer().getManaPool(), null);

        assertEquals(generation.getStatus(), PaymentDecisionProvider.Status.DECISION);
        assertEquals(generation.getRequest().getCandidates().size(), 1);
        assertEquals(generation.getRequest().getCandidates().get(0).getPaymentKind(),
                PaymentCandidateKind.ACTIVATE_MANA_SOURCE);
    }

    @Test
    public void playableAlternativeManaActivationIsNotSilentlyOmitted() {
        final PaymentFixture fixture = fixture("Unsummon");
        final Player opponent = fixture.payer().getGame().getPlayers().get(0);
        addCard("Island", opponent);
        fixture.payer().addChangedKeywords(List.of("Piracy"), null,
                fixture.payer().getGame().getNextTimestamp(), 0L);

        final PaymentDecisionProvider.Generation generation = generate(fixture);

        assertEquals(generation.getStatus(), PaymentDecisionProvider.Status.UNSUPPORTED);
        assertEquals(generation.getUnsupportedReason(),
                PaymentDecisionProvider.UnsupportedReason.MANA_SOURCE_ALTERNATIVE_COST);
    }

    @Test
    public void produceManaReplacementIsUnsupportedBeforePrintedOutputIsExported() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Mountain");
        addCard("Contamination", fixture.payer());

        final PaymentDecisionProvider.Generation generation = generate(fixture);

        assertEquals(generation.getStatus(), PaymentDecisionProvider.Status.UNSUPPORTED);
        assertEquals(generation.getUnsupportedReason(),
                PaymentDecisionProvider.UnsupportedReason.MANA_PRODUCTION_REPLACEMENT);
    }

    @Test
    public void futurePaymentSupportAssessmentReusesReplacementGateWithoutMutatingLiveManaAbility() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Mountain");
        addCard("Contamination", fixture.payer());
        final SpellAbility liveManaAbility = fixture.sources().get(0).getManaAbilities().get(0);
        final Player activatingPlayerBefore = liveManaAbility.getActivatingPlayer();

        final PaymentDecisionProvider.SupportAssessment assessment = provider.assessFuturePaymentSupport(
                fixture.ability(), fixture.payer());

        assertEquals(assessment.getStatus(), PaymentDecisionProvider.SupportStatus.UNSUPPORTED);
        assertEquals(assessment.getUnsupportedReason(),
                PaymentDecisionProvider.UnsupportedReason.MANA_PRODUCTION_REPLACEMENT);
        assertSame(liveManaAbility.getActivatingPlayer(), activatingPlayerBefore);
        assertFalse(fixture.sources().get(0).isTapped());
        assertTrue(fixture.payer().getManaPool().isEmpty());
    }

    @Test
    public void dynamicManaAmountIsNotClassifiedFromOrigProducedAlone() {
        final PaymentFixture fixture = fixture("Dark Banishing", "Everflowing Chalice");

        final PaymentDecisionProvider.Generation generation = generate(fixture);

        assertEquals(generation.getStatus(), PaymentDecisionProvider.Status.UNSUPPORTED);
        assertEquals(generation.getUnsupportedReason(),
                PaymentDecisionProvider.UnsupportedReason.DYNAMIC_MANA_PRODUCTION);
    }

    @Test
    public void nonManaSubAbilityOnSourceIsExplicitlyUnsupported() {
        final PaymentFixture fixture = fixture("Lightning Bolt", "Barbarian Ring");

        final PaymentDecisionProvider.Generation generation = generate(fixture);

        assertEquals(generation.getStatus(), PaymentDecisionProvider.Status.UNSUPPORTED);
        assertEquals(generation.getUnsupportedReason(),
                PaymentDecisionProvider.UnsupportedReason.NONTRIVIAL_MANA_SUBABILITY);
    }

    private PaymentFixture fixture(final String spellName, final String... sources) {
        final Game game = initAndCreateGame();
        final Player payer = game.getPlayers().get(1);
        final Card spellCard = addCardToZone(spellName, payer, ZoneType.Hand);
        final List<Card> sourceCards = new ArrayList<>();
        for (final String source : sources) {
            sourceCards.add(addCard(source, payer));
        }
        final SpellAbility spell = spellCard.getFirstSpellAbility();
        spell.setActivatingPlayer(payer);
        final ManaCostBeingPaid remaining = new ManaCostBeingPaid(
                spell.getPayCosts().getCostMana().getManaCostFor(spell));
        return new PaymentFixture(payer, spell, remaining, sourceCards);
    }

    private PaymentDecisionProvider.Generation generate(final PaymentFixture fixture) {
        return provider.generatePaymentRequest(fixture.remaining(), fixture.ability(), fixture.payer(),
                identityMatrix(), null);
    }

    private static ManaConversionMatrix identityMatrix() {
        final ManaConversionMatrix matrix = new ManaConversionMatrix();
        matrix.restoreColorReplacements();
        return matrix;
    }

    private DecisionRequest decision(final PaymentFixture fixture) {
        final PaymentDecisionProvider.Generation generation = generate(fixture);
        assertEquals(generation.getStatus(), PaymentDecisionProvider.Status.DECISION);
        return generation.getRequest();
    }

    private static List<String> keys(final DecisionRequest request) {
        return request.getCandidates().stream().map(LegalCandidate::getSemanticKey).toList();
    }

    private record PaymentFixture(Player payer, SpellAbility ability, ManaCostBeingPaid remaining,
                                  List<Card> sources) {
    }
}
