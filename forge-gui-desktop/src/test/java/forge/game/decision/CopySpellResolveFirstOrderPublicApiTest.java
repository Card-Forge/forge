package forge.game.decision;

import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.ability.SpellApiBased;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.expectThrows;

public class CopySpellResolveFirstOrderPublicApiTest {
    private static final Set<Class<?>> FORBIDDEN_TYPES = Set.of(
            CardSelectionCard.class, Card.class, Player.class,
            GameObject.class, SpellAbility.class, SpellApiBased.class,
            ActionContinuation.class, forge.game.trigger.WrappedAbility.class);

    @Test
    public void exposesOnlyTheExactTypedProfileAndMarker() {
        assertEquals(CopySpellResolveFirstOrderProfile.values(),
                new CopySpellResolveFirstOrderProfile[] {
                        CopySpellResolveFirstOrderProfile.COPY_SPELL_RESOLVE_FIRST_ORDER
                });
        assertEquals(CopySpellResolveFirstOrderItemKind.values(),
                new CopySpellResolveFirstOrderItemKind[] {
                        CopySpellResolveFirstOrderItemKind.COPIED_SPELL
                });
        assertEquals(OrderDirection.values(), new OrderDirection[] {OrderDirection.RESOLVE_FIRST});

        final CopySpellResolveFirstOrderDecisionProvider provider =
                new CopySpellResolveFirstOrderDecisionProvider();
        assertNull(provider.getResolver());
        assertFalse(provider.hasResolver());
    }

    @Test
    public void duplicateLookingItemsRemainDistinctBySessionOrdinal() {
        final CopySpellResolveFirstOrderSourceProjection source =
                new CopySpellResolveFirstOrderSourceProjection("Pyromatics");
        final CopySpellResolveFirstOrderItem first = new CopySpellResolveFirstOrderItem(
                1L, source, ApiType.DealDamage, CopySpellResolveFirstOrderItemKind.COPIED_SPELL);
        final CopySpellResolveFirstOrderItem second = new CopySpellResolveFirstOrderItem(
                2L, source, ApiType.DealDamage, CopySpellResolveFirstOrderItemKind.COPIED_SPELL);

        assertEquals(first.getItemId(), 1L);
        assertEquals(second.getItemId(), 2L);
        assertEquals(first.getSourceProjection(), second.getSourceProjection());
        assertEquals(first.getEffectApi(), second.getEffectApi());
        assertEquals(first.getKind(), second.getKind());
        assertNotEquals(first, second);
    }

    @Test
    public void contextContainsOnlyStableSessionMetadata() {
        final CopySpellResolveFirstOrderContext context = new CopySpellResolveFirstOrderContext(
                CopySpellResolveFirstOrderProfile.COPY_SPELL_RESOLVE_FIRST_ORDER,
                OrderDirection.RESOLVE_FIRST, 7L, 0, 2, 11);

        assertEquals(context.getProfile(),
                CopySpellResolveFirstOrderProfile.COPY_SPELL_RESOLVE_FIRST_ORDER);
        assertEquals(context.getDirection(), OrderDirection.RESOLVE_FIRST);
        assertEquals(context.getOrderSessionId(), 7L);
        assertEquals(context.getStepIndex(), 0);
        assertEquals(context.getOriginalItemCount(), 2);
        assertEquals(context.getChoosingPlayerId(), 11);
        assertFalse(java.util.Arrays.stream(CopySpellResolveFirstOrderContext.class.getMethods())
                .anyMatch(method -> method.getName().equals("getRemainingItems")));
    }

    @Test
    public void publicDtosExposeNoNativeEngineTypesOrCopiedHostSelectionCard() {
        for (final Class<?> dto : Set.of(
                CopySpellResolveFirstOrderProfile.class,
                CopySpellResolveFirstOrderItemKind.class,
                CopySpellResolveFirstOrderSourceProjection.class,
                CopySpellResolveFirstOrderItem.class,
                CopySpellResolveFirstOrderContext.class,
                CopySpellResolveFirstOrderDecisionProvider.class)) {
            for (final Method method : dto.getMethods()) {
                assertFalse(FORBIDDEN_TYPES.contains(method.getReturnType()),
                        dto.getSimpleName() + " exposes " + method.getName());
                for (final Class<?> parameter : method.getParameterTypes()) {
                    assertFalse(FORBIDDEN_TYPES.contains(parameter),
                            dto.getSimpleName() + " accepts " + method.getName());
                }
            }
            for (final Field field : dto.getFields()) {
                assertFalse(FORBIDDEN_TYPES.contains(field.getType()),
                        dto.getSimpleName() + " exposes field " + field.getName());
            }
        }

        assertFalse(java.util.Arrays.stream(CopySpellResolveFirstOrderItem.class.getMethods())
                .anyMatch(method -> method.getReturnType() == CardSelectionCard.class));
        assertFalse(java.util.Arrays.stream(CopySpellResolveFirstOrderSourceProjection.class.getMethods())
                .anyMatch(method -> method.getReturnType() == CardSelectionCard.class));
    }

