package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class SimultaneousTriggerOrderCoordinatorTest extends AITest {
    @Test
    public void semanticAndNativeOrderArePureReverses() {
        final List<String> nativeInsertion = List.of("A", "B", "C", "D");

        assertEquals(SimultaneousTriggerOrderDecisionCoordinator.toSemanticResolveFirst(nativeInsertion),
                List.of("D", "C", "B", "A"));
        assertEquals(SimultaneousTriggerOrderDecisionCoordinator.toNativeInsertion(
                SimultaneousTriggerOrderDecisionCoordinator.toSemanticResolveFirst(nativeInsertion)),
                nativeInsertion);
    }

    @Test
    public void externalSessionCreatesExactlyNMinusOneAuthoritativeRemainingRequests() {
        final Fixture fixture = fixture(4);
        final SimultaneousTriggerOrderDecisionProvider provider =
                new SimultaneousTriggerOrderDecisionProvider();
        final List<DecisionRequest> requests = new ArrayList<>();
        final AtomicInteger nativeCalls = new AtomicInteger();
        provider.setResolver(request -> {
            requests.add(request);
            return request.getCandidates().get(0);
        });

        final List<SpellAbility> result = new SimultaneousTriggerOrderDecisionCoordinator().order(
                fixture.entries, fixture.player, provider, input -> {
                    nativeCalls.incrementAndGet();
                    return input;
                });

        assertEquals(nativeCalls.get(), 0);
        assertEquals(requests.size(), 3);
        assertEquals(requests.stream().map(request -> request.getCandidates().size()).toList(),
                List.of(4, 3, 2));
        assertEquals(requests.stream().map(request -> request.getOrderContext().getStepIndex()).toList(),
                List.of(0, 1, 2));
        assertEquals(requests.stream().map(request -> request.getOrderContext().getOriginalItemCount()).toList(),
                List.of(4, 4, 4));
        assertEquals(requests.stream().map(request -> request.getOrderContext().getOrderSessionId()).distinct()
                .toList(), List.of(1L));
        assertEquals(result, List.of(fixture.entries.get(3), fixture.entries.get(2),
                fixture.entries.get(1), fixture.entries.get(0)));
    }

    @Test
    public void nativeSessionCallsTeacherExactlyOnceAndReturnsItsInsertionPermutation() {
        final Fixture fixture = fixture(4);
        final SimultaneousTriggerOrderDecisionProvider provider =
                new SimultaneousTriggerOrderDecisionProvider();
        final AtomicInteger nativeCalls = new AtomicInteger();
        final List<SpellAbility> nativeResult = List.of(
                fixture.entries.get(2), fixture.entries.get(0),
                fixture.entries.get(3), fixture.entries.get(1));

        final List<SpellAbility> result = new SimultaneousTriggerOrderDecisionCoordinator().order(
                fixture.entries, fixture.player, provider, input -> {
                    nativeCalls.incrementAndGet();
                    return nativeResult;
                });

        assertEquals(nativeCalls.get(), 1);
        assertSame(result, nativeResult);
    }

    @Test
    public void unsupportedAdmissionUsesNativeOnlyWithoutResolverAndHardFailsWithResolver() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(0);
        final SpellAbility first = addCard("Island", player).getFirstSpellAbility();
        final SpellAbility second = addCard("Mountain", player).getFirstSpellAbility();
        final List<SpellAbility> input = new ArrayList<>(List.of(first, second));
        final AtomicInteger nativeCalls = new AtomicInteger();
        final List<SpellAbility> nativeResult = List.of(second);

        final List<SpellAbility> result = new SimultaneousTriggerOrderDecisionCoordinator().order(
                input, player, new SimultaneousTriggerOrderDecisionProvider(), ignored -> {
                    nativeCalls.incrementAndGet();
                    input.clear();
                    return nativeResult;
                });

        assertEquals(nativeCalls.get(), 1);
        assertSame(result, nativeResult);
        assertEquals(input, List.of());

        final SimultaneousTriggerOrderDecisionProvider external =
                new SimultaneousTriggerOrderDecisionProvider();
        external.setResolver(request -> request.getCandidates().get(0));
        final SimultaneousTriggerOrderIntegrityException exception = expectThrows(
                SimultaneousTriggerOrderIntegrityException.class,
                () -> new SimultaneousTriggerOrderDecisionCoordinator().order(
                        List.of(first, second), player, external, ignored -> {
                            throw new AssertionError("native callback must not run");
                        }));
        assertEquals(exception.getReason(), "UNSUPPORTED_ADMISSION");
    }

    @Test
    public void duplicateNativeIdentityIsIntegrityFailureEvenWithoutResolver() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(0);
        final SpellAbility same = addCard("Island", player).getFirstSpellAbility();
        final List<SpellAbility> duplicate = List.of(same, same);

        final SimultaneousTriggerOrderIntegrityException exception = expectThrows(
                SimultaneousTriggerOrderIntegrityException.class,
                () -> new SimultaneousTriggerOrderDecisionCoordinator().order(
                        duplicate, player, new SimultaneousTriggerOrderDecisionProvider(),
                        ignored -> {
                            throw new AssertionError("integrity failure must not fall back");
                        }));
        assertEquals(exception.getReason(), "SESSION_INTEGRITY_FAILURE");
    }

    @Test
    public void singletonAndEmptyExternalPathsEmitNoRequestsOrCallbacks() {
        final Fixture fixture = fixture(1);
        final SimultaneousTriggerOrderDecisionProvider provider =
                new SimultaneousTriggerOrderDecisionProvider();
        final AtomicInteger resolverCalls = new AtomicInteger();
        final AtomicInteger nativeCalls = new AtomicInteger();
        provider.setResolver(request -> {
            resolverCalls.incrementAndGet();
            return request.getCandidates().get(0);
        });

        final List<SpellAbility> singleton = new SimultaneousTriggerOrderDecisionCoordinator().order(
                fixture.entries, fixture.player, provider, ignored -> {
                    nativeCalls.incrementAndGet();
                    return ignored;
                });
        final List<SpellAbility> empty = new SimultaneousTriggerOrderDecisionCoordinator().order(
                List.of(), fixture.player, provider, ignored -> {
                    nativeCalls.incrementAndGet();
                    return ignored;
                });

        assertSame(singleton.get(0), fixture.entries.get(0));
        assertEquals(empty, List.of());
        assertEquals(resolverCalls.get(), 0);
        assertEquals(nativeCalls.get(), 0);
    }

    @Test
    public void nativeMappingFailureClosesOnlyStepZero() throws Exception {
        final Fixture fixture = fixture(3);
        final Path directory = Files.createTempDirectory("frl02l1-native-map-");
        final DeterminismTrace trace = DeterminismTrace.attach(fixture.game, 0,
                new forge.util.DeterminismAuditRandom(20260813L), directory);
        try {
            final SimultaneousTriggerOrderIntegrityException exception = expectThrows(
                    SimultaneousTriggerOrderIntegrityException.class,
                    () -> new SimultaneousTriggerOrderDecisionCoordinator().order(
                            fixture.entries, fixture.player,
                            new SimultaneousTriggerOrderDecisionProvider(),
                            ignored -> List.of(fixture.entries.get(0), fixture.entries.get(1))));
            assertEquals(exception.getReason(), "MAPPING_FAILED");
            trace.finish();
            final List<String> records = Files.readAllLines(directory.resolve("game-001.decision.trace"),
                    StandardCharsets.UTF_8);
            assertEquals(records.size(), 2);
            assertTrue(records.get(0).contains("|REQUEST|0|"));
            assertTrue(records.get(1).contains("|MAPPING_FAILED|"));
        } finally {
            deleteDirectory(directory);
        }
    }

    @Test
    public void nativeCallbackFailureClosesActiveRequestWithoutFallback() throws Exception {
        final Fixture fixture = fixture(2);
        final Path directory = Files.createTempDirectory("frl02l1-native-failure-");
        final DeterminismTrace trace = DeterminismTrace.attach(fixture.game, 0,
                new forge.util.DeterminismAuditRandom(20260813L), directory);
        try {
            final SimultaneousTriggerOrderIntegrityException exception = expectThrows(
                    SimultaneousTriggerOrderIntegrityException.class,
                    () -> new SimultaneousTriggerOrderDecisionCoordinator().order(
                            fixture.entries, fixture.player,
                            new SimultaneousTriggerOrderDecisionProvider(),
                            ignored -> {
                                throw new IllegalStateException("native detail must be sanitized");
                            }));
            assertEquals(exception.getReason(), "NATIVE_CALLBACK_FAILURE");
            trace.finish();
            final List<String> records = Files.readAllLines(directory.resolve("game-001.decision.trace"),
                    StandardCharsets.UTF_8);
            assertEquals(records.size(), 2);
            assertTrue(records.get(1).contains("|NATIVE_CALLBACK_FAILURE|"));
            assertTrue(exception.getMessage().contains("NATIVE_CALLBACK_FAILURE"));
            assertTrue(!exception.getMessage().contains("native detail"));
        } finally {
            deleteDirectory(directory);
        }
    }

    @Test
    public void externalInvalidityClosesOnlyTheActiveRequestWithoutFallback() throws Exception {
        final Fixture fixture = fixture(3);
        final Path directory = Files.createTempDirectory("frl02l1-external-failure-");
        final DeterminismTrace trace = DeterminismTrace.attach(fixture.game, 0,
                new forge.util.DeterminismAuditRandom(20260813L), directory);
        final SimultaneousTriggerOrderDecisionProvider provider =
                new SimultaneousTriggerOrderDecisionProvider();
        provider.setResolver(ignored -> null);
        try {
            final SimultaneousTriggerOrderIntegrityException exception = expectThrows(
                    SimultaneousTriggerOrderIntegrityException.class,
                    () -> new SimultaneousTriggerOrderDecisionCoordinator().order(
                            fixture.entries, fixture.player, provider,
                            ignored -> {
                                throw new AssertionError("external invalidity must not fall back");
                            }));
            assertEquals(exception.getReason(), "INVALID_EXTERNAL_CANDIDATE");
            trace.finish();
            final List<String> records = Files.readAllLines(directory.resolve("game-001.decision.trace"),
                    StandardCharsets.UTF_8);
            assertEquals(records.size(), 2);
            assertTrue(records.get(1).contains("|INVALID_EXTERNAL_CANDIDATE|"));
        } finally {
            deleteDirectory(directory);
        }
    }

    private Fixture fixture(final int count) {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(0);
        final List<SpellAbility> entries = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            entries.add(wrapperFor(addCard("Gelectrode", player), player));
        }
        return new Fixture(game, player, entries);
    }

    private WrappedAbility wrapperFor(final Card source, final Player player) {
        final Trigger trigger = source.getTriggers().stream()
                .filter(value -> TriggerType.SpellCast.equals(value.getMode()))
                .findFirst()
                .orElseThrow();
        final SpellAbility effect = AbilityFactory.getAbility(source, trigger.getParam("Execute"));
        effect.setActivatingPlayer(player);
        effect.setOptionalTrigger(true);
        effect.setIntrinsic(true);
        final Card castSpell = addCardToZone("Opt", player, ZoneType.Hand);
        final SpellAbility castAbility = castSpell.getFirstSpellAbility();
        castAbility.setActivatingPlayer(player);
        final Map<AbilityKey, Object> triggeringObjects = AbilityKey.newMap();
        triggeringObjects.put(AbilityKey.Activator, player);
        triggeringObjects.put(AbilityKey.SpellAbility, castAbility);
        trigger.setTriggeringObjects(effect, triggeringObjects);
        return new WrappedAbility(trigger, effect, player);
    }

    private static final class Fixture {
        private final Game game;
        private final Player player;
        private final List<SpellAbility> entries;

        private Fixture(final Game game, final Player player, final List<SpellAbility> entries) {
            this.game = game;
            this.player = player;
            this.entries = entries;
        }
    }

    private static void deleteDirectory(final Path directory) throws Exception {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (var files = Files.list(directory)) {
            for (final Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
        }
        Files.deleteIfExists(directory);
    }
}
