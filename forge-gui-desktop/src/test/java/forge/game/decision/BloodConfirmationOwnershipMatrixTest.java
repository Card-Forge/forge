package forge.game.decision;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.util.DeterminismAuditRandom;
import forge.util.MyRandom;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/** First bounded FRL-02K-D1 ownership-matrix RED slice. */
public class BloodConfirmationOwnershipMatrixTest extends AITest {
    private static final long DETERMINISTIC_SEED = 20260813L;

    @Test
    public void externalTargetAndExternalConfirmationUseOneBloodEffectRoute() throws Exception {
        final Random previousRandom = MyRandom.getRandom();
        final DeterminismAuditRandom auditRandom = new DeterminismAuditRandom(DETERMINISTIC_SEED);
        Path traceDirectory = null;
        DeterminismTrace trace = null;
        MyRandom.setRandom(auditRandom);

        try {
            final BloodFixture fixture = bloodFixture();
            assertBloodFixture(fixture);

            final CountingController controller = installController(fixture);
            final AtomicInteger targetResolverCalls = new AtomicInteger();
            final AtomicReference<DecisionRequest> targetRequest = new AtomicReference<>();
            final AtomicReference<LegalCandidate> selectedTarget = new AtomicReference<>();
            controller.setTargetDecisionResolver(request -> {
                targetResolverCalls.incrementAndGet();
                targetRequest.set(request);
                final LegalCandidate selected = request.getCandidates().stream()
                        .filter(candidate -> candidate.getTargetKind() == TargetCandidateKind.TARGET_CARD)
                        .filter(candidate -> candidate.getTarget() == fixture.legalCard())
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Blood target A is not in the legal request"));
                selectedTarget.set(selected);
                return selected;
            });

            final AtomicInteger confirmationResolverCalls = new AtomicInteger();
            final AtomicReference<DecisionRequest> confirmationRequest = new AtomicReference<>();
            final AtomicReference<LegalCandidate> selectedConfirmation = new AtomicReference<>();
            controller.getConfirmationDecisionProvider().setResolver(request -> {
                confirmationResolverCalls.incrementAndGet();
                confirmationRequest.set(request);
                final LegalCandidate selected = request.getCandidates().stream()
                        .filter(candidate -> candidate.getConfirmationKind() == ConfirmationCandidateKind.ACCEPT)
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("ACCEPT is not in the confirmation request"));
                selectedConfirmation.set(selected);
                return selected;
            });

            traceDirectory = Files.createTempDirectory("frl02k-d1-blood-confirmation-");
            trace = DeterminismTrace.attach(fixture.game(), 0, auditRandom, traceDirectory);

            UnsupportedConfirmationDecisionException unsupportedBaseline = null;
            Boolean routeResult = null;
            try {
                routeResult = controller.playTrigger(fixture.source(), fixture.wrapper(), true);
            } catch (final UnsupportedConfirmationDecisionException ex) {
                // Current B1 is intentionally narrower than Blood. Keep the RED at assertions.
                unsupportedBaseline = ex;
            }

            trace.finish();
            final Path decisionTrace = traceDirectory.resolve("game-001.decision.trace");
            final List<String> records = Files.exists(decisionTrace)
                    ? Files.readAllLines(decisionTrace, StandardCharsets.UTF_8) : List.of();
            final List<String> requestRecords = records.stream()
                    .filter(record -> record.startsWith("DECISION_TRACE_V2|REQUEST|"))
                    .toList();

            final SoftAssert assertions = new SoftAssert();
            assertions.assertNull(unsupportedBaseline,
                    "external Blood confirmation must complete without an unsupported-profile exception");
            assertions.assertTrue(Boolean.TRUE.equals(routeResult),
                    "the real PlayerControllerAi playTrigger route must complete");
            assertions.assertEquals(targetResolverCalls.get(), 1,
                    "the external TARGET resolver must be called exactly once");
            assertions.assertEquals(confirmationResolverCalls.get(), 1,
                    "the external CONFIRMATION resolver must be called exactly once");
            assertions.assertSame(selectedTarget.get(), fixture.legalCard(),
                    "the TARGET resolver must select card A");
            assertions.assertEquals(controller.nativeTargetCallbackCalls(), 0,
                    "external TARGET ownership must not invoke the native target callback");
            assertions.assertEquals(controller.nativeConfirmationCallbackCalls(), 0,
                    "external CONFIRMATION ownership must not invoke the native confirmation callback");
            assertions.assertNull(controller.targetAtNativeConfirmation(),
                    "native confirmation must not create a temporary target B");
            assertions.assertTrue(fixture.game().getCardsIn(ZoneType.Exile).contains(fixture.legalCard()),
                    "the existing ChangeZone effect must consume card A");
            assertions.assertFalse(fixture.game().getCardsIn(ZoneType.Graveyard).contains(fixture.legalCard()),
                    "card A must leave the graveyard exactly once");
            assertions.assertEquals(requestRecords.size(), 2,
                    "the route must trace exactly one TARGET and one CONFIRMATION request");
            assertions.assertEquals(requestRecords.stream()
                    .filter(record -> record.contains("|TARGET|"))
                    .count(), 1L, "the route must trace exactly one TARGET request");
            assertions.assertEquals(requestRecords.stream()
                    .filter(record -> record.contains("|CONFIRMATION|"))
                    .count(), 1L, "the route must trace exactly one CONFIRMATION request");

            final DecisionRequest observedTargetRequest = targetRequest.get();
            assertions.assertNotNull(observedTargetRequest, "the external TARGET request must be observable");
            if (observedTargetRequest != null) {
                assertions.assertEquals(observedTargetRequest.getDecisionType(), DecisionType.TARGET);
                assertions.assertEquals(observedTargetRequest.getCandidates().size(), 1);
                assertions.assertSame(observedTargetRequest.getCandidates().get(0).getTarget(),
                        fixture.legalCard());
            }

            final DecisionRequest observedConfirmationRequest = confirmationRequest.get();
            assertions.assertNotNull(observedConfirmationRequest,
                    "the external CONFIRMATION request must be observable");
            assertions.assertNotNull(selectedConfirmation.get(),
                    "the external CONFIRMATION resolver must select ACCEPT");
            if (observedConfirmationRequest != null) {
                assertions.assertEquals(observedConfirmationRequest.getDecisionType(), DecisionType.CONFIRMATION);
            }
            if (selectedConfirmation.get() != null) {
                assertions.assertEquals(selectedConfirmation.get().getConfirmationKind(),
                        ConfirmationCandidateKind.ACCEPT);
            }
            assertions.assertAll();
        } finally {
            if (trace != null) {
                trace.finish();
            }
            if (traceDirectory != null) {
                deleteTree(traceDirectory);
            }
            MyRandom.setRandom(previousRandom);
        }
    }

    private BloodFixture bloodFixture() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card source = addCardToZone("Blood Operative", chooser, ZoneType.Battlefield);
        final Card legalCard = addCardToZone("Runeclaw Bear", opponent, ZoneType.Graveyard);
        final Trigger trigger = source.getTriggers().stream()
                .filter(candidate -> candidate.getMode() == TriggerType.ChangesZone)
                .filter(candidate -> "Any".equals(candidate.getParam("Origin")))
                .filter(candidate -> "Battlefield".equals(candidate.getParam("Destination")))
                .filter(candidate -> "Card.Self".equals(candidate.getParam("ValidCard")))
                .filter(candidate -> "You".equals(candidate.getParam("OptionalDecider")))
                .filter(candidate -> "TrigChangeZone".equals(candidate.getParam("Execute")))
                .filter(Trigger::isIntrinsic)
                .filter(candidate -> !candidate.isStatic())
                .filter(candidate -> candidate.getSpawningAbility() == null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected Blood Operative ETB trigger is unavailable"));
        final SpellAbility ability = trigger.ensureAbility();
        ability.setActivatingPlayer(chooser);
        ability.setOptionalTrigger(true);
        return new BloodFixture(game, chooser, source, legalCard, trigger, ability,
                new WrappedAbility(trigger, ability, chooser));
    }

    private static void assertBloodFixture(final BloodFixture fixture) {
        assertEquals(fixture.source().getName(), "Blood Operative");
        assertEquals(fixture.source().getZone().getZoneType(), ZoneType.Battlefield);
        assertEquals(fixture.source().getController(), fixture.chooser());
        assertEquals(fixture.game().getCardsIn(ZoneType.Graveyard).size(), 1);
        assertTrue(fixture.game().getCardsIn(ZoneType.Graveyard).contains(fixture.legalCard()));
        assertEquals(fixture.trigger().getMode(), TriggerType.ChangesZone);
        assertTrue(fixture.trigger().isIntrinsic());
        assertFalse(fixture.trigger().isStatic());
        assertNull(fixture.trigger().getSpawningAbility());
        assertEquals(fixture.ability().getApi(), ApiType.ChangeZone);
        assertEquals(fixture.ability().getParam("Origin"), "Graveyard");
        assertEquals(fixture.ability().getParam("Destination"), "Exile");
        assertEquals(fixture.ability().getParam("ValidTgts"), "Card");
        final TargetRestrictions restrictions = fixture.ability().getTargetRestrictions();
        assertNotNull(restrictions);
        assertTrue(restrictions.getZone().contains(ZoneType.Graveyard));
        assertEquals(fixture.ability().getMinTargets(), 1);
        assertEquals(fixture.ability().getMaxTargets(), 1);
        assertTrue(fixture.ability().canTarget(fixture.legalCard()));
        assertTrue(fixture.ability().getTargets().isEmpty());
        assertEquals(fixture.wrapper().getDecider(), fixture.chooser());
        assertTrue(fixture.wrapper().isOptionalTrigger());
    }

    private static CountingController installController(final BloodFixture fixture) {
        final CountingController controller = new CountingController(fixture.game(), fixture.chooser());
        fixture.chooser().dangerouslySetController(controller);
        return controller;
    }

    private static void deleteTree(final Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static final class CountingController extends PlayerControllerAi {
        private int nativeTargetCallbackCalls;
        private int nativeConfirmationCallbackCalls;
        private GameObject targetAtNativeConfirmation;

        private CountingController(final Game game, final Player player) {
            super(game, player, new LobbyPlayerAi(player.getName() + "-frl02k-d1", null));
        }

        @Override
        protected boolean invokeNativeTriggeredTarget(final SpellAbility underlying, final boolean mandatory) {
            nativeTargetCallbackCalls++;
            return super.invokeNativeTriggeredTarget(underlying, mandatory);
        }

        @Override
        public boolean confirmTrigger(final WrappedAbility wrapper) {
            nativeConfirmationCallbackCalls++;
            if (wrapper.getWrappedAbility().getTargets().size() == 1) {
                targetAtNativeConfirmation = wrapper.getWrappedAbility().getTargets().get(0);
            }
            return super.confirmTrigger(wrapper);
        }

        private int nativeTargetCallbackCalls() {
            return nativeTargetCallbackCalls;
        }

        private int nativeConfirmationCallbackCalls() {
            return nativeConfirmationCallbackCalls;
        }

        private GameObject targetAtNativeConfirmation() {
            return targetAtNativeConfirmation;
        }
    }

    private record BloodFixture(Game game, Player chooser, Card source, Card legalCard,
            Trigger trigger, SpellAbility ability, WrappedAbility wrapper) {
    }
}