    @Test
    public void l1cOrderRequestUsesOnlyTheTypedProfileContextAndCandidatePayload() {
        final CopySpellResolveFirstOrderSourceProjection source =
                new CopySpellResolveFirstOrderSourceProjection("Pyromatics");
        final CopySpellResolveFirstOrderItem first = new CopySpellResolveFirstOrderItem(
                1L, source, ApiType.DealDamage, CopySpellResolveFirstOrderItemKind.COPIED_SPELL);
        final CopySpellResolveFirstOrderItem second = new CopySpellResolveFirstOrderItem(
                2L, source, ApiType.DealDamage, CopySpellResolveFirstOrderItemKind.COPIED_SPELL);
        final CopySpellResolveFirstOrderContext context = new CopySpellResolveFirstOrderContext(
                CopySpellResolveFirstOrderProfile.COPY_SPELL_RESOLVE_FIRST_ORDER,
                OrderDirection.RESOLVE_FIRST, 9L, 0, 2, 11);

        final LegalCandidate firstCandidate = LegalCandidate.copySpellResolveFirstOrder(
                0, CopySpellResolveFirstOrderItemKind.COPIED_SPELL, first);
        final LegalCandidate secondCandidate = LegalCandidate.copySpellResolveFirstOrder(
                1, CopySpellResolveFirstOrderItemKind.COPIED_SPELL, second);
        final DecisionRequest request = new DecisionRequest(4L, DecisionType.ORDER,
                java.util.List.of(firstCandidate, secondCandidate), context);

        assertSame(request.getCopySpellResolveFirstOrderContext(), context);
        assertNull(request.getOrderContext());
        assertEquals(request.getCandidates().get(0).getCopySpellResolveFirstOrderKind(),
                CopySpellResolveFirstOrderItemKind.COPIED_SPELL);
        assertSame(request.getCandidates().get(0).getCopySpellResolveFirstOrderItem(), first);
        assertEquals(request.getCandidates().get(0).getSemanticKey(), "RESOLVE_FIRST|1");
    }

    @Test
    public void l1AndL1cProvidersOwnIndependentLocalCounters() throws Exception {
        final SimultaneousTriggerOrderDecisionProvider l1 =
                new SimultaneousTriggerOrderDecisionProvider();
        final CopySpellResolveFirstOrderDecisionProvider l1c =
                new CopySpellResolveFirstOrderDecisionProvider();

        assertEquals(l1.nextRequestId(), 1L);
        assertEquals(l1c.nextRequestId(), 1L);
        assertEquals(l1.nextOrderSessionId(), 1L);
        assertEquals(l1c.nextOrderSessionId(), 1L);
        assertEquals(l1.nextRequestId(), 2L);
        assertEquals(l1c.nextRequestId(), 2L);
        expectThrows(ClassNotFoundException.class,
                () -> Class.forName("forge.game.decision.OrderDecisionIdAuthority"));
    }

    @Test
    public void orderRequestsRejectCrossProfilePayloadsAndOneCandidateRequests() {
        final CopySpellResolveFirstOrderSourceProjection source =
                new CopySpellResolveFirstOrderSourceProjection("Pyromatics");
        final CopySpellResolveFirstOrderItem first = new CopySpellResolveFirstOrderItem(
                1L, source, ApiType.DealDamage, CopySpellResolveFirstOrderItemKind.COPIED_SPELL);
        final CopySpellResolveFirstOrderItem second = new CopySpellResolveFirstOrderItem(
                2L, source, ApiType.DealDamage, CopySpellResolveFirstOrderItemKind.COPIED_SPELL);
        final CopySpellResolveFirstOrderContext l1cContext = new CopySpellResolveFirstOrderContext(
                CopySpellResolveFirstOrderProfile.COPY_SPELL_RESOLVE_FIRST_ORDER,
                OrderDirection.RESOLVE_FIRST, 9L, 0, 2, 11);
        final SimultaneousTriggerOrderContext l1Context = new SimultaneousTriggerOrderContext(
                SimultaneousTriggerOrderProfile.SIMULTANEOUS_TRIGGER_ORDER,
                OrderDirection.RESOLVE_FIRST, 9L, 0, 2, 11);

        expectThrows(IllegalArgumentException.class, () -> new DecisionRequest(6L, DecisionType.ORDER,
                java.util.List.of(LegalCandidate.copySpellResolveFirstOrder(
                        0, CopySpellResolveFirstOrderItemKind.COPIED_SPELL, first),
                        LegalCandidate.copySpellResolveFirstOrder(
                                1, CopySpellResolveFirstOrderItemKind.COPIED_SPELL, second)),
                l1Context));
        expectThrows(IllegalArgumentException.class, () -> new DecisionRequest(7L, DecisionType.ORDER,
                java.util.List.of(LegalCandidate.copySpellResolveFirstOrder(
                        0, CopySpellResolveFirstOrderItemKind.COPIED_SPELL, first)), l1cContext));
    }
}
