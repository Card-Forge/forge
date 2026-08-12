package forge.game.decision;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Fresh-JVM proof that an active single-action continuation fails closed at the C2A boundary. */
public final class TriggeredTargetContinuationChildMain {
    private static final String PRIORITY_METRICS_PROPERTY = "forge.priority.metricsFile";

    private TriggeredTargetContinuationChildMain() {
    }

    public static void main(final String[] args) throws Exception {
        final Path metricsFile = Files.createTempFile("frl02k-c2a-continuation-", ".csv");
        System.setProperty(PRIORITY_METRICS_PROPERTY, metricsFile.toString());

        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        final ByteArrayOutputStream suppressedOutput = new ByteArrayOutputStream();
        final PrintStream quietOutput = new PrintStream(suppressedOutput, true, StandardCharsets.UTF_8);
        ProofResult result;
        try {
            System.setOut(quietOutput);
            System.setErr(quietOutput);
            result = new ProofRunner(args.length > 0 && "external".equals(args[0])).run();
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
            quietOutput.close();
        }

        System.out.println("reason=" + result.reason());
        System.out.println("provider_requests=" + result.providerRequests());
        System.out.println("resolver_present=" + result.resolverPresent());
        System.out.println("resolver_calls=" + result.resolverCalls());
        System.out.println("native_calls=" + result.nativeCalls());
    }

    private static final class ProofRunner extends AITest {
        private final boolean externalResolver;

        private ProofRunner(final boolean externalResolver) {
            this.externalResolver = externalResolver;
        }

        private ProofResult run() throws Exception {
            initializeModel();
            final BloodFixture fixture = bloodFixture();
            final CountingTargetController nativeController = installCountingController(
                    fixture.game(), fixture.chooser());
            final TargetDecisionProvider provider = nativeController.getTargetDecisionProvider();
            final long providerRequestStart = providerRequestSequence(provider);
            final AtomicInteger resolverCalls = new AtomicInteger();
            if (externalResolver) {
                nativeController.setTargetDecisionResolver(request -> {
                    resolverCalls.incrementAndGet();
                    return null;
                });
                require(nativeController.getTargetDecisionResolver() != null,
                        "external continuation proof must install an external resolver");
            } else {
                require(nativeController.getTargetDecisionResolver() == null,
                        "native continuation proof must run without an external resolver");
            }
            String reason = null;
            int providerRequests = -1;
            try {
                final PriorityActionDiagnostics.Capture capture = PriorityActionDiagnostics.capture(
                        fixture.chooser());
                require(capture != null, "priority capture was unavailable");
                PriorityActionDiagnostics.beginAction(capture, fixture.priorityAction(), 1);
                require(PriorityActionDiagnostics.hasActiveActionContinuation(),
                        "single-action continuation was not opened");

                try {
                    nativeController.playTrigger(fixture.source(), fixture.wrapper(), true);
                    throw new IllegalStateException("continuation boundary did not fail early");
                } catch (final TriggeredTargetIntegrityException exception) {
                    reason = exception.getReason();
                }

                providerRequests = Math.toIntExact(providerRequestSequence(provider) - providerRequestStart);
                final int nativeCalls = nativeController.getNativeCalls();
                final int resolverCallCount = resolverCalls.get();
                require("UNSUPPORTED_ACTION_CONTINUATION".equals(reason),
                        "unexpected continuation reason: " + reason);
                require(providerRequests == 0, "target provider generated a request");
                require(resolverCallCount == 0, "external resolver was invoked");
                require(nativeCalls == 0, "native target callback was invoked");
                return new ProofResult(reason, providerRequests, externalResolver,
                        resolverCallCount, nativeCalls);
            } finally {
                PriorityActionDiagnostics.endAction();
            }
        }

        private BloodFixture bloodFixture() {
            final Game game = initAndCreateGame();
            final Player chooser = game.getPlayers().get(1);
            final Player opponent = game.getPlayers().get(0);
            final Card source = addCardToZone("Blood Operative", chooser, ZoneType.Battlefield);
            final Card firstTarget = addCardToZone("Runeclaw Bear", opponent, ZoneType.Graveyard);
            final Card secondTarget = addCardToZone("Llanowar Elves", opponent, ZoneType.Graveyard);
            final Trigger trigger = source.getTriggers().stream()
                    .filter(candidate -> candidate.getMode() == TriggerType.ChangesZone)
                    .filter(candidate -> "TrigChangeZone".equals(candidate.getParam("Execute")))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Blood trigger fixture is unavailable"));
            final SpellAbility ability = trigger.ensureAbility();
            ability.setActivatingPlayer(chooser);
            final Card prioritySource = addCardToZone("Island", chooser, ZoneType.Hand);
            final SpellAbility priorityAction = prioritySource.getSpellAbilities().stream()
                    .filter(SpellAbility::isLandAbility)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("priority action fixture is unavailable"));
            priorityAction.setActivatingPlayer(chooser);
            return new BloodFixture(game, chooser, opponent, source, trigger, ability, firstTarget,
                    List.of(firstTarget.getId(), secondTarget.getId()).stream().sorted().toList(),
                    new WrappedAbility(trigger, ability, chooser), priorityAction);
        }

        private static CountingTargetController installCountingController(final Game game, final Player player) {
            final CountingTargetController controller = new CountingTargetController(
                    game, player);
            player.dangerouslySetController(controller);
            return controller;
        }
    }

    private static long providerRequestSequence(final TargetDecisionProvider provider) {
        try {
            final Field requestSequence = TargetDecisionProvider.class.getDeclaredField("nextRequestId");
            requestSequence.setAccessible(true);
            return requestSequence.getLong(provider);
        } catch (final ReflectiveOperationException | SecurityException exception) {
            throw new IllegalStateException("target provider request sequence unavailable", exception);
        }
    }

    private static void require(final boolean condition, final String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static final class CountingTargetController extends PlayerControllerAi {
        private int nativeCalls;

        private CountingTargetController(final Game game, final Player player) {
            super(game, player, new LobbyPlayerAi(player.getName() + "-frl02k-c2a-child", null));
        }

        @Override
        protected boolean invokeNativeTriggeredTarget(final SpellAbility underlying, final boolean mandatory) {
            nativeCalls++;
            return super.invokeNativeTriggeredTarget(underlying, mandatory);
        }

        private int getNativeCalls() {
            return nativeCalls;
        }
    }

    private record ProofResult(String reason, int providerRequests, boolean resolverPresent,
            int resolverCalls, int nativeCalls) {
    }

    private record BloodFixture(Game game, Player chooser, Player opponent, Card source,
            Trigger trigger, SpellAbility ability, Card firstTarget, List<Integer> targetIds,
            WrappedAbility wrapper, SpellAbility priorityAction) {
    }
}
