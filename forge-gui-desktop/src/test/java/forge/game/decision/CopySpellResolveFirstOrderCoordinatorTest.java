package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class CopySpellResolveFirstOrderCoordinatorTest extends AITest {
    @Test
    public void admitsOnlyTheFactoryProducedCopiedSpellShapeAndCapturesPrivateIdentity() {
        final Fixture fixture = fixture(3);
        final CopySpellResolveFirstOrderDecisionCoordinator coordinator =
                new CopySpellResolveFirstOrderDecisionCoordinator();

        final CopySpellResolveFirstOrderDecisionCoordinator.Admission admission = coordinator.admit(
                fixture.copies, fixture.player, 1L);

        assertNotNull(admission);
        assertEquals(admission.getItems().size(), 3);
        assertEquals(admission.getItems().stream().map(CopySpellResolveFirstOrderItem::getItemId).toList(),
                List.of(1L, 2L, 3L));
        assertEquals(admission.getItems().stream()
                .map(item -> item.getSourceProjection().getVisibleOriginalSourceName()).distinct().toList(),
                List.of("Pyromatics"));
        assertTrue(admission.getNativeItem(fixture.copies.get(0))
                != admission.getNativeItem(fixture.copies.get(1)));
        assertSame(admission.getNativeEntry(fixture.copies.get(0)), fixture.copies.get(0));
    }

    @Test
    public void duplicatePublicProjectionsRemainDistinctCandidates() {
        final Fixture fixture = fixture(2);
        final CopySpellResolveFirstOrderDecisionCoordinator.Admission admission =
                new CopySpellResolveFirstOrderDecisionCoordinator().admit(
                        fixture.copies, fixture.player, 1L);

        assertNotNull(admission);
        assertEquals(admission.getItems().get(0).getSourceProjection(),
                admission.getItems().get(1).getSourceProjection());
        assertEquals(admission.getItems().get(0).getEffectApi(),
                admission.getItems().get(1).getEffectApi());
        assertTrue(admission.getItems().get(0).getItemId()
                != admission.getItems().get(1).getItemId());
    }

    @Test
    public void repeatedNativeIdentityIsAnIntegrityFailure() {
        final Fixture fixture = fixture(1);
        final List<SpellAbility> duplicate = List.of(fixture.copies.get(0), fixture.copies.get(0));

        final SimultaneousTriggerOrderIntegrityException exception = expectThrows(
                SimultaneousTriggerOrderIntegrityException.class,
                () -> new CopySpellResolveFirstOrderDecisionCoordinator().admit(
                        duplicate, fixture.player, 1L));
        assertEquals(exception.getReason(), "SESSION_INTEGRITY_FAILURE");
    }

    @Test
    public void malformedShapesAreRejectedWithoutCreatingAProjection() {
        final Fixture fixture = fixture(2);
        final CopySpellResolveFirstOrderDecisionCoordinator coordinator =
                new CopySpellResolveFirstOrderDecisionCoordinator();

        assertTrue(coordinator.admit(null, fixture.player, 1L) == null);
        assertTrue(coordinator.admit(List.of(), fixture.player, 1L) == null);
        assertTrue(coordinator.admit(List.of(fixture.copies.get(0)), fixture.player, 1L) == null);
        final List<SpellAbility> nullEntry = new ArrayList<>();
        nullEntry.add(null);
        nullEntry.add(fixture.copies.get(1));
        assertTrue(coordinator.admit(nullEntry, fixture.player, 1L) == null);

        final SpellAbility nonCopied = fixture.source.getFirstSpellAbility();
        nonCopied.setActivatingPlayer(fixture.player);
        assertTrue(coordinator.admit(List.of(nonCopied, fixture.copies.get(1)), fixture.player, 1L) == null);
        assertTrue(coordinator.admit(List.of((SpellAbility) fixture.triggerLike,
                fixture.copies.get(1)), fixture.player, 1L) == null);
    }

    @Test
    public void strictAdmissionMatrixKeepsFamilyIntentSeparateFromOwnership() {
        final CopySpellResolveFirstOrderDecisionCoordinator coordinator =
                new CopySpellResolveFirstOrderDecisionCoordinator();
        Fixture fixture = fixture(2);
        final SpellAbility copiedNonSpell = new SpellAbility.EmptySa(fixture.source, fixture.player);
        assertEquals(OrderProfileRouter.preClassify(List.of(fixture.copies.get(0), copiedNonSpell)),
                OrderProfileRouter.PreClassification.UNOWNED_OTHER);

        fixture = fixture(2);
        fixture.copies.get(0).getHostCard().setOwner(null);
        assertEquals(OrderProfileRouter.preClassify(fixture.copies),
                OrderProfileRouter.PreClassification.COPY_SPELL_FAMILY_INTENT);
        assertTrue(coordinator.admit(fixture.copies, fixture.player, 1L) == null);

        fixture = fixture(2);
        fixture.copies.get(0).getHostCard().getCopiedPermanent().setFaceDown(true);
        assertTrue(coordinator.admit(fixture.copies, fixture.player, 1L) == null);

        fixture = fixture(2);
        fixture.copies.get(0).setApi(null);
        assertTrue(coordinator.admit(fixture.copies, fixture.player, 1L) == null);

        fixture = fixture(2);
        final Player opponent = fixture.player.getGame().getPlayers().get(1);
        final Card opponentSource = addCardToZone("Pyromatics", opponent, ZoneType.Battlefield);
        final SpellAbility opponentOriginal = opponentSource.getFirstSpellAbility();
        opponentOriginal.setActivatingPlayer(opponent);
        final SpellAbility opponentCopy = CardFactory.copySpellAbilityAndPossiblyHost(
                opponentOriginal, opponentOriginal, opponent);
        final List<SpellAbility> mixedPlayers = List.of(fixture.copies.get(0), opponentCopy);
        assertEquals(OrderProfileRouter.preClassify(mixedPlayers),
                OrderProfileRouter.PreClassification.COPY_SPELL_FAMILY_INTENT);
        assertTrue(coordinator.admit(mixedPlayers, fixture.player, 1L) == null);

        fixture = fixture(2);
        final WrappedAbility trigger = new WrappedAbility(
                TriggerType.Always.createTrigger(new java.util.HashMap<>(), fixture.source, true),
                new SpellAbility.EmptySa(fixture.source, fixture.player), fixture.player);
        assertEquals(OrderProfileRouter.preClassify(List.of(fixture.copies.get(0), trigger)),
                OrderProfileRouter.PreClassification.UNOWNED_OTHER);
    }

    @Test
    public void externalSessionsForN2N3N4EmitNMinusOneTypedRequestsAndReverseOnce() {
        for (final int count : List.of(2, 3, 4)) {
            final Fixture fixture = fixture(count);
            final CopySpellResolveFirstOrderDecisionProvider provider =
                    new CopySpellResolveFirstOrderDecisionProvider();
            final List<DecisionRequest> requests = new ArrayList<>();
            provider.setResolver(request -> {
                requests.add(request);
                return request.getCandidates().get(0);
            });

            final List<SpellAbility> result = new CopySpellResolveFirstOrderDecisionCoordinator().order(
                    fixture.copies, fixture.player, provider,
                    ignored -> {
                        throw new AssertionError("native callback must not run for external L1C");
                    });

            assertEquals(requests.size(), count - 1);
            assertEquals(requests.stream().map(request -> request.getCandidates().size()).toList(),
                    java.util.stream.IntStream.rangeClosed(2, count).boxed().sorted(Collections.reverseOrder())
                            .toList());
            assertEquals(requests.stream().map(request -> request.getCopySpellResolveFirstOrderContext()
                    .getStepIndex()).toList(), java.util.stream.IntStream.range(0, count - 1).boxed().toList());
            final List<SpellAbility> expected = new ArrayList<>(fixture.copies);
            Collections.reverse(expected);
            assertEquals(result, expected);
        }
    }

    @Test
    public void nativeSessionsForN2N3N4CallTheTeacherOnceAndReturnItsValidInsertionList() {
        for (final int count : List.of(2, 3, 4)) {
            final Fixture fixture = fixture(count);
            final CopySpellResolveFirstOrderDecisionProvider provider =
                    new CopySpellResolveFirstOrderDecisionProvider();
            final List<SpellAbility> nativeResult = new ArrayList<>(fixture.copies);
            Collections.reverse(nativeResult);
            final int[] nativeCalls = {0};

            final List<SpellAbility> result = new CopySpellResolveFirstOrderDecisionCoordinator().order(
                    fixture.copies, fixture.player, provider, input -> {
                        nativeCalls[0]++;
                        return nativeResult;
                    });

            assertEquals(nativeCalls[0], 1);
            assertSame(result, nativeResult);
        }
    }

    @Test
    public void invalidExternalCandidatesAndResolverReplacementFailClosed() {
        final Fixture fixture = fixture(3);
        final CopySpellResolveFirstOrderDecisionCoordinator coordinator =
                new CopySpellResolveFirstOrderDecisionCoordinator();
        final CopySpellResolveFirstOrderDecisionProvider provider =
                new CopySpellResolveFirstOrderDecisionProvider();
        final LegalCandidate[] first = {null};
        provider.setResolver(request -> {
            if (first[0] == null) {
                first[0] = request.getCandidates().get(0);
                return first[0];
            }
            return first[0];
        });
        final SimultaneousTriggerOrderIntegrityException stale = expectThrows(
                SimultaneousTriggerOrderIntegrityException.class,
                () -> coordinator.order(fixture.copies, fixture.player, provider,
                        ignored -> {
                            throw new AssertionError("native fallback is forbidden");
                        }));
        assertEquals(stale.getReason(), "INVALID_EXTERNAL_CANDIDATE");

        final Fixture nullFixture = fixture(2);
        final CopySpellResolveFirstOrderDecisionProvider nullProvider =
                new CopySpellResolveFirstOrderDecisionProvider();
        nullProvider.setResolver(request -> null);
        final SimultaneousTriggerOrderIntegrityException nullResult = expectThrows(
                SimultaneousTriggerOrderIntegrityException.class,
                () -> coordinator.order(nullFixture.copies, nullFixture.player, nullProvider,
                        ignored -> {
                            throw new AssertionError("native fallback is forbidden");
                        }));
        assertEquals(nullResult.getReason(), "INVALID_EXTERNAL_CANDIDATE");

        final Fixture foreignFixture = fixture(2);
        final CopySpellResolveFirstOrderDecisionProvider foreignProvider =
                new CopySpellResolveFirstOrderDecisionProvider();
        foreignProvider.setResolver(request -> LegalCandidate.copySpellResolveFirstOrder(
                0, CopySpellResolveFirstOrderItemKind.COPIED_SPELL,
                new CopySpellResolveFirstOrderItem(99L,
                        new CopySpellResolveFirstOrderSourceProjection("Pyromatics"),
                        ApiType.DealDamage, CopySpellResolveFirstOrderItemKind.COPIED_SPELL)));
        final SimultaneousTriggerOrderIntegrityException foreign = expectThrows(
                SimultaneousTriggerOrderIntegrityException.class,
                () -> coordinator.order(foreignFixture.copies, foreignFixture.player, foreignProvider,
                        ignored -> {
                            throw new AssertionError("native fallback is forbidden");
                        }));
        assertEquals(foreign.getReason(), "INVALID_EXTERNAL_CANDIDATE");

        final Fixture wrongKindFixture = fixture(2);
        final CopySpellResolveFirstOrderDecisionProvider wrongKindProvider =
                new CopySpellResolveFirstOrderDecisionProvider();
        wrongKindProvider.setResolver(request -> LegalCandidate.order(0,
                OrderCandidateKind.SELECT_RESOLVE_FIRST,
                new SimultaneousTriggerOrderItem(99L,
                        new CardSelectionCard(wrongKindFixture.source), TriggerType.ChangesZone,
                        ApiType.DealDamage)));
        final SimultaneousTriggerOrderIntegrityException wrongKind = expectThrows(
                SimultaneousTriggerOrderIntegrityException.class,
                () -> coordinator.order(wrongKindFixture.copies, wrongKindFixture.player,
                        wrongKindProvider, ignored -> {
                            throw new AssertionError("native fallback is forbidden");
                        }));
        assertEquals(wrongKind.getReason(), "INVALID_EXTERNAL_CANDIDATE");
    }

    @Test
    public void nativeInvalidPermutationsAndCallbackThrowAreHardFailures() {
        final CopySpellResolveFirstOrderDecisionCoordinator coordinator =
                new CopySpellResolveFirstOrderDecisionCoordinator();
        final Fixture wrongSize = fixture(2);
        assertNativeMappingFailed(coordinator, wrongSize, List.of(wrongSize.copies.get(0)));

        final Fixture nullEntry = fixture(2);
        final List<SpellAbility> nullResult = new ArrayList<>(List.of(nullEntry.copies.get(0)));
        nullResult.add(null);
        assertNativeMappingFailed(coordinator, nullEntry, nullResult);

        final Fixture foreign = fixture(2);
        final Card foreignSource = addCardToZone("Pyromatics", foreign.player, ZoneType.Battlefield);
        final SpellAbility foreignOriginal = foreignSource.getFirstSpellAbility();
        foreignOriginal.setActivatingPlayer(foreign.player);
        final SpellAbility foreignCopy = CardFactory.copySpellAbilityAndPossiblyHost(
                foreignOriginal, foreignOriginal, foreign.player);
        assertNativeMappingFailed(coordinator, foreign,
                List.of(foreign.copies.get(0), foreignCopy));

        final Fixture omission = fixture(3);
        assertNativeMappingFailed(coordinator, omission,
                List.of(omission.copies.get(0), omission.copies.get(1), omission.copies.get(1)));

        final Fixture fixture = fixture(2);
        final SimultaneousTriggerOrderIntegrityException callbackFailure = expectThrows(
                SimultaneousTriggerOrderIntegrityException.class,
                () -> coordinator.order(fixture.copies, fixture.player,
                        new CopySpellResolveFirstOrderDecisionProvider(), ignored -> {
                            throw new IllegalStateException("native detail must not escape");
                        }));
        assertEquals(callbackFailure.getReason(), "NATIVE_CALLBACK_FAILURE");
        assertTrue(!callbackFailure.getMessage().contains("native detail"));
    }

    private void assertNativeMappingFailed(final CopySpellResolveFirstOrderDecisionCoordinator coordinator,
            final Fixture fixture, final List<SpellAbility> nativeResult) {
        final SimultaneousTriggerOrderIntegrityException exception = expectThrows(
                SimultaneousTriggerOrderIntegrityException.class,
                () -> coordinator.order(fixture.copies, fixture.player,
                        new CopySpellResolveFirstOrderDecisionProvider(), ignored -> nativeResult));
        assertEquals(exception.getReason(), "MAPPING_FAILED");
    }

    @Test
    public void resolverIsCapturedOnceForTheWholeSession() {
        final Fixture fixture = fixture(3);
        final CopySpellResolveFirstOrderDecisionProvider provider =
                new CopySpellResolveFirstOrderDecisionProvider();
        final int[] firstCalls = {0};
        final int[] replacementCalls = {0};
        final CopySpellResolveFirstOrderDecisionProvider.Resolver replacement = request -> {
            replacementCalls[0]++;
            return request.getCandidates().get(0);
        };
        provider.setResolver(request -> {
            firstCalls[0]++;
            provider.setResolver(replacement);
            return request.getCandidates().get(0);
        });

        new CopySpellResolveFirstOrderDecisionCoordinator().order(fixture.copies, fixture.player, provider,
                ignored -> {
                    throw new AssertionError("native callback must not run");
                });
        assertEquals(firstCalls[0], 2);
        assertEquals(replacementCalls[0], 0);
    }

    private Fixture fixture(final int count) {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(0);
        final Card source = addCardToZone("Pyromatics", player, ZoneType.Battlefield);
        final SpellAbility original = source.getFirstSpellAbility();
        original.setActivatingPlayer(player);

        final List<SpellAbility> copies = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            final SpellAbility copy = CardFactory.copySpellAbilityAndPossiblyHost(
                    original, original, player);
            copies.add(copy);
        }
        final SpellAbility nonCopy = source.getFirstSpellAbility();
        nonCopy.setActivatingPlayer(player);
        return new Fixture(player, source, copies, nonCopy);
    }

    private static final class Fixture {
        private final Player player;
        private final Card source;
        private final List<SpellAbility> copies;
        private final SpellAbility triggerLike;

        private Fixture(final Player player, final Card source, final List<SpellAbility> copies,
                final SpellAbility triggerLike) {
            this.player = player;
            this.source = source;
            this.copies = copies;
            this.triggerLike = triggerLike;
        }
    }
}
